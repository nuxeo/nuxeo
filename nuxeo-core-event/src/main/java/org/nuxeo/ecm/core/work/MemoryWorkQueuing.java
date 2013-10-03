/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;

/**
 * Implementation of a {@link WorkQueuing} using in-memory queuing.
 *
 * @since 5.8
 */
public class MemoryWorkQueuing implements WorkQueuing {

    private static final Log log = LogFactory.getLog(MemoryWorkQueuing.class);

    // @GuardedBy("this")
    protected final WorkQueueDescriptorRegistry workQueueDescriptors;

    // @GuardedBy("this")
    protected Map<String, BlockingQueue<Runnable>> allScheduled = new HashMap<String, BlockingQueue<Runnable>>();

    // @GuardedBy("this")
    protected Map<String, Map<String, Work>> allRunning = new HashMap<String, Map<String, Work>>();

    // @GuardedBy("this")
    protected Map<String, List<Work>> allCompleted = new HashMap<String, List<Work>>();

    public MemoryWorkQueuing(WorkQueueDescriptorRegistry workQueueDescriptors) {
        this.workQueueDescriptors = workQueueDescriptors;
    }

    @Override
    public synchronized void init() {
        allScheduled.clear();
        allRunning.clear();
        allCompleted.clear();
    }

    // called synchronized
    protected WorkQueueDescriptor getDescriptor(String queueId) {
        WorkQueueDescriptor descriptor = workQueueDescriptors.get(queueId);
        if (descriptor == null) {
            throw new IllegalArgumentException("No such work queue: " + queueId);
        }
        return descriptor;
    }

    @Override
    public synchronized BlockingQueue<Runnable> getScheduledQueue(String queueId) {
        BlockingQueue<Runnable> scheduled = allScheduled.get(queueId);
        if (scheduled == null) {
            allScheduled.put(queueId,
                    scheduled = newBlockingQueue(getDescriptor(queueId)));
        }
        return scheduled;
    }

    // called synchronized
    protected Map<String, Work> getRunning(String queueId) {
        Map<String, Work> running = allRunning.get(queueId);
        if (running == null) {
            allRunning.put(queueId, running = newRunningMap());
        }
        return running;
    }

    // called synchronized
    protected List<Work> getCompleted(String queueId) {
        List<Work> completed = allCompleted.get(queueId);
        if (completed == null) {
            allCompleted.put(queueId, completed = newCompletedList());
        }
        return completed;
    }

    protected BlockingQueue<Runnable> newBlockingQueue(
            WorkQueueDescriptor workQueueDescriptor) {
        int capacity = workQueueDescriptor.capacity;
        if (workQueueDescriptor.usePriority) {
            log.warn("Priority queues are now deprecated and function as regular queues");
        }
        if (capacity > 0) {
            return new NuxeoLinkedBlockingQueue<Runnable>(
                    workQueueDescriptor.capacity);
        }
        // default unbounded queue
        return new LinkedBlockingQueue<Runnable>();
    }

    protected HashMap<String, Work> newRunningMap() {
        return new HashMap<String, Work>();
    }

    protected List<Work> newCompletedList() {
        return new LinkedList<Work>();
    }

    @Override
    public synchronized void workRunning(String queueId, Work work) {
        // work is already taken from the scheduled queue
        // by the thread pool executor
        getRunning(queueId).put(work.getId(), work);
    }

    @Override
    public synchronized void workCompleted(String queueId, Work work) {
        getRunning(queueId).remove(work.getId());
        getCompleted(queueId).add(work);
    }

    /**
     * A LinkedBlockingQueue that blocks on "offer" and prevent starvation
     * deadlock on reentrant call.
     *
     */
    public class NuxeoLinkedBlockingQueue<T> extends LinkedBlockingQueue<T> {

        private static final long serialVersionUID = 1L;

        private final ReentrantLock limitedPutLock = new ReentrantLock();

        private final int limitedCapacity;

        public NuxeoLinkedBlockingQueue(int capacity) {
            // Allocate more space to prevent starvation dead lock
            // because a worker can add a new job to the queue.
            super(2 * capacity);
            limitedCapacity = capacity;
        }

        /**
         * Block until there are enough remaining capacity to put the entry.
         */
        public void limitedPut(T e) throws InterruptedException {
            limitedPutLock.lockInterruptibly();
            try {
                while (remainingCapacity() < limitedCapacity) {
                    Thread.sleep(100);
                }
                put(e);
            } finally {
                limitedPutLock.unlock();
            }

        }

