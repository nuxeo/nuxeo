/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.threading.task;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;

/**
 * Factory class creating {@code Task}s to be run by the {@code
 * IndexingThreadPoolExecutor}.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class TaskFactory {

    private TaskFactory() {
        // Factory class
    }

    // IndexingSingleDocumentTasks
    public static Task createIndexingTask(DocumentRef docRef,
            String repositoryName) {
        return new IndexingSingleDocumentTask(docRef, repositoryName, false);
    }

    public static Task createIndexingTask(DocumentRef docRef,
            String repositoryName, boolean fulltext) {
        return new IndexingSingleDocumentTask(docRef, repositoryName, fulltext);
    }

    public static Task createIndexingTask(ResolvedResources resources) {
        return new IndexingSingleDocumentTask(resources);
    }

    // IndexingBrowseTask
    public static Task createIndexingBrowseTask(DocumentRef docRef,
            String repositoryName) {
        return new IndexingBrowseTask(docRef, repositoryName);
    }

    // ReindexingAllTask
    public static Task createReindexingAllTask(DocumentRef docRef,
            String repositoryName) {
        return new ReindexingAllTask(docRef, repositoryName);
    }

    // UnindexingSingleDocumentTask
    public static Task createUnindexingTask(DocumentRef docRef,
            String repositoryName) {
        return new UnIndexingSingleDocumentTask(docRef, repositoryName);
    }

    public static Task createUnindexingBrowseTask(DocumentRef docRef,
            String repositoryName) {
        return new UnIndexingBrowseTask(docRef, repositoryName);
    }

}
