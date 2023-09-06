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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.ecm.platform.rendering.impl.DocumentRenderingContext;
import org.nuxeo.mail.MailException;
import org.nuxeo.mail.MailMessage;
import org.nuxeo.mail.MailService;
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

    private static final Logger log = LogManager.getLogger(EmailHelper.class);

    // used for loading templates from strings
    private final Configuration stringCfg = new Configuration(Configuration.VERSION_2_3_0);

    /**
     * Sends {@link MailMessage}s.
     *
     * @since 2023.4
     */
    public void sendMailMessage(Map<String, Object> mail) {
        try {
            sendmail0(mail);
        } catch (IOException | TemplateException | RenderingException e) {
            throw new MailException(e.getMessage(), e);
        }
    }

    /**
     * Sends mails from a {@link Map}.
     *
     * @param mail A map of the settings
     * @deprecated since 2023.4 because doesn't fit in a generic service. Use {@link #sendMailMessage} instead.
     */
    @Deprecated(since = "2023.4")
    public void sendmail(Map<String, Object> mail) throws MessagingException {
        try {
            sendmail0(mail);
        } catch (IOException | TemplateException | RenderingException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    protected void sendmail0(Map<String, Object> mail) throws IOException, TemplateException, RenderingException {
        Object to = mail.get("mail.to");
        if (!(to instanceof String recipient)) {
            log.error("Invalid email recipient: {}", to);
            return;
        }

        DocumentRenderingContext context = new DocumentRenderingContext();
        context.putAll(mail);
        context.setDocument((DocumentModel) mail.get("document"));
        context.put("Runtime", Framework.getRuntime());

        // Build the message.
        var mailMessage = new MailMessage.Builder(recipient).subject(computeSubject(mail, context))
                                                            .content(renderHTMLBody(mail, context),
                                                                    "text/html; charset=utf-8")
                                                            .senderName(getSenderName())
                                                            .build();

        // Send the message.
        Framework.getService(MailService.class).sendMail(mailMessage);
    }

    protected String computeSubject(Map<String, Object> mail, DocumentRenderingContext context)
            throws IOException, TemplateException, RenderingException {
        String customSubjectTemplate = (String) mail.get(NotificationConstants.SUBJECT_TEMPLATE_KEY);
        if (customSubjectTemplate == null) {
            String subjTemplate = (String) mail.get(NotificationConstants.SUBJECT_KEY);
            Template templ = new Template("name", new StringReader(subjTemplate), stringCfg);

            Writer out = new StringWriter();
            templ.process(mail, out);
            out.flush();

            return out.toString();
        } else {
            RenderingService rs = Framework.getService(RenderingService.class);
            rs.registerEngine(new NotificationsRenderingEngine(customSubjectTemplate));

            try (NuxeoLoginContext loginContext = Framework.loginSystem()) {
                Collection<RenderingResult> results = rs.process(context);
                String subjectMail = "<HTML><P>No parsing Succeded !!!</P></HTML>";

                for (RenderingResult result : results) {
                    subjectMail = (String) result.getOutcome();
                }
                subjectMail = NotificationServiceHelper.getNotificationService().getEMailSubjectPrefix() + subjectMail;
                return subjectMail;
            }
        }
    }

    protected String renderHTMLBody(Map<String, Object> mail, DocumentRenderingContext context)
            throws RenderingException {
        RenderingService rs = Framework.getService(RenderingService.class);
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

        return bodyMail;
    }

    private static String getSenderName() {
        return Framework.getService(NotificationManager.class).getMailSenderName();
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
