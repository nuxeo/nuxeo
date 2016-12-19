/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.notification;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.mail.Composer;
import org.nuxeo.ecm.automation.core.mail.Mailer;
import org.nuxeo.ecm.automation.core.mail.Mailer.Message;
import org.nuxeo.ecm.automation.core.mail.Mailer.Message.AS;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.TemplateException;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SendMail.ID, category = Constants.CAT_NOTIFICATION, label = "Send E-Mail", description = "Send an email using the input document to the specified recipients. You can use the HTML parameter to specify whether you message is in HTML format or in plain text. Also you can attach any blob on the current document to the message by using the comma separated list of xpath expressions 'files'. If you xpath points to a blob list all blobs in the list will be attached. Return back the input document(s). If rollbackOnError is true, the whole chain will be rollbacked if an error occurs while trying to send the email (for instance if no SMTP server is configured), else a simple warning will be logged and the chain will continue.", aliases = {
        "Notification.SendMail" })
public class SendMail {

    protected static final Log log = LogFactory.getLog(SendMail.class);

    public static final Composer COMPOSER = new Composer();

    public static final String ID = "Document.Mail";

    @Context
    protected OperationContext ctx;

    @Context
    protected UserManager umgr;

    @Param(name = "from")
    protected String from;

    @Param(name = "to")
    protected StringList to;

    // Useful for tests.
    protected Session mailSession;

    /**
     * @since 5.9.1
     */
    @Param(name = "cc", required = false)
    protected StringList cc;

    /**
     * @since 5.9.1
     */
    @Param(name = "bcc", required = false)
    protected StringList bcc;

    /**
     * @since 5.9.1
     */
    @Param(name = "replyto", required = false)
    protected StringList replyto;

    @Param(name = "subject")
    protected String subject;

    @Param(name = "message", widget = Constants.W_MAIL_TEMPLATE)
    protected String message;

    @Param(name = "HTML", required = false, values = { "false" })
    protected boolean asHtml = false;

    @Param(name = "files", required = false)
    protected StringList blobXpath;

    @Param(name = "rollbackOnError", required = false, values = { "true" })
    protected boolean rollbackOnError = true;

    /**
     * @since 5.9.1
     */
    @Param(name = "Strict User Resolution", required = false)
    protected boolean isStrict = true;

