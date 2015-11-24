package org.nuxeo.opensocial.container.server.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public interface WebContentSaverService {
    WebContentData create(WebContentData data, String parentId,
            CoreSession session) throws Exception;

    WebContentData read(DocumentModel doc, CoreSession session)
            throws Exception;

    WebContentData update(WebContentData data, CoreSession session)
            throws Exception;

    void delete(WebContentData data, CoreSession session) throws Exception;

    Class<?> getWebContentAdapterFor(DocumentModel doc);

    Class<?> getWebContentDAOFor(DocumentModel doc);

    String getDocTypeFor(WebContentData data);
}
