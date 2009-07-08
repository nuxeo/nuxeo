package org.nuxeo.ecm.platform.publisher.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * 
 * Interface of the pluggable factory used to create a PublishedDocument in a
 * give PublicationTree
 * 
 * @author tiry
 * 
 */
public interface PublishedDocumentFactory {

    String getName();

    PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode) throws ClientException;

    PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException;

    void init(CoreSession coreSession, Map<String, String> parameters)
            throws ClientException;

    DocumentModel snapshotDocumentBeforePublish(DocumentModel doc)
            throws ClientException;

    PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException;

    /**
     * Set the {@code PublicationTree} owner of this factory.
     *
     * @param publicationTree the {@code PublicationTree} owner of this factory.
     */
    void setPublicationTree(PublicationTree publicationTree);
}
