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
            tasksInQueue.remove(r);
            log.debug("Removing a task from queue: " + tasksInQueue.size());
            return r;
        }

        while ((r = super.poll()) != null) {
            if (!IndexingThreadPoolExecutor.RUNNING_TASKS.contains(r)) {
                tasksInQueue.remove(r);
                log.debug("Removing a task from queue: " + tasksInQueue.size());
                return r;
            } else {
                awaitingTasks.add(r);
            }
        }
        return null;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        Runnable r = getAwaitingTask();
        if (r != null) {
            tasksInQueue.remove(r);
            log.debug("Removing a task from queue: " + tasksInQueue.size());
            return r;
        }

        while ((r = super.poll(timeout, unit)) != null) {
            if (!IndexingThreadPoolExecutor.RUNNING_TASKS.contains(r)) {
                tasksInQueue.remove(r);
                log.debug("Removing a task from queue: " + tasksInQueue.size());
                return r;
            } else {
                awaitingTasks.add(r);
                log.debug("Adding a new task to the awaitingTasks list: "
                        + awaitingTasks.size());
            }
        }
        return null;
    }

    @Override
    public Runnable take() throws InterruptedException {
        Runnable r = getAwaitingTask();
        if (r != null) {
            tasksInQueue.remove(r);
            log.debug("Removing a task from queue: " + tasksInQueue.size());
            return r;
        }

        for (;;) {
            r = super.poll(500, TimeUnit.MILLISECONDS);
            if (r != null) {
                if (!IndexingThreadPoolExecutor.RUNNING_TASKS.contains(r)) {
                    tasksInQueue.remove(r);
                    log.debug("Removing a task from queue: "
                            + tasksInQueue.size());
                    return r;
                } else {
                    awaitingTasks.add(r);
                }
            } else {
                if (awaitingTasks.isEmpty()) {
                    return super.take();
                } else {
                    for (Runnable task : awaitingTasks) {
                        if (!IndexingThreadPoolExecutor.RUNNING_TASKS.contains(task)) {
                            tasksInQueue.remove(r);
                            log.debug("Removing a task from queue: "
                                    + tasksInQueue.size());
                            return task;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean offer(Runnable o, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (tasksInQueue.contains(o)) {
            return false;
        }
        if (super.offer(o, timeout, unit)) {
            tasksInQueue.add(o);
            log.debug("Adding a task in queue: " + tasksInQueue.size());
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(Runnable o) {
        if (tasksInQueue.contains(o)) {
            return false;
        }
        if (super.offer(o)) {
            tasksInQueue.add(o);
            log.debug("Adding a task in queue: " + tasksInQueue.size());
            return true;
        }
        return false;
    }

    @Override
    public void put(Runnable o) throws InterruptedException {
        if (tasksInQueue.contains(o)) {
            return;
        }
        super.put(o);
        tasksInQueue.add(o);
        log.debug("Adding a task in queue: " + tasksInQueue.size());
    }

    private Runnable getAwaitingTask() {
        // any awaiting task in the list?
        if (!awaitingTasks.isEmpty()) {
            for (Runnable r : awaitingTasks) {
                if (!IndexingThreadPoolExecutor.RUNNING_TASKS.contains(r)) {
                    awaitingTasks.remove(r);
                    log.debug("Removing a task from the awaitingTasks list: "
                            + awaitingTasks.size());
                    return r;
                }
            }
        }
        return null;
    }

}
