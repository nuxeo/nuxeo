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

package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingThread;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;

/**
 * Runnable indexing task.
 * <p>
 * Used to perform an indexing within an <code>IndexingThread</code>. App code
 * executed within this <code>Runnable</code> can share
 * <code>IndexingThread</code> Nuxeo core session and login context.
 * 
 * @see org.nuxeo.ecm.core.search.threading.IndexingThreadImpl
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
class IndexingSingleDocumentTask extends AbstractIndexingTask implements Task {

    private static final Log log = LogFactory.getLog(IndexingSingleDocumentTask.class);

    protected final boolean fulltext;

    public IndexingSingleDocumentTask(DocumentRef docRef,
            String repositoryName, boolean fulltext) {
        super(docRef, repositoryName);
        this.fulltext = fulltext;
    }

    public IndexingSingleDocumentTask(ResolvedResources resources) {
        super(resources);
        fulltext = false;
    }

    public void run() {

        log.debug("Thread: "
                + Thread.currentThread().getName()
                + " ---Indexing task started for "
                + (docRef == null ? "resources: " + resources.getId()
                        : "document: " + docRef));

        // Check if the search service is active
        if (!searchService.isEnabled()) {
            log.warn("Search service is disabled. Indexing cannot be completed.");
            return;
        }

        try {
            if (docRef == null) {
                searchService.index(resources);
                log.debug("Indexing task done for resource: "
                        + resources.getId());
            } else {
                DocumentModel dm = getCoreSession().getDocument(docRef);
                IndexableResources docResources = computeResourcesFor(dm);
                if (docResources != null) {
                    // We are doing asynchronous indexing, thus the document
                    // model can have been deleted in the core repository
                    // before we were able to compute the resources.
                    // We need not log any warning here since
                    // computeResourcesFor already does so
                    searchService.index(docResources, fulltext);
                    log.debug("Indexing task done for document: "
                            + dm.getTitle() + " docRef: " + docRef);
                }
            }
            recycledIfNeeded();
        } catch (Exception e) {
            // log complete stack trace since the Runnable.run interface does
            // not allow to raise any exception
            if (docRef == null && resources == null) {
                log.error("cannot index null document or null resources", e);
            } else if (docRef != null) {
                log.error(String.format(
                        "failed to index document with ref: '%s': %s", docRef,
                        e.getMessage()), e);
            } else {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void recycledIfNeeded() {

        Thread thread = Thread.currentThread();
        if (thread instanceof IndexingThread) {
            IndexingThread idxThread = (IndexingThread) thread;
            if (idxThread.canBeRecycled()) {
                log.debug("recycling thread " + thread.getName());
                thread.interrupt();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof IndexingSingleDocumentTask) {
            IndexingSingleDocumentTask task = (IndexingSingleDocumentTask) obj;

            if (docRef == null) {
                return resources.getId().equals(task.resources.getId());
            } else {
                return docRef.equals(task.docRef)
                        && repositoryName.equals(task.repositoryName);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (docRef == null ? 0 : docRef.hashCode());
        result = 37 * result
                + (repositoryName == null ? 0 : repositoryName.hashCode());
        result = 37 * result + (resources == null ? 0 : resources.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "IndexingSingleDocumentTask for document: " + docRef;
    }

}
