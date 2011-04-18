package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Based on org.nuxeo.ecm.platform.wss.backend.AbstractNuxeoCoreBackend
 *
 * @author Organization: Gagnavarslan ehf
 */
public abstract class AbstractCoreBackend implements Backend {

    private static final Log log = LogFactory.getLog(AbstractCoreBackend.class);

    protected CoreSession session;

    public AbstractCoreBackend() {
        super();
    }

    protected AbstractCoreBackend(CoreSession session) {
        this.session = session;
    }

    /*public void begin() throws ClientException {
        if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.startTransaction();
        }
    }*/

    public CoreSession getSession() throws ClientException {
        return getSession(false);
    }

    public CoreSession getSession(boolean requiredNew) throws ClientException {
        try {
            if (session == null) {
                RepositoryManager rm;
                rm = Framework.getService(RepositoryManager.class);
                session = rm.getDefaultRepository().open();
            } else {
                session.save();
            }
        } catch (Exception e) {
            throw new ClientException("Error while getting session", e);
        }
        return session;
    }

    public void setSession(CoreSession session){
        this.session = session;
    }

    public void destroy() {
        close();
    }

    protected void close() {
        if (session != null) {
            CoreInstance.getInstance().close(session);
            session = null;
        }
    }

    public void discardChanges() throws ClientException {
        discardChanges(false);
    }

    public void discardChanges(boolean release) throws ClientException {
        //TransactionHelper.setTransactionRollbackOnly();
        try {
            if (session != null) {
                try {
                    session.cancel();
                    if (release) {
                        close();
                    }
                } catch (Exception e) {
                    throw new ClientException("Error during discard", e);
                }
            }
        } finally {
            //TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public void saveChanges() throws ClientException {
        saveChanges(false);
    }

    public void saveChanges(boolean release) throws ClientException {
        try {
            if (session != null) {
                try {
                    session.save();
                    if (release) {
                        close();
                    }
                } catch (ClientException e) {
                    throw new ClientException("Error during save", e);
                }
            }
        } finally {
            //TransactionHelper.commitOrRollbackTransaction();
        }

    }

}
