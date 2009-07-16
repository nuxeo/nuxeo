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

    List<String> getAvailablePublicationTree();

    PublicationTree getPublicationTree(String treeName,
            CoreSession coreSession, Map<String, String> params)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) throws ClientException;

    void unpublish(DocumentModel doc, PublicationNode targetNode)
            throws ClientException;

    boolean isPublishedDocument(DocumentModel documentModel);

    PublicationTree getPublicationTreeFor(DocumentModel doc, CoreSession coreSession) throws ClientException;

}
