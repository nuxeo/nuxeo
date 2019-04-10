/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.wi.backend;

import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractCoreBackend implements Backend {

    protected CoreSession session;

    public AbstractCoreBackend() {
        super();
    }

    protected AbstractCoreBackend(CoreSession session) {
        this.session = session;
    }

    /*
     * public void begin() throws ClientException { if
     * (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
     * TransactionHelper.startTransaction(); } }
     */

    @Override
    public CoreSession getSession() throws ClientException {
        return getSession(false);
    }

    @Override
    public CoreSession getSession(boolean synchronize) throws ClientException {
        try {
            if (session == null) {
                session = Framework.getService(CoreSession.class);
                String repoURI = Framework.getService(RepositoryManager.class).getDefaultRepository().getName();
                session.connect(repoURI, new HashMap<String,Serializable>());
            } else {
                session.save();
            }
        } catch (Exception e) {
            throw new ClientException("Error while getting session", e);
        }
        if (synchronize) {
            session.save();
        }
        return session;
    }

    @Override
    public void setSession(CoreSession session) {
        this.session = session;
    }

    @Override
    public void destroy() {
        close();
    }

    protected void close() {
        if (session != null) {
            CoreInstance.getInstance().close(session);
            session = null;
        }
    }

    @Override
    public void discardChanges() throws ClientException {
        discardChanges(false);
    }

    public void discardChanges(boolean release) throws ClientException {
        // TransactionHelper.setTransactionRollbackOnly();
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
            // TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Override
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
            // TransactionHelper.commitOrRollbackTransaction();
        }

    }

}
