package org.nuxeo.ecm.platform.publisher.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;

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
}
