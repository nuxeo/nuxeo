package org.nuxeo.ecm.platform.publisher.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;

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

    void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, ValidatorsRule validatorsRule) throws ClientException;

    void release();

    String getIconExpanded();

    String getIconCollapsed();

    /**
     * Computes the list of publishing validators given the document model of
     * the document just published.
     *
     * The string can be prefixed with 'group:' or 'user:'. If there is no
     * prefix (no : in the string) it is assumed to be a user.
     *
     * @param dm a Nuxeo Core document model. (the document that just has been
     *            published)
     * @return a list of principal names.
     * @throws PublishingValidatorException TODO
     */
    String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException;

    /**
     * Returns the registered section validators rule.
     *
     * @return a validators rule
     */
    ValidatorsRule getValidatorsRule() throws PublishingValidatorException;

}
