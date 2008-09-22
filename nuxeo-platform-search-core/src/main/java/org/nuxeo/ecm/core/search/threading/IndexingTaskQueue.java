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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@code BlockingQueue} implementation used for the indexing tasks.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class IndexingTaskQueue extends LinkedBlockingQueue<Runnable> {

    private static final Log log = LogFactory.getLog(IndexingTaskQueue.class);

    private static final long serialVersionUID = 791623630137571215L;

    private List<Runnable> awaitingTasks = Collections.synchronizedList(new ArrayList<Runnable>());

    private Set<Runnable> tasksInQueue = Collections.synchronizedSet(new HashSet<Runnable>());

    public IndexingTaskQueue() {
        super();
    }

    public IndexingTaskQueue(int capacity) {
        super(capacity);
    }

    @Override
    public Runnable poll() {
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
        return null;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
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
        return null;
    }

    @Override
    public Runnable take() throws InterruptedException {
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
                    return super.take();
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
    }

    @Override
    public boolean offer(Runnable r, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (tasksInQueue.contains(r)) {
            return false;
        }
        if (super.offer(r, timeout, unit)) {
            addTask(r);
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(Runnable r) {
        if (tasksInQueue.contains(r)) {
            return false;
        }
        if (super.offer(r)) {
            addTask(r);
            return true;
        }
        return false;
    }

    @Override
    public void put(Runnable r) throws InterruptedException {
        if (tasksInQueue.contains(r)) {
            return;
        }
        super.put(r);
        addTask(r);
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
                if (!IndexingThreadPoolExecutor.RUNNING_TASKS.contains(r)) {
                    removeAwaitingTask(r);
                    return r;
                }
            }
        }
        return null;
    }

    private boolean canRunTask(Runnable r) {
        return !IndexingThreadPoolExecutor.RUNNING_TASKS.contains(r);
    }

    private void removeTask(Runnable r) {
        tasksInQueue.remove(r);
        log.debug("Removing a task from queue: " + tasksInQueue.size());
    }

    private void addTask(Runnable r) {
        tasksInQueue.add(r);
        log.debug("Adding a task in queue: " + tasksInQueue.size());
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
