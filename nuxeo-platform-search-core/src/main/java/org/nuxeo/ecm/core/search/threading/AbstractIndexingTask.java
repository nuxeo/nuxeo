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

package org.nuxeo.ecm.core.search.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;

/**
 * Abstract Indexing Task.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractIndexingTask implements Runnable {

    protected static final String PREFIXED_NAME = "NuxeoCoreIndexing_";

    private static final Log log = LogFactory.getLog(AbstractIndexingTask.class);

    protected final DocumentModel dm;

    protected final boolean recursive;

    protected final boolean fulltext;

    protected final ResolvedResources resources;

    protected SearchService searchService;

    protected AbstractIndexingTask(DocumentModel dm, Boolean recursive) {
        this.dm = dm;
        this.recursive = recursive;
        fulltext = true;
        resources = null;
    }

    protected AbstractIndexingTask(DocumentModel dm, Boolean recursive,
            boolean fulltext) {
        this.dm = dm;
        this.recursive = recursive;
        this.fulltext = fulltext;
        resources = null;
    }

    protected AbstractIndexingTask(ResolvedResources resources) {
        dm = null;
        recursive = false; // useless in this case
        fulltext = false; // useless in this case
        this.resources = resources;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    protected SearchService getSearchService() {
        if (searchService == null) {
            // XXX We assume that the thread pool will always be instanciated on
            // the node running the search service.
            try {
                searchService = SearchServiceDelegate.getLocalSearchService();
            } catch (Exception e) {
                // Fallback on remote search service.
                log.warn("Local search service is not available : trying to lookup remote instead");
                searchService = SearchServiceDelegate.getRemoteSearchService();
            }
        }
        return searchService;
    }

    protected static IndexableResources computeResourcesFor(DocumentModel dm) {
        return IndexableResourcesFactory.computeResourcesFor(dm);
    }

    @Override
    protected void finalize() throws Throwable {
        log.debug("Finalize...........................");
        super.finalize();
    }

}
