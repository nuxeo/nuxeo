package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.List;
import java.util.Map;

/**
 * Main publication Service *
 *
 * @author tiry
 */
public interface PublisherService {

    final String DOMAIN_NAME_KEY = "DomainName";

    List<String> getAvailablePublicationTree();

    /**
     * Returns a {@code Map} with tree name as key and tree title as value.
     * @return
     */
    Map<String ,String> getAvailablePublicationTrees();

    PublicationTree getPublicationTree(String treeName,
            CoreSession coreSession, Map<String, String> params)
            throws ClientException, PublicationTreeNotAvailable;

    PublicationTree getPublicationTree(String treeName,
            CoreSession coreSession, Map<String, String> params, DocumentModel currentDocument)
            throws ClientException, PublicationTreeNotAvailable;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) throws ClientException;

    void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    boolean isPublishedDocument(DocumentModel documentModel);

    PublicationTree getPublicationTreeFor(DocumentModel doc, CoreSession coreSession) throws ClientException;

    PublicationNode wrapToPublicationNode(DocumentModel documentModel, CoreSession coreSession) throws ClientException, PublicationTreeNotAvailable;

    Map<String, String> getParametersFor(String treeConfigName);

}
