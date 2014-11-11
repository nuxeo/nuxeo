package org.nuxeo.ecm.platform.publisher.impl.localfs;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.Map;

public class FSPublishedDocumentFactory extends
        AbstractBasePublishedDocumentFactory implements
        PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        try {
            FSPublishedDocument pubDoc = new FSPublishedDocument("local", doc);
            pubDoc.persist(targetNode.getPath());
            return pubDoc;
        } catch (Exception e) {
            throw new ClientException("Error duning FS Publishing", e);
        }
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        try {

            doc = snapshotDocumentBeforePublish(doc);
            return new FSPublishedDocument("local", doc);
        } catch (Exception e) {
            throw new ClientException(
                    "Error while wrapping DocumentModel as FSPublishedDocument",
                    e);
        }
    }

}
