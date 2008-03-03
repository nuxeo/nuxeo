/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.indexing.nxcore;

import java.util.List;
import java.util.NoSuchElementException;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.runtime.api.Framework;

/**
 * This class provides helpers for (re)(un)indexing processes
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public final class IndexingHelper {

    private static final Log log = LogFactory.getLog(IndexingHelper.class);

    // Utility class.
    private IndexingHelper() {
    }

    private static void index(DocumentModel docModel, CoreSession session,
            SearchService service) throws IndexingException, ClientException {
        if (log.isDebugEnabled()) {
            log.debug("indexing: " + docModel.getPath());
        }
        // Pool the task in an indexing thread.
        // Do not recurse in thread and do compute fulltext
        service.indexInThread(docModel, false, true);
    }

    /**
     * Delete one document from the document indexes.
     *
     * @param docModel : the actual document model to unindex.
     * @param service : the search service.
     * @throws IndexingException
     */
    private static void unindex(DocumentModel docModel, SearchService service)
            throws IndexingException {
        if (log.isDebugEnabled()) {
            log.debug("Unindexing: " + docModel.getPathAsString());
        }
        // Document model identifier is the aggregated key for now.
        service.deleteAggregatedResources(docModel.getId());
    }

    private static void recursiveIndex(DocumentModel docModel,
            CoreSession session, SearchService service)
            throws IndexingException, ClientException {

        try {
            index(docModel, session, service);
        } catch (Throwable t) {
            if (docModel == null) {
                return;
            }
            log.error("Indexing failed for document " + docModel.getRef(), t);
        }
        DocumentRef ref = docModel.getRef();
        Boolean isProxy = docModel.isProxy();
        Path docPath = docModel.getPath();
        docModel=null;

        try {
            for (DocumentModel child : session.getChildrenIterator(ref)) {
                recursiveIndex(child, session, service);
            }
        } catch (Exception e) {
            log.error("Doc children iterator inconsistency under "
                    + docPath, e);
        }

        // NXP-1463 fix.
        // Document proxies might have indexable versions some day in the future
        // in the meanwhile, asking for versions fires an exception in the core,
        // and this kills the bean
        if (isProxy) {
            log.debug("Indexed DocumentModel is a proxy. Don't fetch versions");
            return;
        }

        try {
            List<DocumentModel> versions = session.getVersions(ref);
            if (versions != null) {
                for (DocumentModel version : versions) {
                    index(version, session, service);
                }
            }
        } catch (NoSuchElementException e) {
            log.warn("Doc versions iterator inconsistency for versions of "
                    + docPath, e);
        } catch (Throwable t) {
            // XXX do not shield like this but no way to catch the base
            // exception here.
            // TODO Not enough protection. Rebuild a session.
            // Trying to index a proxy in the section space.
            log.error("An error occured trying to index versions...", t);
        }
    }

    /* Ugly copy-paste */
    protected static LoginContext login() throws IndexingException {
        try {
            if (!isBoundToIndexingThread()) {
                LoginContext systemSession = Framework.login();
                return systemSession;
            }
            log.debug("Bound to an IndexingThead. No need reauthenticate....");
            return null;
        } catch (Exception e) {
            throw new IndexingException("Login failed: " + e.getMessage(), e);
        }
    }

    protected static void logout(LoginContext systemSession) throws Exception {
        if (systemSession != null && !isBoundToIndexingThread()) {
            systemSession.logout();
        }
    }

    /**
     * Start a recursive indexing process using a single existing CoreSession
     * instance identified by 'managedSessionId'. The session will not be
     * closed. This method is useful for synchronous indexing, when we want to
     * index a newly created document as part as the same transaction that
     * created the document.
     *
     * @param docModel
     * @param managedSessionId
     * @throws Exception
     */
    public static void recursiveIndex(DocumentModel docModel,
            String managedSessionId) throws Exception {
        CoreSession session = CoreInstance.getInstance().getSession(
                managedSessionId);
        if (session == null) {
            throw new IndexingException(String.format(
                    "Managed session id %s is invalid", managedSessionId));
        }
        recursiveIndex(docModel, session);
        // do not close the session if it is managed externally
    }

    /**
     * Start a recursive indexing process using a new CoreSession instance that
     * will automatically be closed at the end og the indexing process.
     *
     * @param docModel
     * @throws Exception
     */
    public static void recursiveIndex(DocumentModel docModel) throws Exception {
        // we are in the processs of external asynchronous indexing, use a
        // SystemLogin to do the job
        LoginContext sysSession = login();

        CoreSession session;
        if (!isBoundToIndexingThread()) {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            session = mgr.getRepository(docModel.getRepositoryName()).open();
            log.debug("Open a new core session");
        } else {
            session = ((IndexingThread) Thread.currentThread()).getCoreSession(docModel.getRepositoryName());
            log.debug("Bound to an indexing thread. Using shared Nuxeo core connection...");
        }
        try {
            recursiveIndex(docModel, session);
        } finally {
            if (session != null && !isBoundToIndexingThread()) {
                log.debug("Closing core session");
                try {
                    CoreInstance.getInstance().close(session);
                } catch (Exception e) {
                    // Here, let's not make the txn fail if closing the session
                    // fails.
                    log.error("Failed to close core session.... for docModel="
                            + docModel.getPathAsString(), e);
                }
            }
            // Bound to an indexing thread.
            if (sysSession != null) {
                logout(sysSession);
            }
        }
    }

    public static void recursiveUnindex(DocumentModel docModel)
            throws Exception {
        // we are in the processs of external asynchronous indexing, use a
        // SystemLogin to do the job
        LoginContext sysSession = login();

        CoreSession session;
        if (!isBoundToIndexingThread()) {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            session = mgr.getRepository(docModel.getRepositoryName()).open();
            log.debug("Open a new core session");
        } else {
            session = ((IndexingThread) Thread.currentThread()).getCoreSession(docModel.getRepositoryName());
            log.debug("Bound to an indexing thread. Using shared Nuxeo core connection...");
        }
        try {
            recursiveUnIndex(docModel, session);
        } finally {
            if (session != null && !isBoundToIndexingThread()) {
                log.debug("Closing core session");
                try {
                    CoreInstance.getInstance().close(session);
                } catch (Exception e) {
                    // Here, let's not make the txn fail if closing the session
                    // fails.
                    log.error("Failed to close core session.... for docModel="
                            + docModel.getPathAsString(), e);
                }
            }
            // Bound to an indexing thread.
            if (sysSession != null) {
                logout(sysSession);
            }
        }
    }

    private static void recursiveUnIndex(DocumentModel docModel,
            CoreSession session, SearchService service)
            throws IndexingException {

        // Here, the document model might already be unindexed by a synchronous
        // listener. Let's ask for it again anyway.
        unindex(docModel, service);

        // Perform a query to get all matching children under the path
        ResultSet rset = null;
        try {
            rset = service.searchQuery(createUnindexPathQuery(docModel), 0, 100);
        } catch (SearchException se) {
            throw new IndexingException(se);
        } catch (QueryException qe) {
            throw new IndexingException(qe);
        }

        if (rset == null) {
            if (log.isDebugEnabled()) {
                log.debug("No children to unindex for dm=");
            }
            return;
        }

        while (true) {
            for (ResultItem item : rset) {
                String key = (String) item.get(BuiltinDocumentFields.FIELD_DOC_UUID);
                if (key != null) {
                    service.deleteAggregatedResources(key);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No UUID indexed for dm=");
                    }
                }
            }

            if (rset.hasNextPage()) {
                try {
                    rset = rset.nextPage();
                } catch (SearchException se) {
                    throw new IndexingException(se);
                }
            } else {
                break;
            }
        }
    }

    private static ComposedNXQuery createUnindexPathQuery(DocumentModel dm) {
        String queryStr = "SELECT * FROM Document WHERE "
                + BuiltinDocumentFields.FIELD_DOC_PATH + " STARTSWITH " + "'"
                + dm.getPathAsString() + "'";
        return new ComposedNXQueryImpl(queryStr);
    }

    private static void recursiveIndex(DocumentModel docModel,
            CoreSession session) throws Exception {
        recursiveIndex(docModel, session,
                SearchServiceDelegate.getRemoteSearchService());
    }

    private static void recursiveUnIndex(DocumentModel docModel,
            CoreSession session) throws Exception {
        recursiveUnIndex(docModel, session,
                SearchServiceDelegate.getRemoteSearchService());
    }

    private static boolean isBoundToIndexingThread() {
        return Thread.currentThread() instanceof IndexingThread;
    }

}
