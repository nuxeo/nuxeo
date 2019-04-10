package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/** Publish a file in the repository as a child of an existing document. */
public class PublishAsDocument implements IResultPublisher {
    private static final Log log = LogFactory.getLog(PublishAsDocument.class);

    protected File fileToPublish;
    protected String documentName;
    protected String repository;
    protected DocumentModel parent;

    public PublishAsDocument(File fileToPublish, String documentName,
            String repository, DocumentModel parent) {
        this.fileToPublish = fileToPublish;
        this.documentName = documentName;
        this.repository = repository;
        this.parent = parent;
    }

    @Override
    public void publish() throws ClientException{
        log.debug("about to save audit");
        Blob b = new FileBlob(fileToPublish);
        b.setFilename(documentName);

        try {
            reconnectAndCreateDocument(repository, parent, documentName, b);
        } catch (ClientException e) {
            log.error(e,e);
        }
    }

    protected void reconnectAndCreateDocument(String repository,
            final DocumentModel parent, final String name, final Blob doc)
            throws ClientException {
        new UnrestrictedSessionRunner(repository) {
            @Override
            public void run() throws ClientException {
                createOrUpdateDocument(session, parent, name, doc);
                log.debug("audit saved");
            }
        }.runUnrestricted();
    }

    protected DocumentModel createOrUpdateDocument(CoreSession session,
            DocumentModel parent, String name, Blob doc) throws ClientException {
        DocumentRef dr = new PathRef(parent.getPath().append(name).toString());
        String filenamePlusExt = doc.getFilename();
        if (session.exists(dr)) {
            DocumentModel document = session.getDocument(dr);
            document.setPropertyValue("file:content", (Serializable) doc);
            document.setPropertyValue("file:filename", filenamePlusExt);
            document.setPropertyValue("dublincore:title", name);
            return session.saveDocument(document);
        } else {
            DocumentModel document = session.createDocumentModel(
                    parent.getPathAsString(),
                    IdUtils.generatePathSegment(name), "File");
            document.setPropertyValue("file:content", (Serializable) doc);
            document.setPropertyValue("file:filename", filenamePlusExt);
            document.setPropertyValue("dublincore:title", name);
            DocumentModel d = session.createDocument(document);
            return session.saveDocument(d);
        }
    }
}
