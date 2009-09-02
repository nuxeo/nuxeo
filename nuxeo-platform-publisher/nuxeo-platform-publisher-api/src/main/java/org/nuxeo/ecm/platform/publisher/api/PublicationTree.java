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

    String getTreeTitle();

    void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String title) throws ClientException;

    /**
     * Set the current document on which the tree will be based, if needed. Can be useful for
     * some implementations that need to know on which document the user is.
     * @param currentDocument the current document
     */
    void setCurrentDocument(DocumentModel currentDocument) throws ClientException;

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

    /**
     * Returns {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     * @param publicationNode
     * @return {@code true} if the current user can publish to the specified publicationNode, {@code false} otherwise.
     * @throws ClientException
     */
    boolean canPublishTo(PublicationNode publicationNode) throws ClientException;

    /**
     * Returns {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     * @param publishedDocument
     * @return {@code true} if the current user can unpublish the given publishedDocument, {@code false} otherwise.
     * @throws ClientException
     */
    boolean canUnpublish(PublishedDocument publishedDocument) throws ClientException;

    boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException;

    /**
     * Returns {@code true} if the current user can mnage the publishing of the given publisheddocument, ie. approve or reject the document.
     * @param publishedDocument
     * @return
     * @throws ClientException
     */
    boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException;

    PublishedDocument wrapToPublishedDocument(DocumentModel documentModel) throws ClientException;

    /**
     * Returns {@code true} if the given {@code documentModel} is a PublicationNode of the current tree, {@code false} otherwise.
     * @param documentModel
     * @return
     * @throws ClientException
     */
    boolean isPublicationNode(DocumentModel documentModel) throws ClientException;

    /**
     * Returns a PublicationNode for the current tree built on the given {@code documentModel}. Throws an exception if
     * the given documentModel cannot be a PublicationNode.
     * @param documentModel
     * @return
     * @throws ClientException
     */
    PublicationNode wrapToPublicationNode(DocumentModel documentModel) throws ClientException;

}