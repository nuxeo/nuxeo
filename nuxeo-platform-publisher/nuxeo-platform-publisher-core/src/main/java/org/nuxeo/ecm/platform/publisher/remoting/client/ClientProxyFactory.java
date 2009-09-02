package org.nuxeo.ecm.platform.publisher.remoting.client;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.Map;

/**
 * 
 * Implementation of {@link PublishedDocumentFactory} for local tree pointing to
 * a remote tree. This basically means that the only job done by the
 * {@link PublishedDocumentFactory} is snaphoting the document before sending it
 * for remote publishing.
 * 
 * @author tiry
 * 
 */
public class ClientProxyFactory extends AbstractBasePublishedDocumentFactory
        implements PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        throw new IllegalStateException(
                "ClientProxyFactory can not be called to publish locally");
    }

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        throw new IllegalStateException(
                "ClientProxyFactory can not be called to publish locally");
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        throw new ClientException(
                "this factory can not wrap a PublishedDocument");
    }

}