        @Override
        public boolean offer(T e) {
            // Patch to turn non blocking offer into a blocking put
            try {
                if (Thread.currentThread().getName().startsWith(
                        WorkManagerImpl.THREAD_PREFIX)) {
                    // use the full queue capacity for reentrant call
                    put(e);
                } else {
                    // put only if there are enough remaining capacity
                    limitedPut(e);
                }
                return true;
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

    }

    @Override
    public Work find(String workId, State state) {
        if (state == null || state == State.SCHEDULED) {
            Work w = findScheduled(workId);
            if (w != null) {
                return w;
            }
        }
        if (state == null || state == State.RUNNING) {
            return findRunning(workId);
        }
        if (state == State.COMPLETED) {
            return findCompleted(workId);
        }
        throw new IllegalArgumentException(String.valueOf(state));
    }

    @Override
    public State getWorkState(String workId) {
        if (findScheduled(workId) != null) {
            return State.SCHEDULED;
        }
        if (findRunning(workId) != null) {
            return State.RUNNING;
        }
        Work w = findCompleted(workId);
        // COMPLETED / FAILED / CANCELED
        return w == null ? null : w.getWorkInstanceState();
    }

    @Override
    public synchronized List<Work> listWork(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return listScheduled(queueId);
        case RUNNING:
            return listRunning(queueId);
        case COMPLETED:
            return listCompleted(queueId);
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public int getQueueSize(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return getScheduledSize(queueId);
        case RUNNING:
            return getRunningSize(queueId);
        case COMPLETED:
            return getCompletedSize(queueId);
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    protected synchronized int getScheduledSize(String queueId) {
        BlockingQueue<Runnable> scheduled = allScheduled.get(queueId);
        return scheduled == null ? 0 : scheduled.size();
    }

    protected synchronized int getRunningSize(String queueId) {
        Map<String, Work> running = allRunning.get(queueId);
        return running == null ? 0 : running.size();
    }

    protected synchronized int getCompletedSize(String queueId) {
        List<Work> completed = allCompleted.get(queueId);
        return completed == null ? 0 : completed.size();
    }

    protected synchronized Work findScheduled(String workId) {
        for (BlockingQueue<Runnable> scheduled : allScheduled.values()) {
            for (Runnable r : scheduled) {
                Work w = WorkHolder.getWork(r);
                if (w.getId().equals(workId)) {
                    return w;
                }
            }
        }
        return null;
    }

    protected synchronized Work findRunning(String workId) {
        for (Map<String, Work> running : allRunning.values()) {
            Work w = running.get(workId);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    protected synchronized Work findCompleted(String workId) {
        for (List<Work> completed : allCompleted.values()) {
            for (Work w : completed) {
                if (w.getId().equals(workId)) {
                    return w;
                }
            }
        }
        return null;
    }

    // no synchronized as scheduled queue is thread-safe
    protected List<Work> listScheduled(String queueId) {
        BlockingQueue<Runnable> scheduled = getScheduledQueue(queueId);
        List<Work> list = new ArrayList<Work>(scheduled.size());
        for (Runnable r : scheduled) {
            Work w = WorkHolder.getWork(r);
            list.add(w);
        }
        return list;
    }

    // called synchronized
    protected List<Work> listRunning(String queueId) {
        return new ArrayList<Work>(getRunning(queueId).values());
    }

    // called synchronized
    protected List<Work> listCompleted(String queueId) {
        return new ArrayList<Work>(getCompleted(queueId));
    }

    @Override
    public Work removeScheduled(String queueId, String workId) {
        for (Iterator<Runnable> it = getScheduledQueue(queueId).iterator(); it.hasNext();) {
            Runnable r = it.next();
            Work w = WorkHolder.getWork(r);
            if (w.getId().equals(workId)) {
                it.remove();
                return w;
            }
        }
        return null;
    }

    @Override
    public int setSuspending(String queueId) {
        // for in-memory queuing, there's no suspend
        // drain scheduled queue and mark work canceled
        List<Runnable> scheduled = new ArrayList<Runnable>();
        getScheduledQueue(queueId).drainTo(scheduled);
        for (Runnable r : scheduled) {
            Work work = WorkHolder.getWork(r);
            work.setWorkInstanceState(State.CANCELED);
        }
        return scheduled.size();
    }

    @Override
    public Set<String> getCompletedQueueIds() {
        return new HashSet<String>(allCompleted.keySet());
    }

    @Override
    public synchronized void clearCompletedWork(String queueId,
            long completionTime) {
        List<Work> completed = getCompleted(queueId);
        if (completionTime <= 0) {
            completed.clear();
        } else {
            for (Iterator<Work> it = completed.iterator(); it.hasNext();) {
                Work w = it.next();
                if (w.getCompletionTime() < completionTime) {
                    it.remove();
                }
            }
        }
    }

}
