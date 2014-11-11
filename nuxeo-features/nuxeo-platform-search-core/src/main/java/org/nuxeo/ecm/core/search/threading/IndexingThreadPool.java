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
 * $Id: IndexingThreadPool.java 30415 2008-02-21 19:06:22Z tdelprat $
 */

package org.nuxeo.ecm.core.search.threading;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.NXSearch;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;
import org.nuxeo.ecm.core.search.threading.task.TaskFactory;

/**
 * Indexing thread pool.
 * <p>
 * Control the amount of indexing threads that will run in a concurrent way.
 * 
 * @see IndexingSingleDocumentTask
 * @see IndexingThreadImpl
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class IndexingThreadPool {

    // :TODO: move the thread pool as a member of the search service.

    private static final Log log = LogFactory.getLog(IndexingThreadPool.class);

    private static final int MIN_POOL_SIZE = 5;

    private static int BROWSING_TASK_QUEUE_SIZE = 100;

    // Here, idle threads waiting for work if IndexingTask pool is full, will
    // wait for the among time specified. Keep this large so that we won't loose
    // tasks;
    private static final long THREAD_KEEP_ALIVE = 5000L;

    private static final ThreadPoolExecutor indexingTpExec;

    private static final ThreadPoolExecutor browsingTpExec;

    private static final ThreadPoolExecutor reindexExec;

    static {
        // Thread pool aught to be on the node which holds the search service.
        int maxPoolSize = NXSearch.getSearchService().getNumberOfIndexingThreads();
        log.info("Indexing thread pool will be initialized with a size pool @ "
                + maxPoolSize);

        indexingTpExec = IndexingThreadPoolExecutor.newInstance(MIN_POOL_SIZE,
                maxPoolSize, THREAD_KEEP_ALIVE, TimeUnit.MILLISECONDS);
        // tpExec.prestartAllCoreThreads();
        log.info("Indexing Thread Pool initialized...");

        browsingTpExec = new ThreadPoolExecutor(MIN_POOL_SIZE, maxPoolSize, 0,
                TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>(
                        BROWSING_TASK_QUEUE_SIZE), new IndexingThreadFactory(),
                new IndexingRejectedExecutionHandler());

        reindexExec = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MICROSECONDS,
                new LinkedBlockingQueue<Runnable>(1),
                new IndexingThreadFactory(),
                new IndexingRejectedExecutionHandler());
    }

    public static void index(DocumentModel dm, Boolean recursive)
            throws IndexingException {
        if (recursive) {
            executeBrowsingTask(TaskFactory.createIndexingBrowseTask(
                    dm.getRef(), dm.getRepositoryName()));
        } else {
            executeIndexingTask(TaskFactory.createIndexingTask(dm.getRef(),
                    dm.getRepositoryName()));
        }
    }

    public static void index(DocumentModel dm, Boolean recursive,
            boolean fulltext) throws IndexingException {
        if (recursive) {
            executeBrowsingTask(TaskFactory.createIndexingBrowseTask(
                    dm.getRef(), dm.getRepositoryName()));
        } else {
            executeIndexingTask(TaskFactory.createIndexingTask(dm.getRef(),
                    dm.getRepositoryName(), fulltext));
        }
    }

    public static void index(ResolvedResources resources)
            throws IndexingException {
        executeIndexingTask(TaskFactory.createIndexingTask(resources));
    }

    public static void unindex(DocumentModel dm, boolean recursive)
            throws IndexingException {
        if (recursive) {
            executeBrowsingTask(TaskFactory.createUnindexingBrowseTask(
                    dm.getRef(), dm.getRepositoryName()));
        } else {
            executeIndexingTask(TaskFactory.createUnindexingTask(dm.getRef(),
                    dm.getRepositoryName()));
        }
    }

    public static int getActiveIndexingTasks() {
        return indexingTpExec.getActiveCount();
    }

    public static long getTotalCompletedIndexingTasks() {
        return indexingTpExec.getCompletedTaskCount();
    }

    public static long getQueueSize() {
        return indexingTpExec.getQueue().size();
    }

    protected static void executeIndexingTask(Task task) {
        synchronized (indexingTpExec) {
            indexingTpExec.execute(task);
        }
    }

    protected static void executeBrowsingTask(Task task)
            throws IndexingException {
        synchronized (browsingTpExec) {
            while (browsingTpExec.getQueue().size() >= BROWSING_TASK_QUEUE_SIZE) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new IndexingException(e);
                }
            }
            browsingTpExec.execute(task);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        indexingTpExec.shutdown();
        super.finalize();
    }

    public static void reindexAll(DocumentModel dm) throws IndexingException {
        executeReindexingTask(TaskFactory.createReindexingAllTask(dm.getRef(),
                dm.getRepositoryName()));
    }

    protected static void executeReindexingTask(Task task) {
        synchronized (reindexExec) {
            reindexExec.execute(task);
        }
    }

    public static boolean isReindexing() {
        return reindexExec.getActiveCount() > 0;
    }

}
