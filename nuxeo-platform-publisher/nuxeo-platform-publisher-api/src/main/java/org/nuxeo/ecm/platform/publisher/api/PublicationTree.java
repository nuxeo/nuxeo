package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;

import java.util.List;
import java.util.Map;

/**
 * Interface for the publication tree. A Publication Tree is a generic view on a
 * set of PublicationNode.
 * 
 * @author tiry
 */
public interface PublicationTree extends PublicationNode {

    PublicationNode getNodeByPath(String path) throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) throws ClientException;

    void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    void unpublish(PublishedDocument publishedDocument) throws ClientException;

    List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc)
            throws ClientException;

    List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node)
            throws ClientException;

    String getConfigName();

    String getTreeType();

    void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName) throws ClientException;

    void release();

    String getIconExpanded();

    String getIconCollapsed();

    /**
     * A validator (the current user) approves the publication.
     * 
     * @param publishedDocument the current published document that will be
     *            approved
     * @throws PublishingException
     */
    void validatorPublishDocument(PublishedDocument publishedDocument)
            throws ClientException;

    /**
     * A validator (the current user) rejects the publication.
     * 
     * @param publishedDocument the currently published document that will be
     *            rejected
     * @param comment
     * @throws PublishingException
     */
    void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws ClientException;

}