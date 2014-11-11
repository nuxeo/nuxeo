/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     anguenot
 *
 * $Id: IndexingThreadImpl.java 30415 2008-02-21 19:06:22Z tdelprat $
 */

package org.nuxeo.ecm.core.search.threading;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingThread;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.runtime.api.Framework;

/**
 * Indexing dedicated thread.
 * <p>
 * Maintains a Nuxeo core session along with a JAAS session which can be shared
 * in between app code executed within this thread. As well, the thread
 * maintains a search service session.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class IndexingThreadImpl extends Thread implements IndexingThread {

    private static final Log log = LogFactory.getLog(IndexingThreadImpl.class);

    protected CoreSession coreSession;

    protected LoginContext loginCtx;

    protected SearchServiceSession searchServiceSession;

    protected Boolean canRecycle = false;

    protected int recycleRequests = 0;

    // recycle at least every 20*batch_size documents
    protected static final int RECYCLE_INTERVAL = 20;

    private SearchService searchService;

    public IndexingThreadImpl(Runnable r) {
        super(r, "NuxeoIndexingThread");
        log.debug(getThreadNameAndId() + " : Indexing thread with name="
                + getThreadNameAndId());
        // // FIXME Initialze this in a lazy way
        // try {
        // getSearchServiceSession();
        // } catch (Exception e) {
        // log.error(e.getMessage(), e);
        // }
    }

    private String getThreadNameAndId() {
        return "name=" + getName() + " ID= " + getId();
    }

    public CoreSession getCoreSession(String repositoryName) throws Exception {

        if (repositoryName == null) {
            throw new IndexingException(
                    getThreadNameAndId()
                            + " : Repository name is null. No Nuxeo core session can be initialized...");
        }

        login(); // throws an exception if failed.

        // If we got a session not on the right repository or disconnected.
        if (coreSession == null || coreSession.getSessionId() == null
                || !coreSession.getRepositoryName().equals(repositoryName)) {
            log.debug(getThreadNameAndId()
                    + " : (re)connect on Nuxeo core repository with name="
                    + repositoryName);
            closeCoreSession();
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            coreSession = mgr.getRepository(repositoryName).open();
        }
        return coreSession;
    }

    /**
     * Closes the bound core session if exists and still active.
     */
    private void closeCoreSession() {
        try {
            if (coreSession != null && coreSession.getSessionId() != null) {
                log.debug(getThreadNameAndId() + " : "
                        + "Closing Nuxeo Core current session");
                CoreInstance.getInstance().close(coreSession);
            }
        } catch (Throwable t) {
            log.error("Error when cleaning CoreSession bound to indexing thread", t);
        } finally {
            coreSession = null;
        }
    }

    /**
     * Initialize a new JAAS login.
     *
     * @throws Exception
     */
    private void login() throws Exception {
        if (loginCtx == null) {
            loginCtx = Framework.login();
            log.debug(getThreadNameAndId()
                    + " : New JAAS login initialized.....");
        }
    }

    /**
     * Logout.
     */
    private void logout() {
        if (loginCtx != null) {
            try {
                loginCtx.logout();
            } catch (Throwable t) {
                log.error("Error when logging out in indexing thread", t);
            }
            loginCtx = null;
        }
    }

    @Override
    public void interrupt() {
        closeSearchServiceSession();
        closeCoreSession();
        logout();
        super.interrupt();
    }

    // NXP-2107
    /*@Override
    protected void finalize() {
        closeSearchServiceSession();
        closeCoreSession();
        logout();
    }*/

    public SearchServiceSession getSearchServiceSession() throws Exception {
        if (searchServiceSession == null) {
            searchServiceSession = getSearchService().openSession();
        }

        // Raise an exception
        if (searchServiceSession == null) {
            throw new Exception(
                    "Failed to open a new session against search service...");
        }

        return searchServiceSession;
    }

    protected SearchService getSearchService() {
        if (searchService == null) {
            searchService = SearchServiceDelegate.getLocalSearchService();
        }
        return searchService;
    }

    private void closeSearchServiceSession() {
        if (searchServiceSession != null) {
            try {
                getSearchService().closeSession(
                        searchServiceSession.getSessionId());
            } catch (Throwable t) {
                log.error(
                        "Error when cleaning SearchService instance bound to indexing thread",
                        t);
            }
        }
    }

    public Boolean canBeRecycled() {
        return canRecycle;
    }

    public void markForRecycle() {
        recycleRequests += 1;
        if (recycleRequests >= RECYCLE_INTERVAL) {
            recycleRequests = 0;
            canRecycle = true;
        }
    }
}
