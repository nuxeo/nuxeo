/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.work;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

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

    protected final WorkManagerImpl mgr;

    // @GuardedBy("this")
    protected final WorkQueueDescriptorRegistry workQueueDescriptors;

    // @GuardedBy("this")
    protected final Map<String, BlockingQueue<Runnable>> allQueued = new HashMap<>();

    // @GuardedBy("this")
    // queueId -> workId -> work
    protected final Map<String, Map<String, Work>> allScheduled = new HashMap<>();

    // @GuardedBy("this")
    // queueId -> workId -> work
    protected final Map<String, Map<String, Work>> allRunning = new HashMap<>();

    // @GuardedBy("this")
    // queueId -> workId -> work
    protected final Map<String, Map<String, Work>> allCompleted = new HashMap<>();

    public MemoryWorkQueuing(WorkManagerImpl mgr, WorkQueueDescriptorRegistry workQueueDescriptors) {
        this.mgr = mgr;
        this.workQueueDescriptors = workQueueDescriptors;
    }

    @Override
    public synchronized void init() {
        allQueued.clear();
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
    public BlockingQueue<Runnable> initWorkQueue(String queueId) {
        if (allQueued.containsKey(queueId)) {
            throw new IllegalStateException(queueId + " was already configured");
        }
        final BlockingQueue<Runnable> queue = newBlockingQueue(getDescriptor(queueId));
        allQueued.put(queueId, queue);
        return queue;
    }

    public BlockingQueue<Runnable> getWorkQueue(String queueId) {
        if (!allQueued.containsKey(queueId)) {
            throw new IllegalStateException(queueId + " was not configured yet");
        }
        return allQueued.get(queueId);
    }

    @Override
    public synchronized boolean workSchedule(String queueId, Work work) {
        boolean ret = getWorkQueue(queueId).offer(new WorkHolder(work));
        if (ret) {
            getScheduled(queueId).put(work.getId(), work);
        }
        return ret;
    }

    // called synchronized
    protected Map<String, Work> getScheduled(String queueId) {
        Map<String, Work> scheduled = allScheduled.get(queueId);
        if (scheduled == null) {
            allScheduled.put(queueId, scheduled = newScheduledMap());
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
    protected Map<String, Work> getCompleted(String queueId) {
        Map<String, Work> completed = allCompleted.get(queueId);
        if (completed == null) {
            allCompleted.put(queueId, completed = newCompletedMap());
        }
        return completed;
    }

    protected BlockingQueue<Runnable> newBlockingQueue(WorkQueueDescriptor workQueueDescriptor) {
        int capacity = workQueueDescriptor.getCapacity();
        if (capacity <= 0) {
            capacity = -1; // unbounded
        }
        return new MemoryBlockingQueue(capacity);
    }

    protected Map<String, Work> newScheduledMap() {
        return new HashMap<>();
    }

    protected Map<String, Work> newRunningMap() {
        return new HashMap<>();
    }

    protected Map<String, Work> newCompletedMap() {
        return new LinkedHashMap<>();
    }

    @Override
    public synchronized void workRunning(String queueId, Work work) {
        // at this time the work is already taken from the queue by the thread pool
        Work ret = getRunning(queueId).put(work.getId(), work);
        if (ret != null) {
            log.warn("Running a work with the same ID " + work.getId() + " multiple time, already running " + ret +
                    ", new work: " + work);
        }
        getScheduled(queueId).remove(work.getId());
        if (log.isTraceEnabled()) {
            log.trace("Work running " + work.getId() + " on " + queueId + ", total running: " +
                    count(queueId, State.RUNNING));
        }
    }

    @Override
    public synchronized void workCompleted(String queueId, Work work) {
        if (log.isTraceEnabled()) {
            log.trace("Work completed " + work.getId());
        }
        getCompleted(queueId).put(work.getId(), work);
        getRunning(queueId).remove(work.getId());
    }

    @Override
    public Work find(String workId, State state) {
        if (state == null) {
            Work w = findScheduled(workId);
            if (w == null) {
                w = findRunning(workId);
            }
            return w;
        }
        switch (state) {
        case SCHEDULED:
            return findScheduled(workId);
        case RUNNING:
            return findRunning(workId);
        case COMPLETED:
            return findCompleted(workId);
        default:
            return null;
        }
    }

    @Override
    public boolean isWorkInState(String workId, State state) {
        if (state == null) {
            return isScheduled(workId) || isRunning(workId);
        }
        switch (state) {
        case SCHEDULED:
            return isScheduled(workId);
        case RUNNING:
            return isRunning(workId);
        case COMPLETED:
            return isCompleted(workId);
        default:
            return false;
        }
    }

    @Override
    public State getWorkState(String workId) {
        // TODO this is linear, but isScheduled is buggy
        if (findScheduled(workId) != null) {
            return State.SCHEDULED;
        }
        if (isRunning(workId)) {
            return State.RUNNING;
        }
        if (isCompleted(workId)) {
            return State.COMPLETED;
        }
        return null;
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
    public synchronized List<String> listWorkIds(String queueId, State state) {
        if (state == null) {
            return listNonCompletedIds(queueId);
        }
        switch (state) {
        case SCHEDULED:
            return listScheduledIds(queueId);
        case RUNNING:
            return listRunningIds(queueId);
        case COMPLETED:
            return listCompletedIds(queueId);
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public int count(String queueId, State state) {
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
        Map<String, Work> scheduled = allScheduled.get(queueId);
        return scheduled == null ? 0 : scheduled.size();
    }

    protected synchronized int getRunningSize(String queueId) {
        Map<String, Work> running = allRunning.get(queueId);
        return running == null ? 0 : running.size();
    }

    protected synchronized int getCompletedSize(String queueId) {
        Map<String, Work> completed = allCompleted.get(queueId);
        return completed == null ? 0 : completed.size();
    }

    protected synchronized boolean isScheduled(String workId) {
        return find(workId, allScheduled) != null;
    }

    protected synchronized boolean isRunning(String workId) {
        return find(workId, allRunning) != null;
    }

    protected synchronized boolean isCompleted(String workId) {
        return find(workId, allCompleted) != null;
    }

    protected synchronized Work findScheduled(String workId) {
        return find(workId, allScheduled);
    }

    protected synchronized Work findRunning(String workId) {
        return find(workId, allRunning);
    }

    protected synchronized Work findCompleted(String workId) {
        return find(workId, allCompleted);
    }

    private Work find(String workId, Map<String, Map<String, Work>> allWorks) {
        for (Map<String, Work> works : allWorks.values()) {
            Work ret = works.get(workId);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    // called synchronized
    protected List<Work> listScheduled(String queueId) {
        return new ArrayList<>(getScheduled(queueId).values());
    }

    // called synchronized
    protected List<Work> listRunning(String queueId) {
        return new ArrayList<>(getRunning(queueId).values());
    }

    // called synchronized
    protected List<Work> listCompleted(String queueId) {
        return new ArrayList<>(getCompleted(queueId).values());
    }

    // called synchronized
    protected List<String> listScheduledIds(String queueId) {
        return new ArrayList<>(getScheduled(queueId).keySet());
    }

    // called synchronized
    protected List<String> listRunningIds(String queueId) {
        return new ArrayList<>(getRunning(queueId).keySet());
    }

    // called synchronized
    protected List<String> listNonCompletedIds(String queueId) {
        List<String> list = listScheduledIds(queueId);
        list.addAll(listRunningIds(queueId));
        return list;
    }

    // called synchronized
    protected List<String> listCompletedIds(String queueId) {
        return new ArrayList<>(getCompleted(queueId).keySet());
    }

    @Override
    public Work removeScheduled(String queueId, String workId) {
        removeFromScheduledSet(queueId, workId);
        for (Iterator<Runnable> it = getWorkQueue(queueId).iterator(); it.hasNext();) {
            Runnable r = it.next();
            Work w = WorkHolder.getWork(r);
            if (w.getId().equals(workId)) {
                it.remove();
                return w;
            }
        }
        return null;
    }

    protected Work removeFromScheduledSet(String queueId, String workId) {
        for (Iterator<Work> it = getScheduled(queueId).values().iterator(); it.hasNext();) {
            Work w = it.next();
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
        List<Runnable> scheduled = new ArrayList<>();
        getWorkQueue(queueId).drainTo(scheduled);
        for (Runnable r : scheduled) {
            Work work = WorkHolder.getWork(r);
            work.setWorkInstanceState(State.CANCELED);
        }
        getScheduled(queueId).clear();
        return scheduled.size();
    }

    @Override
    public Set<String> getCompletedQueueIds() {
        return new HashSet<>(allCompleted.keySet());
    }

    @Override
    public synchronized void clearCompletedWork(String queueId, long completionTime) {
        Map<String, Work> completed = getCompleted(queueId);
        if (completionTime <= 0) {
            completed.clear();
        } else {
            for (Iterator<Work> it = completed.values().iterator(); it.hasNext();) {
                Work w = it.next();
                if (w.getCompletionTime() < completionTime) {
                    it.remove();
                }
            }
        }
    }

}
