/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */
package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.mail.Mailer;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.groups.audit.service.acl.utils.MessageAccessor;
import org.nuxeo.runtime.api.Framework;

public class PublishByMail implements IResultPublisher {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishByMail.class);

    public static final String PROPERTY_ACLAUDIT_SENDMAIL_CHAIN = "ACL.Audit.SendMail";

    public static final String PROPERTY_MAILFROM = "mail.from";

    public static final String PROPERTY_MAIL_SUBJECT = "message.acl.audit.mail.title";

    public static final String PROPERTY_MAIL_BODY = "message.acl.audit.mail.body";

    public static final String OUTPUT_FILE_NAME = "permissions.xls";

    public static final String FROM = "noreply@nuxeo.com";

    protected String repositoryName;

    protected String to;

    protected String defaultFrom;

    public PublishByMail(String to, String defaultFrom, String repositoryName) {
        this.repositoryName = repositoryName;
        this.to = to;
        this.defaultFrom = defaultFrom;
    }

    @Override
    public void publish(final Blob file) {
        file.setFilename(OUTPUT_FILE_NAME);
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() {
                DocumentModel docToSend = createDocument(session, file, "", "");
                doCallOperationSendMail(session, docToSend, to, defaultFrom);
                log.debug("audit sent");
            }
        }.runUnrestricted();
    }

    protected void doCallOperationSendMail(CoreSession session, DocumentModel docToSend, String to, String defaultFrom)
            {
        String title = MessageAccessor.get(session, PROPERTY_MAIL_SUBJECT);
        String body = MessageAccessor.get(session, PROPERTY_MAIL_BODY);
        String from = Framework.getProperty(PROPERTY_MAILFROM, defaultFrom);
        AutomationService automation = Framework.getService(AutomationService.class);

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(docToSend);

            OperationChain chain = new OperationChain(PROPERTY_ACLAUDIT_SENDMAIL_CHAIN);
            OperationParameters params = chain.add(SendMail.ID);
            if (params == null) {
                log.error("failed to retrieve operation " + SendMail.ID + " in chain " + chain);
                return;
            }

            // configure email
            params.set("from", from);
            params.set("to", to);
            params.set("subject", title);
            params.set("message", body);
            String[] str = { "file:content" };
            params.set("files", new StringList(str));
            // TODO: see SendMail test case where we can directly pass a blob

            // do send mail
            log.debug("Automation run " + PROPERTY_ACLAUDIT_SENDMAIL_CHAIN + " for " + to);
            automation.run(ctx, chain);
            log.debug("Automation done " + PROPERTY_ACLAUDIT_SENDMAIL_CHAIN + " for " + to);
        } catch (OperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected OperationParameters findParameters(OperationChain chain, String id) {
        List<OperationParameters> params = chain.getOperations();
        for (OperationParameters p : params)
            if (p.id().equals(id))
                return p;
        return null;
    }

    protected DocumentModel createDocument(CoreSession session, Blob blob, String title, String filename)
            {
        DocumentModel document = session.createDocumentModel("File");
        document.setPropertyValue("file:content", (Serializable) blob);
        document.setPropertyValue("file:filename", filename);
        document.setPropertyValue("dublincore:title", title);
        return document;
    }

    protected void logMailerConfiguration() {
        Mailer m = SendMail.COMPOSER.getMailer();
        log.info("mail.smtp.auth:" + m.getConfiguration().get("mail.smtp.auth"));
        log.info("mail.smtp.starttls.enable:" + m.getConfiguration().get("mail.smtp.starttls.enable"));
        log.info("mail.smtp.host:" + m.getConfiguration().get("mail.smtp.host"));
        log.info("mail.smtp.user:" + m.getConfiguration().get("mail.smtp.user"));
        log.info("mail.smtp.password:" + m.getConfiguration().get("mail.smtp.password"));
    }
}
