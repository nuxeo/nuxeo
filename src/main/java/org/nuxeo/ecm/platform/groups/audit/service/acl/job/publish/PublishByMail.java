package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import java.io.File;
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
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

public class PublishByMail implements IResultPublisher {
    private static final Log log = LogFactory.getLog(PublishByMail.class);

    public static final String PROPERTY_MAILFROM = "noreply@nuxeo.com";

    public static final String PROPERTY_ACLAUDIT_SENDMAIL_CHAIN = "ACL.Audit.SendMail";

    protected final AutomationService automation = Framework.getLocalService(AutomationService.class);

    protected File fileToPublish;

    protected String documentName;

    protected String repository;

    protected String email;

    public PublishByMail(File fileToPublish, String documentName, String email,
            String repository) {
        this.fileToPublish = fileToPublish;
        this.repository = repository;
        this.email = email;
        this.documentName = documentName;
    }

    public void publish() throws ClientException {
        reconnectAndSendMail(repository, fileToPublish, email);
    }

    protected void reconnectAndSendMail(String repository,
            final File fileToPublish, final String email)
            throws ClientException {
        new UnrestrictedSessionRunner(repository) {
            @Override
            public void run() throws ClientException {
                Blob b = new FileBlob(fileToPublish);
                b.setFilename(documentName);

                DocumentModel docToSend = createDocument(session, b,
                        documentName, documentName);
                doCallOperationSendMail(session, docToSend, email);
                log.debug("audit sent");
            }
        }.runUnrestricted();
    }

    protected void doCallOperationSendMail(CoreSession session,
            DocumentModel docToSend, String email) {
        String from = Framework.getProperty(PROPERTY_MAILFROM, "noreply");
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docToSend);

        try {
            OperationChain chain = new OperationChain(
                    PROPERTY_ACLAUDIT_SENDMAIL_CHAIN);
            OperationParameters params = chain.add(SendMail.ID);//findParameters(chain, SendMail.ID);
            if (params == null) {
                log.error("failed to retrieve operation " + SendMail.ID
                        + " in chain " + chain);
                return;
            }
            params.set("from", from);
            params.set("to", email);
            params.set("message", "ACL Audit report");
            params.set("subject", "ACL Audit report");
            log.debug("Automation run " + PROPERTY_ACLAUDIT_SENDMAIL_CHAIN
                    + " for " + email);
            automation.run(ctx, chain);
            log.debug("Automation done " + PROPERTY_ACLAUDIT_SENDMAIL_CHAIN
                    + " for " + email);
        } catch (InvalidChainException e) {
            log.error(e);
        } catch (OperationException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }
    }

    protected OperationParameters findParameters(OperationChain chain, String id) {
        List<OperationParameters> params = chain.getOperations();
        for (OperationParameters p : params)
            if (p.id().equals(id))
                return p;
        return null;
    }

    protected DocumentModel createDocument(CoreSession session, Blob blob,
            String title, String filename) throws ClientException {
        DocumentModel document = session.createDocumentModel("File");
        document.setPropertyValue("file:content", (Serializable) blob);
        document.setPropertyValue("file:filename", filename);
        document.setPropertyValue("dublincore:title", title);
        return document;
    }
}
