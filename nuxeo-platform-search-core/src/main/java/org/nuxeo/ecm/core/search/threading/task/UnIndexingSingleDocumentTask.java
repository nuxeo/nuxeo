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
 * $Id: UnIndexingTask.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;

/**
 * Runnable unindexing task.
 * <p>
 * Used to perform an unindexing within an <code>IndexingThread</code>. App code
 * executed within this <code>Runnable</code> can share
 * <code>IndexingThread</code> Nuxeo core session and login context.
 * 
 * @see org.nuxeo.ecm.core.search.threading.IndexingThreadImpl
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
class UnIndexingSingleDocumentTask extends AbstractIndexingTask implements Task {

    private static final Log log = LogFactory.getLog(UnIndexingSingleDocumentTask.class);

    public UnIndexingSingleDocumentTask(DocumentRef docRef,
            String repositoryName) {
        super(docRef, repositoryName);
    }

    public void run() {

        log.debug("UnIndexing task started for document: " + docRef);

        // Check if the search service is active
        if (!searchService.isEnabled()) {
            log.warn("Search service is disabled. UnIndexing cannot be completed.");
            return;
        }

        try {
            DocumentModel dm = getCoreSession().getDocument(docRef);
            searchService.deleteAggregatedResources(dm.getId());
        } catch (Exception e) {
            log.error("An error occured while performing the job : "
                    + e.getMessage());
        }

        log.debug("UnIndexing task done fopr document: " + docRef);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof UnIndexingSingleDocumentTask) {
            UnIndexingSingleDocumentTask task = (UnIndexingSingleDocumentTask) obj;
            return docRef.equals(task.docRef)
                    && repositoryName.equals(task.repositoryName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (docRef == null ? 0 : docRef.hashCode());
        result = 37 * result
                + (repositoryName == null ? 0 : repositoryName.hashCode());
        return result;
    }

}
