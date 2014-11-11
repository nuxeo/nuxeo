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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;

/**
 * A {@code BlockingQueue} implementation used for the indexing tasks.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class IndexingTaskQueue extends LinkedBlockingQueue<Runnable> {

    private static final Log log = LogFactory.getLog(IndexingTaskQueue.class);

    private static final long serialVersionUID = 791623630137571215L;

    /**
     * Tasks from the underlying queue that cannot be run when they were taken
     * because a task was already running for the same document
     */
    private final List<Runnable> awaitingTasks = Collections.synchronizedList(new ArrayList<Runnable>());

    /**
     * Store all the tasks managed by this queue: the ones from the {@code
     * awaitingTasks} plus the ones from the underlying queue
     */
    private final Set<Runnable> tasksInQueue = Collections.synchronizedSet(new HashSet<Runnable>());

    /**
     * Read-only list containing the currently running indexing tasks. This list
     * is managed by the {@code IndexingThreadPoolExecutor} associated to it
     */
    private final List<Runnable> currentRunningIndexingTasks;

    /**
     * Lock held by put, offer, etc
     */
    private final ReentrantLock putLock = new ReentrantLock();

    /**
     * Lock held by take, poll, etc
     */
    private final ReentrantLock takeLock = new ReentrantLock();

    public IndexingTaskQueue(List<Runnable> currentRunningIndexingTasks) {
        super();
        this.currentRunningIndexingTasks = currentRunningIndexingTasks;
    }

    public IndexingTaskQueue(int capacity,
            List<Runnable> currentRunningIndexingTasks) {
        super(capacity);
        this.currentRunningIndexingTasks = currentRunningIndexingTasks;
    }

    @Override
    public Runnable poll() {
        takeLock.lock();
        try {
            Runnable r = getAwaitingTask();
            if (r != null) {
                removeTask(r);
                return r;
            }

            while ((r = super.poll()) != null) {
                if (canRunTask(r)) {
                    removeTask(r);
                    return r;
                } else {
                    addAwaitingTask(r);
                }
            }
        } finally {
            takeLock.unlock();
        }
        return null;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        takeLock.lockInterruptibly();
        try {
            Runnable r = getAwaitingTask();
            if (r != null) {
                removeTask(r);
                return r;
            }

            while ((r = super.poll(timeout, unit)) != null) {
                if (canRunTask(r)) {
                    removeTask(r);
                    return r;
                } else {
                    addAwaitingTask(r);
                }
            }
        } finally {
            takeLock.unlock();
        }
        return null;
    }

    @Override
    public Runnable take() throws InterruptedException {
        takeLock.lock();
        try {
            // any awaiting task not yet running?
            Runnable r = getAwaitingTask();
            if (r != null) {
                removeTask(r);
                return r;
            }

            for (;;) {
                // simulate a take by polling a task every 500ms
                r = super.poll(500, TimeUnit.MILLISECONDS);
                if (r != null) {
                    // if we get one, see if we can run it...
                    if (canRunTask(r)) {
                        removeTask(r);
                        return r;
                    } else {
                        addAwaitingTask(r);
                    }
                } else {
                    // ...else check the awaiting tasks list
                    if (awaitingTasks.isEmpty()) {
                        // no more awaiting task, call the default take
                        r = super.take();
                        if (canRunTask(r)) {
                            removeTask(r);
                            return r;
                        } else {
                            addAwaitingTask(r);
                        }
                    } else {
                        // see if we can run an awaiting task
                        r = getAwaitingTask();
                        if (r != null) {
                            removeTask(r);
                            return r;
                        }
                    }
                }
            }
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public boolean offer(Runnable r, long timeout, TimeUnit unit)
            throws InterruptedException {
        putLock.lock();
        try {
            if (tasksInQueue.contains(r)) {
                return false;
            }
            addTask(r); // add it to the tasks list before offer it to the queue
            if (super.offer(r, timeout, unit)) {
                return true;
            } else {
                // if the queue refused the task, remove it from the tasks list
                removeTask(r);
                return false;
            }
        } finally {
            putLock.unlock();
        }
    }

    @Override
    public boolean offer(Runnable r) {
        putLock.lock();
        try {
            if (tasksInQueue.contains(r)) {
                return false;
            }
            addTask(r);
            if (super.offer(r)) {
                return true;
            } else {
                removeTask(r);
                return false;
            }
        } finally {
            putLock.unlock();
        }
    }

    @Override
    public void put(Runnable r) throws InterruptedException {
        putLock.lock();
        try {
            if (tasksInQueue.contains(r)) {
                return;
            }
            addTask(r);
            super.put(r);
        } finally {
            putLock.unlock();
        }
    }

    @Override
    public int size() {
        return tasksInQueue.size();
    }

    /**
     * Returns the first awaiting task of the list not yet already running by
     * the IndexingThreadPool, {@code null} if all tasks are already running.
     *
     */
    private Runnable getAwaitingTask() {
        // any awaiting task in the list?
        if (!awaitingTasks.isEmpty()) {
            for (Runnable r : awaitingTasks) {
                if (canRunTask(r)) {
                    removeAwaitingTask(r);
                    return r;
                }
            }
        }
        return null;
    }

    private boolean canRunTask(Runnable r) {
        boolean canRun = !currentRunningIndexingTasks.contains(r);
        log.debug("Can run task? " + canRun + " -- Running tasks: "
                + currentRunningIndexingTasks);
        return canRun;
    }

    private void removeTask(Runnable r) {
        Task t = (Task) r;
        tasksInQueue.remove(r);
        log.debug("Removing a task from queue: " + tasksInQueue.size()
                + " -- BlockingQueue size: " + super.size() + " -- docRef: "
                + t.getDocumentRef());
    }

    private void addTask(Runnable r) {
        Task t = (Task) r;
        tasksInQueue.add(r);
        log.debug("Adding a task in queue: " + tasksInQueue.size()
                + " -- BlockingQueue size: " + super.size() + " -- docRef: "
                + t.getDocumentRef());
    }

    private void addAwaitingTask(Runnable r) {
        awaitingTasks.add(r);
        log.debug("Adding a new task to the awaitingTasks list: "
                + awaitingTasks.size());
    }

    private void removeAwaitingTask(Runnable r) {
        awaitingTasks.remove(r);
        log.debug("Removing a task from the awaitingTasks list: "
                + awaitingTasks.size());
    }

}
