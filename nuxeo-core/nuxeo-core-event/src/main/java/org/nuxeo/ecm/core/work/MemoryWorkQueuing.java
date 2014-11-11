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

    // @GuardedBy("this")
    protected final WorkQueueDescriptorRegistry workQueueDescriptors;

    // @GuardedBy("this")
    protected Map<String, BlockingQueue<Runnable>> allScheduled = new HashMap<String, BlockingQueue<Runnable>>();

    // @GuardedBy("this")
    // queueId -> workId -> work
    protected Map<String, Map<String, Work>> allRunning = new HashMap<String, Map<String, Work>>();

    // @GuardedBy("this")
    // queueId -> workId -> work
    protected Map<String, Map<String, Work>> allCompleted = new HashMap<String, Map<String, Work>>();

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
    protected Map<String, Work> getCompleted(String queueId) {
        Map<String, Work> completed = allCompleted.get(queueId);
        if (completed == null) {
            allCompleted.put(queueId, completed = newCompletedMap());
        }
        return completed;
    }

    protected BlockingQueue<Runnable> newBlockingQueue(
            WorkQueueDescriptor workQueueDescriptor) {
        if (workQueueDescriptor.usePriority) {
            log.warn("Priority queues are now deprecated and function as regular queues");
        }
        int capacity = workQueueDescriptor.capacity;
        if (capacity <= 0) {
            capacity = -1; // unbounded
        }
        return new NuxeoBlockingQueue<Runnable>(capacity);
    }

    protected Map<String, Work> newRunningMap() {
        return new HashMap<String, Work>();
    }

    protected Map<String, Work> newCompletedMap() {
        return new LinkedHashMap<String, Work>();
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
        getCompleted(queueId).put(work.getId(), work);
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
        Map<String, Work> completed = allCompleted.get(queueId);
        return completed == null ? 0 : completed.size();
    }

    // TODO fix this, q.containsWorkId is not actually properly thread-safe
    protected synchronized boolean isScheduled(String workId) {
        for (BlockingQueue<Runnable> scheduled : allScheduled.values()) {
            NuxeoBlockingQueue<Runnable> q = (NuxeoBlockingQueue<Runnable>) scheduled;
            if (q.containsWorkId(workId)) {
                return true;
            }
        }
        return false;
    }

    protected synchronized boolean isRunning(String workId) {
        for (Map<String, Work> running : allRunning.values()) {
            if (running.containsKey(workId)) {
                return true;
            }
        }
        return false;
    }

    protected synchronized boolean isCompleted(String workId) {
        for (Map<String, Work> completed : allCompleted.values()) {
            if (completed.containsKey(workId)) {
                return true;
            }
        }
        return false;
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
        for (Map<String, Work> completed : allCompleted.values()) {
            Work w = completed.get(workId);
            if (w != null) {
                return w;
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
        return new ArrayList<Work>(getCompleted(queueId).values());
    }

    // no synchronized as scheduled queue is thread-safe
    protected List<String> listScheduledIds(String queueId) {
        BlockingQueue<Runnable> scheduled = getScheduledQueue(queueId);
        List<String> list = new ArrayList<String>(scheduled.size());
        for (Runnable r : scheduled) {
            Work w = WorkHolder.getWork(r);
            list.add(w.getId());
        }
        return list;
    }

    // called synchronized
    protected List<String> listRunningIds(String queueId) {
        return new ArrayList<String>(getRunning(queueId).keySet());
    }

    // called synchronized
    protected List<String> listNonCompletedIds(String queueId) {
        List<String> list = listScheduledIds(queueId);
        list.addAll(listRunningIds(queueId));
        return list;
    }

    // called synchronized
    protected List<String> listCompletedIds(String queueId) {
        return new ArrayList<String>(getCompleted(queueId).keySet());
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
