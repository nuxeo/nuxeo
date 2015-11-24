package org.nuxeo.opensocial.container.server.webcontent.abs;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.opensocial.container.server.service.WebContentSaverService;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentDAO;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stéphane Fourrier
 */
public abstract class AbstractWebContentDAO<T extends WebContentData>
        implements WebContentDAO<T> {

    public T create(T data, String parentId, CoreSession session)
            throws Exception {
        return create(data, data.getName(), parentId, session);
    }

    @SuppressWarnings("unchecked")
    protected T create(T data, String webContentName, String parentId, CoreSession session) throws Exception {
        // TODO Remove call to the service !
        WebContentSaverService service = Framework.getService(WebContentSaverService.class);

        String unitPath = session.getDocument(new IdRef(parentId)).getPathAsString();
        // TODO data.getName() + date
        DocumentModel doc = session.createDocumentModel(unitPath,
                webContentName, service.getDocTypeFor(data));

        doc = session.createDocument(doc);

        WebContentAdapter<T> adapter = doc.getAdapter(WebContentAdapter.class);
        adapter.feedFrom(data);

        doc = session.saveDocument(doc);

        return adapter.getData();
    }

    @SuppressWarnings("unchecked")
    public T read(DocumentModel doc, CoreSession session)
            throws ClientException {
        WebContentAdapter<T> adapter = doc.getAdapter(WebContentAdapter.class);

        return adapter.getData();
    }

    @SuppressWarnings("unchecked")
    public T update(T webContent, CoreSession session) throws ClientException {
        DocumentModel doc = session.getDocument(new IdRef(webContent.getId()));

        WebContentAdapter<T> adapter = doc.getAdapter(WebContentAdapter.class);
        adapter.feedFrom(webContent);

        session.saveDocument(doc);
        return webContent;
    }

    public void delete(T webContent, CoreSession session)
            throws ClientException {
        session.removeDocument(new IdRef(webContent.getId()));
    }
}
