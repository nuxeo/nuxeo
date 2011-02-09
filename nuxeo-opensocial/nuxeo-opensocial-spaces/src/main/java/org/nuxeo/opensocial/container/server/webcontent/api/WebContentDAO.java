package org.nuxeo.opensocial.container.server.webcontent.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public interface WebContentDAO<T extends WebContentData> {
    T create(T webContent, String parentId, CoreSession session) throws Exception;

    T read(DocumentModel doc, CoreSession session) throws Exception;

    T update(T webContent, CoreSession session) throws Exception;

    void delete(T webContent, CoreSession session) throws Exception;
}
