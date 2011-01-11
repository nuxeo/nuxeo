package org.nuxeo.ecm.webdav.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Based on org.nuxeo.ecm.platform.wss.backend.AbstractNuxeoCoreBackend
 *
 * @author Organization: Gagnavarslan ehf
 */
public abstract class AbstractCoreBackend implements WebDavBackend {

    private static final Log log = LogFactory.getLog(AbstractCoreBackend.class);

    protected CoreSession session;

    public AbstractCoreBackend() {
        super();
    }

    public CoreSession getSession() throws ClientException {
        try {
            if (session == null) {
                RepositoryManager rm;
                rm = Framework.getService(RepositoryManager.class);
                session = rm.getDefaultRepository().open();
            }
        } catch (Exception e) {
            throw new ClientException("Error while getting session", e);
        }
        return session;
    }

    public boolean exists(String location) {
        try {
            DocumentModel doc = resolveLocation(location);
            if(doc != null){
                return true;
            } else {
                return false;
            }
        } catch (ClientException e) {
            return false;
        }
    }

    public boolean exists(DocumentRef ref) throws ClientException {
        return getSession().exists(ref);
    }

    protected void close() {
        if (session != null) {
            CoreInstance.getInstance().close(session);
            session = null;
        }
    }

    public void discardChanges() throws ClientException {
        discardChanges(true);
    }

    public void discardChanges(boolean release) throws ClientException {
        if (session != null) {
            session.cancel();
            if (release) {
                close();
            }
        }
    }

    public void saveChanges() throws ClientException {
        saveChanges(true);
    }

    public void saveChanges(boolean release) throws ClientException {
        if (session != null) {
            session.save();
            if (release) {
                close();
            }
        }
    }

}
