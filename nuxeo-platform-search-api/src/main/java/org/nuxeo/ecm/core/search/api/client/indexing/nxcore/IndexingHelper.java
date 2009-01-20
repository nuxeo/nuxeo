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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
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
            SearchService service) throws IndexingException {
        if (log.isDebugEnabled()) {
            log.debug("indexing: " + docModel.getPath() + " docRef: "
                    + docModel.getRef());
        }
        // Pool the task in an indexing thread.
        // Do not recurse in thread and do compute fulltext
        service.indexInThread(docModel, false, true);
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
        docModel = null;

        recursiveIndexOnChildren(ref, session, service);

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
            log.error("An error occured trying to index versions for doc: "
                    + ref, t);
        }
    }

    private static void recursiveIndexOnChildren(DocumentRef parentRef,
            CoreSession session, SearchService service) throws ClientException,
            IndexingException {
        // Get the folderish childrens
        DocumentModelList folderishChildren = getFolderishChildren(parentRef,
                session);
        for (DocumentModel child : folderishChildren) {
            recursiveIndex(child, session, service);
        }
        // Get the non folderish children
        DocumentModelList nonFolderishChildren = getNonFolderishChildren(
                parentRef, session);
        for (DocumentModel child : nonFolderishChildren) {
            index(child, session, service);
        }
    }

    private static DocumentModelList getFolderishChildren(
            DocumentRef parentRef, CoreSession session) throws ClientException {
        DocumentModelList folderishChildren = session.getChildren(parentRef,
                null, new FacetFilter("Folderish", true), null);
        return folderishChildren;
    }

    private static DocumentModelList getNonFolderishChildren(
            DocumentRef parentRef, CoreSession session) throws ClientException {
        DocumentModelList nonFolderishChildren = session.getChildren(parentRef,
                null, new FacetFilter("Folderish", false), null);
        return nonFolderishChildren;
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
            String managedSessionId, SearchService service) throws Exception {
        CoreSession session = CoreInstance.getInstance().getSession(
                managedSessionId);
        if (session == null) {
            throw new IndexingException(String.format(
                    "Managed session id %s is invalid", managedSessionId));
        }
        recursiveIndex(docModel, session, service);
        // do not close the session if it is managed externally
    }

    /**
     * Start a recursive indexing process using a new CoreSession instance that
     * will automatically be closed at the end og the indexing process.
     *
     * @param docModel
     * @param service
     * @throws Exception
     */
    public static void recursiveIndex(DocumentModel docModel,
            SearchService service) throws Exception {
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
            recursiveIndex(docModel, session, service);
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

    private static boolean isBoundToIndexingThread() {
        return Thread.currentThread() instanceof IndexingThread;
    }

}
