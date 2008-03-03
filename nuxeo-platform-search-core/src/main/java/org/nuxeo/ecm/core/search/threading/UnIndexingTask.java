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

package org.nuxeo.ecm.core.search.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingHelper;

/**
 * Runnable unindexing task.
 * <p>
 * Used to perform an unindexing within an <code>IndexingThread</code>. App
 * code executed within this <code>Runnable</code> can share
 * <code>IndexingThread</code> Nuxeo core session and login context.
 *
 * @see org.nuxeo.ecm.core.search.threading.IndexingThreadImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class UnIndexingTask extends AbstractIndexingTask {

    private static final Log log = LogFactory.getLog(UnIndexingTask.class);

    public UnIndexingTask(DocumentModel dm, Boolean recursive) {
        super(dm, recursive);
    }

    public void run() {

        log.debug("UnIndexing task runnin'......");

        // Check if the search service is active
        if (!getSearchService().isEnabled()) {
            log.warn("Search service is disabled. UnIndexing cannot be completed.");
            return;
        }

        try {
            if (recursive) {
                IndexingHelper.recursiveUnindex(dm);
            } else {
                getSearchService().deleteAggregatedResources(dm.getId());
            }
        } catch (Exception e) {
            log.error("An error occured while performing the job : "
                    + e.getMessage());
        }

        log.debug("UnIndexing task done......");
    }

}
