package org.nuxeo.opensocial.container.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentDAO;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentSaverServiceImpl extends DefaultComponent implements
        WebContentSaverService {

    public static final ComponentName NAME = new ComponentName(
            ComponentName.DEFAULT_TYPE,
            "org.nuxeo.opensocial.container.server.service.WebContentSaverService");

    protected Map<String, WebContentSaverDescriptor> savers;

    @SuppressWarnings({ "unchecked" })
    public WebContentData create(WebContentData data, String parentId,
            CoreSession session) throws Exception {

        WebContentSaverDescriptor descriptor = savers.get(data.getAssociatedType());
        WebContentDAO dao = (WebContentDAO) descriptor.getDaoClass().newInstance();
        return dao.create(data, parentId, session);
    }

    @SuppressWarnings("unchecked")
    public WebContentData read(DocumentModel doc, CoreSession session)
            throws Exception {
        WebContentDAO dao;
        dao = (WebContentDAO) getWebContentDAOFor(doc).newInstance();
        return dao.read(doc, session);
    }

    @SuppressWarnings("unchecked")
    public WebContentData update(WebContentData data, CoreSession session)
            throws Exception {
        WebContentSaverDescriptor descriptor = savers.get(data.getAssociatedType());
        WebContentDAO dao = (WebContentDAO) descriptor.getDaoClass().newInstance();
        return dao.update(data, session);
    }

    @SuppressWarnings("unchecked")
    public void delete(WebContentData data, CoreSession session)
            throws Exception {
        WebContentSaverDescriptor descriptor = savers.get(data.getAssociatedType());
        WebContentDAO dao;
        dao = (WebContentDAO) descriptor.getDaoClass().newInstance();
        dao.delete(data, session);
    }

    public Class<?> getWebContentAdapterFor(DocumentModel doc) {
        for (Entry<String, WebContentSaverDescriptor> saver : savers.entrySet()) {
            if (doc.getType().equals(saver.getValue().getDocType())) {
                return saver.getValue().getCoreAdapter();
            }
        }
        return null;
    }

    public Class<?> getWebContentDAOFor(DocumentModel doc) {
        for (Entry<String, WebContentSaverDescriptor> saver : savers.entrySet()) {
            if (doc.getType().equals(saver.getValue().getDocType())) {
                return saver.getValue().getDaoClass();
            }
        }
        return null;
    }

    public String getDocTypeFor(WebContentData data) {
        return savers.get(data.getAssociatedType()).getDocType();
    }

    @Override
    public void activate(ComponentContext context) {
        savers = new HashMap<String, WebContentSaverDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        savers.clear();
        savers = null;
    }

    private void registerSaver(WebContentSaverDescriptor wcsd) {
        savers.put(wcsd.getType(), wcsd);
    }

    private void unregisterSaver(WebContentSaverDescriptor wcsd) {
        if (wcsd != null) {
            savers.remove(wcsd.getType());
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("savers")) {
            WebContentSaverDescriptor wcsd = (WebContentSaverDescriptor) contribution;
            registerSaver(wcsd);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("savers")) {
            WebContentSaverDescriptor wcsd = (WebContentSaverDescriptor) contribution;
            unregisterSaver(wcsd);
        }
    }
}