    @Param(name = "viewId", required = false, values = { "view_documents" })
    protected String viewId = "view_documents";

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc)
            throws TemplateException, RenderingException, OperationException, MessagingException, IOException {
        send(doc);
        return doc;
    }

    protected String getContent() throws OperationException, IOException {
        message = message.trim();
        if (message.startsWith("template:")) {
            String name = message.substring("template:".length()).trim();
            URL url = MailTemplateHelper.getTemplate(name);
            if (url == null) {
                throw new OperationException("No such mail template: " + name);
            }
            try (InputStream in = url.openStream()) {
                return IOUtils.toString(in, Charsets.UTF_8);
            }
        } else {
            return StringEscapeUtils.unescapeHtml(message);
        }
    }

    protected void send(DocumentModel doc)
            throws TemplateException, RenderingException, OperationException, MessagingException, IOException {
        // TODO should sent one by one to each recipient? and have the template
        // rendered for each recipient? Use: "mailto" var name?
        try {
            Map<String, Object> map = Scripting.initBindings(ctx);
            // do not use document wrapper which is working only in mvel.
            map.put("Document", doc);
            map.put("docUrl", MailTemplateHelper.getDocumentUrl(doc, viewId));
            map.put("subject", subject);
            map.put("to", to);
            map.put("toResolved", MailBox.fetchPersonsFromList(to, isStrict));
            map.put("from", from);
            map.put("fromResolved", MailBox.fetchPersonsFromString(from, isStrict));
            map.put("from", cc);
            map.put("fromResolved", MailBox.fetchPersonsFromList(cc, isStrict));
            map.put("from", bcc);
            map.put("fromResolved", MailBox.fetchPersonsFromList(bcc, isStrict));
            map.put("from", replyto);
            map.put("fromResolved", MailBox.fetchPersonsFromList(replyto, isStrict));
            map.put("viewId", viewId);
            map.put("baseUrl", NotificationServiceHelper.getNotificationService().getServerUrlPrefix());
            map.put("Runtime", Framework.getRuntime());
            Mailer.Message msg = createMessage(doc, getContent(), map);
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());

            addMailBoxInfo(msg);

            msg.send();
        } catch (NuxeoException | TemplateException | RenderingException | OperationException | MessagingException
                | IOException e) {
            if (rollbackOnError) {
                throw e;
            } else {
                log.warn(String.format(
                        "An error occured while trying to execute the %s operation, see complete stack trace below. Continuing chain since 'rollbackOnError' was set to false.",
                        ID), e);
            }
        }
    }

    /**
     * @since 5.9.1
     */
    private void addMailBoxInfo(Mailer.Message msg) throws MessagingException {
        List<MailBox> persons = MailBox.fetchPersonsFromString(from, isStrict);
        addMailBoxInfoInMessageHeader(msg, AS.FROM, persons);

        persons = MailBox.fetchPersonsFromList(to, isStrict);
        addMailBoxInfoInMessageHeader(msg, AS.TO, persons);

        persons = MailBox.fetchPersonsFromList(cc, isStrict);
        addMailBoxInfoInMessageHeader(msg, AS.CC, persons);

        persons = MailBox.fetchPersonsFromList(bcc, isStrict);
        addMailBoxInfoInMessageHeader(msg, AS.BCC, persons);

        if (replyto != null && !replyto.isEmpty()) {
            msg.setReplyTo(null);
            persons = MailBox.fetchPersonsFromList(replyto, isStrict);
            addMailBoxInfoInMessageHeader(msg, AS.REPLYTO, persons);
        }
    }

    /**
     * @since 5.9.1
     */
    private void addMailBoxInfoInMessageHeader(Message msg, AS as, List<MailBox> persons) throws MessagingException {
        for (MailBox person : persons) {
            msg.addInfoInMessageHeader(person.toString(), as);
        }
    }

    protected Mailer.Message createMessage(DocumentModel doc, String message, Map<String, Object> map)
            throws MessagingException, TemplateException, RenderingException, IOException {
        if (blobXpath == null) {
            if (asHtml) {
                return COMPOSER.newHtmlMessage(message, map);
            } else {
                return COMPOSER.newTextMessage(message, map);
            }
        } else {
            List<Blob> blobs = new ArrayList<>();
            for (String xpath : blobXpath) {
                try {
                    Property p = doc.getProperty(xpath);
                    if (p instanceof BlobProperty) {
                        getBlob(p.getValue(), blobs);
                    } else if (p instanceof ListProperty) {
                        for (Property pp : p) {
                            getBlob(pp.getValue(), blobs);
                        }
                    } else if (p instanceof MapProperty) {
                        for (Property sp : ((MapProperty) p).values()) {
                            getBlob(sp.getValue(), blobs);
                        }
                    } else {
                        Object o = p.getValue();
                        if (o instanceof Blob) {
                            blobs.add((Blob) o);
                        }
                    }
                } catch (PropertyException pe) {
                    log.error("Error while fetching blobs: " + pe.getMessage());
                    log.debug(pe, pe);
                }
            }
            return COMPOSER.newMixedMessage(message, map, asHtml ? "html" : "plain", blobs);
        }
    }

    /**
     * @since 5.7
     * @param o: the object to introspect to find a blob
     * @param blobs: the Blob list where the blobs are put during property introspection
     */
    @SuppressWarnings("unchecked")
    private void getBlob(Object o, List<Blob> blobs) {
        if (o instanceof List) {
            for (Object item : (List<Object>) o) {
                getBlob(item, blobs);
            }
        } else if (o instanceof Map) {
            for (Object item : ((Map<String, Object>) o).values()) {
                getBlob(item, blobs);
            }
        } else if (o instanceof Blob) {
            blobs.add((Blob) o);
        }

    }
}
