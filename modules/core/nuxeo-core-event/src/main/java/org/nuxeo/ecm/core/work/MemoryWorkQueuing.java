/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.work;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;

/**
 * Implementation of a {@link WorkQueuing} using in-memory queuing.
 *
 * @since 5.8
 */
public class MemoryWorkQueuing implements WorkQueuing {

    protected final Map<String, MemoryBlockingQueue> allQueued = new HashMap<>();

    protected Listener listener;

    public MemoryWorkQueuing(Listener listener) {
        this.listener = listener;
    }

    @Override
    public MemoryBlockingQueue init(WorkQueueDescriptor config) {
        int capacity = config.getCapacity();
        if (capacity <= 0) {
            capacity = -1; // unbounded
        }
        MemoryBlockingQueue queue = new MemoryBlockingQueue(config.id, this, capacity);
        allQueued.put(queue.queueId, queue);
        return queue;
    }

    @Override
    public MemoryBlockingQueue getQueue(String queueId) {
        return allQueued.get(queueId);
    }

    @Override
    public void workSchedule(String queueId, Work work) {
        listener.queueChanged(work, getQueue(queueId).workSchedule(work));
    }

    @Override
    public void workCanceled(String queueId, Work work) {
        listener.queueChanged(work, getQueue(queueId).workCanceled(work));
    }

    @Override
    public void workRunning(String queueId, Work work) {
        listener.queueChanged(work, getQueue(queueId).workRunning(work));
    }

    @Override
    public void workCompleted(String queueId, Work work) {
        listener.queueChanged(work, getQueue(queueId).workCompleted(work));
    }

    @Override
    public void workReschedule(String queueId, Work work) {
        listener.queueChanged(work, getQueue(queueId).workRescheduleRunning(work));
    }

    Optional<Work> lookup(String workId) {
        return allQueued.values().stream().map(queue -> queue.lookup(workId)).filter(Objects::nonNull).findAny();
    }

    @Override
    public Work find(String workId, State state) {
        return lookup(workId).filter(work -> workHasState(work, state)).orElse(null);
    }

    @Override
    public boolean isWorkInState(String workId, State state) {
        return lookup(workId).filter(work -> workHasState(work, state)).isPresent();
    }

    @Override
    public State getWorkState(String workId) {
        return lookup(workId).map(Work::getWorkInstanceState).orElse(null);
    }

    @Override
    public List<Work> listWork(String queueId, State state) {
        MemoryBlockingQueue queue = getQueue(queueId);
        if (state == null) {
            return queue.list();
        }
        switch (state) {
        case SCHEDULED:
            return queue.listScheduled();
        case RUNNING:
            return queue.listRunning();
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public List<String> listWorkIds(String queueId, State state) {
        MemoryBlockingQueue queue = getQueue(queueId);
        if (state == null) {
            return queue.keys();
        }
        switch (state) {
        case SCHEDULED:
            return queue.scheduledKeys();
        case RUNNING:
            return queue.runningKeys();
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public long count(String queueId, State state) {
        switch (state) {
        case SCHEDULED:
            return metrics(queueId).scheduled.longValue();
        case RUNNING:
            return metrics(queueId).running.longValue();
        default:
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public synchronized void removeScheduled(String queueId, String workId) {
        final MemoryBlockingQueue queue = getQueue(queueId);
        Work work = queue.lookup(workId);
        if (work == null) {
            return;
        }
        work.setWorkInstanceState(State.UNKNOWN);
        listener.queueChanged(work, queue.workCanceled(work));
    }

    @Override
    public void setActive(String queueId, boolean value) {
        WorkQueueMetrics metrics = getQueue(queueId).setActive(value);
        if (value) {
            listener.queueActivated(metrics);
        } else {
            listener.queueDeactivated(metrics);
        }
    }

    @Override
    public void listen(Listener listener) {
        this.listener = listener;
    }

    @Override
    public WorkQueueMetrics metrics(String queueId) {
        return getQueue(queueId).metrics();
    }

    /**
     * Returns {@code true} if the given state is not {@code null} and matches the state of the given work or if the
     * state is {@code null} and the work's state is either {@link State#SCHEDULED} or {@link State#RUNNING},
     * {@code false} otherwise.
     *
     * @since 9.3
     */
    protected static boolean workHasState(Work work, State state) {
        State workState = work.getWorkInstanceState();
        return state == null ? (workState == State.SCHEDULED || workState == State.RUNNING) : workState == state;
    }

    @Override
    public boolean supportsProcessingDisabling() {
        return false;
    }

}
