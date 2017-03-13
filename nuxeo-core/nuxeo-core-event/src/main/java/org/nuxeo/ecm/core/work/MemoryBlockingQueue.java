/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;

/**
 * Memory-based {@link BlockingQueue}.
 * <p>
 * In addition, this implementation also keeps a set of {@link Work} ids in the queue when the queue elements are
 * {@link WorkHolder}s.
 */
public class MemoryBlockingQueue extends NuxeoBlockingQueue {

    /**
     * A {@link LinkedBlockingQueue} that blocks on {@link #offer} and prevents starvation deadlocks on reentrant calls.
     */
    private static class ReentrantLinkedBlockingQueue<T> extends LinkedBlockingQueue<T> {

        private static final long serialVersionUID = 1L;

        private final ReentrantLock limitedPutLock = new ReentrantLock();

        private final int limitedCapacity;

        /**
         * Creates a {@link LinkedBlockingQueue} with a maximum capacity.
         * <p>
         * If the capacity is -1 then this is treated as a regular unbounded {@link LinkedBlockingQueue}.
         *
         * @param capacity the capacity, or -1 for unbounded
         */
        public ReentrantLinkedBlockingQueue(int capacity) {
            // Allocate more space to prevent starvation dead lock
            // because a worker can add a new job to the queue.
            super(capacity < 0 ? Integer.MAX_VALUE : (2 * capacity));
            limitedCapacity = capacity;
        }

        /**
         * Block until there are enough remaining capacity to put the entry.
         */
        public void limitedPut(T e) throws InterruptedException {
            limitedPutLock.lockInterruptibly();
            try {
                while (remainingCapacity() < limitedCapacity) {
                    // TODO replace by wakeup when an element is removed
                    Thread.sleep(100);
                }
                put(e);
            } finally {
                limitedPutLock.unlock();
            }
        }

        @Override
        public boolean offer(T e) {
            if (limitedCapacity < 0) {
                return super.offer(e);
            }
            // turn non-blocking offer into a blocking put
            try {
                if (Thread.currentThread()
                        .getName()
                        .startsWith(WorkManagerImpl.THREAD_PREFIX)) {
                    // use the full queue capacity for reentrant call
                    put(e);
                } else {
                    // put only if there are enough remaining capacity
                    limitedPut(e);
                }
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread()
                        .interrupt();
                throw new RuntimeException("interrupted", ie);
            }
        }
    }

    protected final BlockingQueue<Runnable> queue;

    protected final Map<String, Work> works = new HashMap<>();

    protected final Set<String> scheduledWorks = new HashSet<>();

    protected final Set<String> runningWorks = new HashSet<>();

    long scheduledCount;
    long runningCount;
    long completedCount;
    long cancelledCount;

    /**
     * Creates a {@link BlockingQueue} with a maximum capacity.
     * <p>
     * If the capacity is -1 then this is treated as a regular unbounded {@link LinkedBlockingQueue}.
     *
     * @param capacity the capacity, or -1 for unbounded
     */
    public MemoryBlockingQueue(String id, MemoryWorkQueuing queuing, int capacity) {
        super(id, queuing);
        queue = new ReentrantLinkedBlockingQueue<>(capacity);
    }

    @Override
    synchronized protected WorkQueueMetrics metrics() {
        return new WorkQueueMetrics(queueId, scheduledCount, runningCount, completedCount, cancelledCount);
    }

    @Override
    public int getQueueSize() {
        return queue.size();
    }

    @Override
    public void putElement(Runnable r) throws InterruptedException {
        queue.put(r);
    }

    @Override
    public Runnable pollElement() {
        Runnable r = queue.poll();
        return r;
    }

    @Override
    public Runnable take() throws InterruptedException {
        Runnable r = queue.take();
        if (anotherWorkIsAlreadyRunning(r)) {
            // reschedule the work so it does not run concurrently
            offer(r);
            // take a break we don't want to take too much CPU looping on the same message.
            Thread.sleep(100);
            return null;
        }
        return r;
    }

    private boolean anotherWorkIsAlreadyRunning(Runnable r) throws InterruptedException {
        Work work = WorkHolder.getWork(r);
        String id = work.getId();
        if (runningWorks.contains(id)) {
            return true;
        }
        return false;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        nanos = awaitActivation(nanos);
        if (nanos <= 0) {
            return null;
        }
        return queue.poll(nanos, TimeUnit.NANOSECONDS);
    }

    synchronized WorkQueueMetrics workSchedule(Work work) {
        String id = work.getId();
        if (scheduledWorks.contains(id)) {
            return metrics();
        }
        if (!offer(new WorkHolder(work))) {
            return metrics();
        }
        works.put(id, work);
        scheduledWorks.add(id);
        scheduledCount += 1;
        return metrics();
    }

    synchronized WorkQueueMetrics workRunning(Work work) {
        String id = work.getId();
        scheduledWorks.remove(id);
        works.put(id, work); // update state
        runningWorks.add(id);
        scheduledCount -= 1;
        runningCount += 1;
        return metrics();
    }

    synchronized WorkQueueMetrics workCanceled(Work work) {
        String id = work.getId();
        for (Iterator<Runnable> it = queue.iterator(); it.hasNext();) {
            if (id.equals(WorkHolder.getWork(it.next())
                    .getId())) {
                it.remove();
                scheduledWorks.remove(id);
                works.remove(id);
                scheduledCount -= 1;
                cancelledCount +=1 ;
                break;
            }
        }
        return metrics();
    }

    synchronized WorkQueueMetrics workCompleted(Work work) {
        String id = work.getId();
        if (runningWorks.remove(id) && !scheduledWorks.contains(id)) {
            works.remove(id);
        }
        runningCount -= 1;
        completedCount += 1;
        return metrics();
    }

    synchronized WorkQueueMetrics workRescheduleRunning(Work work) {
        String id = work.getId();
        if (!runningWorks.remove(id)) {
            return metrics();
        }
        works.remove(id);
        runningCount -= 1;
        return workSchedule(work);
    }

    synchronized Work lookup(String workId) {
        return works.get(workId);
    }

    synchronized List<Work> list() {
        return new ArrayList<>(works.values());
    }

    synchronized List<String> keys() {
        return new ArrayList<>(works.keySet());
    }

    synchronized List<Work> listScheduled() {
        return scheduledWorks.stream()
                .map(works::get)
                .collect(Collectors.toList());
    }

    synchronized List<String> scheduledKeys() {
        return new ArrayList<>(scheduledWorks);
    }

    synchronized List<Work> listRunning() {
        return runningWorks.stream()
                .map(works::get)
                .collect(Collectors.toList());
    }

    synchronized List<String> runningKeys() {
        return new ArrayList<>(runningWorks);
    }

}
