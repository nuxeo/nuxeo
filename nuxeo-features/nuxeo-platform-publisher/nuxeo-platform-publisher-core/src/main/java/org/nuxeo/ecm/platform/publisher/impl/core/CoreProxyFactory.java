package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.Map;

/**
 *
 * Implementation of the {@link PublishedDocumentFactory} for simple core
 * implementation using native proxy system
 *
 * @author tiry
 *
 */
public class CoreProxyFactory extends AbstractBasePublishedDocumentFactory
        implements PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(
                    targetNode.getPath()));
        }

        DocumentModel proxy ;
        if ((params != null) && (params.containsKey("overwriteExistingProxy"))) {
            proxy = coreSession.publishDocument(doc, targetDocModel,
                    Boolean.parseBoolean(params.get("overwriteExistingProxy")));
        } else {
            proxy = coreSession.publishDocument(doc, targetDocModel);
        }
        coreSession.save();
        return new SimpleCorePublishedDocument(proxy);
    }

    public DocumentModel snapshotDocumentBeforePublish(DocumentModel doc) {
        // snapshooting is done as part of the publishing
        return doc;
    }

    public DocumentModel unwrapPublishedDocument(PublishedDocument pubDoc)
            throws ClientException {
        if (pubDoc instanceof SimpleCorePublishedDocument) {
            SimpleCorePublishedDocument pubProxy = (SimpleCorePublishedDocument) pubDoc;
            return pubProxy.getProxy();
        }
        throw new ClientException(
                "factory can not unwrap this PublishedDocument");
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        return new SimpleCorePublishedDocument(doc);
    }



}
