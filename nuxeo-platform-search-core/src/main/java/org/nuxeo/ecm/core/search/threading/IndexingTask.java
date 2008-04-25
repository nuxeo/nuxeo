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
 * $Id: IndexingTask.java 29924 2008-02-06 18:56:25Z tdelprat $
 */

package org.nuxeo.ecm.core.search.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingHelper;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingThread;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;

/**
 * Runnable indexing task.
 * <p>
 * Used to perform an indexing within an <code>IndexingThread</code>. App
 * code executed within this <code>Runnable</code> can share
 * <code>IndexingThread</code> Nuxeo core session and login context.
 *
 * @see org.nuxeo.ecm.core.search.threading.IndexingThreadImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class IndexingTask extends AbstractIndexingTask {

    private static final Log log = LogFactory.getLog(IndexingTask.class);

    public IndexingTask(DocumentModel dm, Boolean recursive) {
        super(dm, recursive);
    }

    public IndexingTask(DocumentModel dm, Boolean recursive, boolean fulltext) {
        super(dm, recursive, fulltext);
    }

    public IndexingTask(ResolvedResources resources) {
        super(resources);
    }

    public void run() {

        log.debug("Indexing task started");

        // Check if the search service is active
        if (!getSearchService().isEnabled()) {
            log.warn("Search service is disabled. Indexing cannot be completed.");
            return;
        }

        try {
            if (dm == null) {
                getSearchService().index(resources);
                log.debug("Indexing task done for resource: " + resources.getId());
            } else {
                if (recursive) {
                    IndexingHelper.recursiveIndex(dm);
                    log.debug("Indexing task done for document: " + dm.getTitle());

                } else {
                    IndexableResources docResources = computeResourcesFor(dm);
                    if (docResources != null) {
                        // We are doing asynchronous indexing, thus the document
                        // model can have been deleted in the core repository
                        // before we were able to compute the resources.
                        // We need not log any warning here since
                        // computeResourcesFor already does so
                        getSearchService().index(docResources, fulltext);
                        log.debug("Indexing task done for document: " + dm.getTitle());
                    }
                }
            }
            recycledIfNeeded();
        } catch (Exception e) {
            // log complete stack trace since the Runnable.run interface does
            // not allow to raise any exception
            if (dm == null && resources == null) {
                log.error("cannot index null document or null resources", e);
            } else if (dm != null) {
                log.error(String.format(
                        "failed to index document '%s' with type '%s': %s",
                        dm.getTitle(), dm.getType(), e.getMessage()), e);
            } else {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void recycledIfNeeded()
    {
        boolean needRecycle;

        Thread thread = Thread.currentThread();
        if (thread instanceof IndexingThread) {
            IndexingThread idxThread = (IndexingThread) thread;
            if (idxThread.canBeRecycled())
            {
                log.debug("recycling thread " + thread.getName());
                thread.interrupt();
            }
        }
    }

}
