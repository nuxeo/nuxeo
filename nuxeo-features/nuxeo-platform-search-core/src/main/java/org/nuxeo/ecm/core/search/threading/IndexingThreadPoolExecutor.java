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

package org.nuxeo.ecm.core.search.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A ThreadPoolExecutor used for the asynchronous indexing tasks.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class IndexingThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Log log = LogFactory.getLog(IndexingThreadPoolExecutor.class);

    private final List<Runnable> currentRunningIndexingTasks;

    public static IndexingThreadPoolExecutor newInstance(int corePoolSize,
            int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        // List of current running indexing tasks the IndexingThreadPoolExecutor
        // and the IndexingTaskQueue will share
        List<Runnable> currentRunningIndexingTasks = Collections.synchronizedList(new ArrayList<Runnable>());
        IndexingTaskQueue workQueue = new IndexingTaskQueue(
                Collections.unmodifiableList(currentRunningIndexingTasks));

        return new IndexingThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, unit, workQueue, new IndexingThreadFactory(),
                new IndexingRejectedExecutionHandler(),
                currentRunningIndexingTasks);
    }

    private IndexingThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler,
            List<Runnable> currentRunningIndexingTasks) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, handler);
        this.currentRunningIndexingTasks = currentRunningIndexingTasks;
    }

    @Override
    public void execute(Runnable command) {
        // assure we have all the threads started before executing a task
        prestartAllCoreThreads();
        super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        currentRunningIndexingTasks.add(r);
        log.debug("Adding a task to the running tasks list: "
                + currentRunningIndexingTasks.size() + " -- Running tasks: "
                + currentRunningIndexingTasks);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        currentRunningIndexingTasks.remove(r);
        log.debug("Removing a task from the running tasks list: "
                + currentRunningIndexingTasks.size() + " -- Running tasks: "
                + currentRunningIndexingTasks);
    }

}
