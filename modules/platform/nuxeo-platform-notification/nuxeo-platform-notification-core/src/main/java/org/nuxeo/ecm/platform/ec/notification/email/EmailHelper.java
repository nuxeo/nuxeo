/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     npaslaru, tmartins, jcarsique
 *
 */

package org.nuxeo.ecm.platform.ec.notification.email;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mvel2.MVEL;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.ecm.platform.rendering.impl.DocumentRenderingContext;
import org.nuxeo.mail.MailSessionBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Class EmailHelper.
 * <p>
 * An email helper:
 * <p>
 *
 * <pre>
 * Hashtable mail = new Hashtable();
 * mail.put(&quot;from&quot;, &quot;dion@almaer.com&quot;);
 * mail.put(&quot;to&quot;, &quot;dion@almaer.com&quot;);
 * mail.put(&quot;subject&quot;, &quot;a subject&quot;);
 * mail.put(&quot;template&quot;, &quot;a template name&quot;);
 * &lt;p&gt;
 * EmailHelper.sendmail(mail);
 * </pre>
 *
 * Currently only supports one email in to address
 */
public class EmailHelper {

    private static final Log log = LogFactory.getLog(EmailHelper.class);

    // used for loading templates from strings
    private final Configuration stringCfg = new Configuration(Configuration.VERSION_2_3_0);

    protected static boolean javaMailNotAvailable = false;

    /**
     * Static Method: sendmail(Map mail).
     *
     * @param mail A map of the settings
     */
    public void sendmail(Map<String, Object> mail) throws MessagingException {
        try {
            sendmail0(mail);
        } catch (IOException | TemplateException | RenderingException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected void sendmail0(Map<String, Object> mail)
            throws MessagingException, IOException, TemplateException, RenderingException {

        Session session = getSession();
        if (javaMailNotAvailable || session == null) {
            log.warn("Not sending email since JavaMail is not configured");
            return;
        }

        // Construct a MimeMessage
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(session.getProperty("mail.from")));
        Object to = mail.get("mail.to");
        if (!(to instanceof String)) {
            log.error("Invalid email recipient: " + to);
            return;
        }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse((String) to, false));

        RenderingService rs = Framework.getService(RenderingService.class);

        DocumentRenderingContext context = new DocumentRenderingContext();
        context.putAll(mail);
        context.setDocument((DocumentModel) mail.get("document"));
        context.put("Runtime", Framework.getRuntime());

        String customSubjectTemplate = (String) mail.get(NotificationConstants.SUBJECT_TEMPLATE_KEY);
        if (customSubjectTemplate == null) {
            String subjTemplate = (String) mail.get(NotificationConstants.SUBJECT_KEY);
            Template templ = new Template("name", new StringReader(subjTemplate), stringCfg);

            Writer out = new StringWriter();
            templ.process(mail, out);
            out.flush();

            msg.setSubject(out.toString(), "UTF-8");
        } else {
            rs.registerEngine(new NotificationsRenderingEngine(customSubjectTemplate));

            try (NuxeoLoginContext loginContext = Framework.loginSystem()) {
                Collection<RenderingResult> results = rs.process(context);
                String subjectMail = "<HTML><P>No parsing Succeded !!!</P></HTML>";

                for (RenderingResult result : results) {
                    subjectMail = (String) result.getOutcome();
                }
                subjectMail = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + subjectMail;
                msg.setSubject(subjectMail, "UTF-8");
            }
        }

        msg.setSentDate(new Date());

        String template = (String) mail.get(NotificationConstants.TEMPLATE_KEY);
        rs.registerEngine(new NotificationsRenderingEngine(template));

        String bodyMail = "<HTML><P>No parsing Succedeed !!!</P></HTML>";

        Collection<RenderingResult> results;
        try (NuxeoLoginContext lc = Framework.loginSystem()) {
            results = rs.process(context);
        }
        for (RenderingResult result : results) {
            bodyMail = (String) result.getOutcome();
        }

        rs.unregisterEngine(template);

        msg.setContent(bodyMail, "text/html; charset=utf-8");

        // Send the message.
        Transport.send(msg);
    }

    /**
     * Gets the session from the JNDI.
     */
    private static Session getSession() {
        if (!javaMailNotAvailable) {
            // First, try to get the session from JNDI, as would be done under J2EE.
            try {
                NotificationService service = (NotificationService) Framework.getService(NotificationManager.class);
                return MailSessionBuilder.fromJndi(service.getMailSessionJndiName()).build();
            } catch (NuxeoException ex) {
                log.warn("Unable to find Java mail API", ex);
                javaMailNotAvailable = true;
            }
        }
        return null;
    }

    /**
     * Instantiates a new session that authenticates given the protocol's properties. Initializes also the default
     * transport protocol handler according to the properties.
     *
     * @since 5.6
     * @deprecated since 11.1, use {@link MailSessionBuilder}
     */
    @Deprecated(since = "11.1")
    public static Session newSession(Properties props) {
        return MailSessionBuilder.fromProperties(props).build();
    }

    protected Map<String, Object> initMvelBindings(Map<String, Serializable> infos) {
        Map<String, Object> map = new HashMap<>();
        map.put("NotificationContext", infos);
        return map;
    }

    /***
     * Evaluates a MVEL expression within some context infos accessible on the "NotificationContext" object. Returns
     * null if the result is not evaluated to a String
     *
     * @since 5.6
     */
    public String evaluateMvelExpresssion(String expr, Map<String, Serializable> ctx) {
        // check to see if there is a dynamic MVEL expr
        Serializable compiledExpr = MVEL.compileExpression(expr);
        Object result = MVEL.executeExpression(compiledExpr, initMvelBindings(ctx));
        if (result instanceof String) {
            return (String) result;
        }
        return null;
    }
}
