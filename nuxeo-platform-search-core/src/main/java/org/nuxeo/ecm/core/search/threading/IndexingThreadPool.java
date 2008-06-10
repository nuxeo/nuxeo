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
import org.nuxeo.ecm.core.search.api.client.SearchService;

/**
 * Indexing thread pool.
 * <p>
 * Control the amount of indexing threads that will run in a concurrent way.
 *
 * @see IndexingTask
 * @see IndexingThreadImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class IndexingThreadPool {

    // :TODO: move the thread pool as a member of the search service.

    private static final Log log = LogFactory.getLog(IndexingThreadPool.class);

    private static final int MIN_POOL_SIZE = 5;

    // Here, idle threads waiting for work if IndexingTask pool is full, will
    // wait for the among time specified. Keep this large so that we won't loose
    // tasks;
    private static final long THREAD_KEEP_ALIVE = 5000L;

    private static final int DEFAULT_QUEUE_SIZE = 100;

    private static final ThreadPoolExecutor tpExec;

    private static final ThreadPoolExecutor reindexExec;

    private static SearchService searchService;

    static {
        // Thread pool aught to be on the node which holds the search service.
        int maxPoolSize = NXSearch.getSearchService().getNumberOfIndexingThreads();
        log.info("Indexing thread pool will be initialized with a size pool @ "
                + maxPoolSize);
        tpExec = new ThreadPoolExecutor(MIN_POOL_SIZE, maxPoolSize,
                THREAD_KEEP_ALIVE, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(DEFAULT_QUEUE_SIZE),
                new IndexingThreadFactory());
        // tpExec.prestartAllCoreThreads();
        log.info("Indexing Thread Pool initialized...");

        reindexExec = new ThreadPoolExecutor(1, 1, 0,
                TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>(1),
                new IndexingThreadFactory());
    }

    public static void setSearchService(SearchService service) {
        searchService = service;
    }

    public static void index(DocumentModel dm, Boolean recursive)
            throws IndexingException {
        execute(new IndexingTask(dm, recursive));
    }

    public static void index(DocumentModel dm, Boolean recursive,
            boolean fulltext) throws IndexingException {
        execute(new IndexingTask(dm, recursive, fulltext));
    }

    public static void index(ResolvedResources resources)
            throws IndexingException {
        execute(new IndexingTask(resources));
    }

    public static void reindexAll(DocumentModel dm, Boolean recursive) {
        Runnable r = new ReindexingAllTask(dm, recursive);
        if (searchService != null) {
            ((AbstractIndexingTask) r).setSearchService(searchService);
        }
        synchronized (reindexExec) {
            reindexExec.execute(r);
        }
    }

    public static void reindexAll(DocumentModel dm, Boolean recursive,
            boolean fulltext) {
        Runnable r = new ReindexingAllTask(dm, recursive, fulltext);
        if (searchService != null) {
            ((AbstractIndexingTask) r).setSearchService(searchService);
        }
        synchronized (reindexExec) {
            reindexExec.execute(r);
        }
    }

    public static boolean isReindexing() {
        return reindexExec.getActiveCount() > 0;
    }

    public static void reindexAll(ResolvedResources resources) {
        Runnable r = new ReindexingAllTask(resources);
        if (searchService != null) {
            ((AbstractIndexingTask) r).setSearchService(searchService);
        }
        synchronized (reindexExec) {
            reindexExec.execute(r);
        }
    }

    public static void unindex(DocumentModel dm, boolean recursive)
            throws IndexingException {
        execute(new UnIndexingTask(dm, recursive));
    }

    public static int getActiveIndexingTasks() {
        return tpExec.getActiveCount();
    }

    public static long getTotalCompletedIndexingTasks() {
        return tpExec.getCompletedTaskCount();
    }

    public static long getQueueSize() {
        return tpExec.getQueue().size();
    }

    protected static void execute(AbstractIndexingTask r)
            throws IndexingException {
        if (searchService != null) {
            r.setSearchService(searchService);
        }
        synchronized (tpExec) {
            // Should be safe to use this here with the synchronized kw.
            while (tpExec.getQueue().size() >= DEFAULT_QUEUE_SIZE) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    throw new IndexingException(ie);
                }
            }
            tpExec.execute(r);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        tpExec.shutdown();
        super.finalize();
    }

}
