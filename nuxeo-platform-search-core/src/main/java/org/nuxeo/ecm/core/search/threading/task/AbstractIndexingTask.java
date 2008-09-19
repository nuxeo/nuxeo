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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: AbstractIndexingTask.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingThread;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;

/**
 * Absract Indexing Task.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * 
 */
abstract class AbstractIndexingTask implements Task {

    private static final Log log = LogFactory.getLog(AbstractIndexingTask.class);

    protected final DocumentRef docRef;

    protected final String repositoryName;

    protected final ResolvedResources resources;

    protected final static SearchService searchService;

    static {
        // Initialize the SearchService, all the tasks will share the same one.
        SearchService service;
        try {
            service = SearchServiceDelegate.getLocalSearchService();
        } catch (Exception e) {
            // Fallback on remote search service.
            log.warn("Local search service is not available : trying to lookup remote instead");
            service = SearchServiceDelegate.getRemoteSearchService();
        }
        if (service == null) {
            throw new IllegalStateException(
                    "Unable to initialize the SearchService");
        }
        searchService = service;
    }

    protected AbstractIndexingTask(DocumentRef docRef, String repositoryName) {
        this.docRef = docRef;
        this.repositoryName = repositoryName;
        resources = null;
    }

    protected AbstractIndexingTask(ResolvedResources resources) {
        this.docRef = null;
        this.repositoryName = null;
        this.resources = resources;
    }

    public DocumentRef getDocumentRef() {
        return docRef;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public ResolvedResources getResources() {
        return resources;
    }

    protected CoreSession getCoreSession() throws IndexingException {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof IndexingThread) {
            IndexingThread thread = (IndexingThread) currentThread;
            try {
                return thread.getCoreSession(repositoryName);
            } catch (Exception e) {
                throw new IndexingException(
                        "Unable to get a core session for repository name: "
                                + repositoryName);
            }
        } else {
            // should not happen
            throw new IndexingException("Not in an IndexingThread");
        }
    }

    protected static IndexableResources computeResourcesFor(DocumentModel dm)
            throws IndexingException {
        return IndexableResourcesFactory.computeResourcesFor(dm);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        log.debug("Finalize...........................");
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}
