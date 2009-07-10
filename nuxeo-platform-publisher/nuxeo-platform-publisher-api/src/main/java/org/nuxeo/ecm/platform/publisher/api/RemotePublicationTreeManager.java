package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.List;
import java.util.Map;

/**
 * 
 * Remote interface used by PublicationService to communicate with each others
 * 
 * @author tiry
 * 
 */
public interface RemotePublicationTreeManager {

    List<PublishedDocument> getChildrenDocuments(PublicationNode node)
            throws ClientException;

    List<PublicationNode> getChildrenNodes(PublicationNode node)
            throws ClientException;

    PublicationNode getParent(PublicationNode node);

    PublicationNode getNodeByPath(String sid, String path)
            throws ClientException;

    List<PublishedDocument> getExistingPublishedDocument(String sid,
            DocumentLocation docLoc) throws ClientException;

    List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) throws ClientException;

    void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    void unpublish(String sid, PublishedDocument publishedDocument)
            throws ClientException;

    Map<String, String> initRemoteSession(String treeConfigName,
            Map<String, String> params) throws Exception;

    void release(String sid);

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be
     *            approved
     * @throws ClientException
     */
    void validatorPublishDocument(String sid, PublishedDocument publishedDocument)
            throws ClientException;

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be
     *            rejected
     * @param comment
     * @throws ClientException
     */
    void validatorRejectPublication(String sid, PublishedDocument publishedDocument,
            String comment) throws ClientException;

}
