/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.nuxeo.ecm.core.work.api.Work;

/**
 * Memory-based {@link BlockingQueue}.
 * <p>
 * In addition, this implementation also keeps a set of {@link Work} ids in the
 * queue when the queue elements are {@link WorkHolder}s.
 */
public class MemoryBlockingQueue extends NuxeoBlockingQueue {

    /**
     * A {@link LinkedBlockingQueue} that blocks on {@link #offer} and prevents
     * starvation deadlocks on reentrant calls.
     */
    private static class ReentrantLinkedBlockingQueue<T> extends
            LinkedBlockingQueue<T> {

        private static final long serialVersionUID = 1L;

        private final ReentrantLock limitedPutLock = new ReentrantLock();

        private final int limitedCapacity;

        /**
         * Creates a {@link LinkedBlockingQueue} with a maximum capacity.
         * <p>
         * If the capacity is -1 then this is treated as a regular unbounded
         * {@link LinkedBlockingQueue}.
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
                if (Thread.currentThread().getName().startsWith(
                        WorkManagerImpl.THREAD_PREFIX)) {
                    // use the full queue capacity for reentrant call
                    put(e);
                } else {
                    // put only if there are enough remaining capacity
                    limitedPut(e);
                }
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted", ie);
            }
        }
    }

    protected final MemoryWorkQueuing queuing;

    protected final BlockingQueue<Runnable> queue;

    // @GuardedBy("itself")
    protected final Set<String> workIds;

    /**
     * Creates a {@link BlockingQueue} with a maximum capacity.
     * <p>
     * If the capacity is -1 then this is treated as a regular unbounded
     * {@link LinkedBlockingQueue}.
     *
     * @param capacity the capacity, or -1 for unbounded
     */
    public MemoryBlockingQueue(MemoryWorkQueuing queuing, int capacity) {
        this.queuing = queuing;
        queue = new ReentrantLinkedBlockingQueue<Runnable>(capacity);
        workIds = new HashSet<String>();
    }

    /**
     * Checks if the queue contains a given work id.
     *
     * @param workId the work id
     * @return {@code true} if the queue contains the work id
     */
    public boolean containsWorkId(String workId) {
        synchronized (workIds) {
            return workIds.contains(workId);
        }
    }

    private Runnable addWorkId(Runnable r) {
        if (r instanceof WorkHolder) {
            WorkHolder wh = (WorkHolder) r;
            String id = WorkHolder.getWork(wh).getId();
            synchronized (workIds) {
                workIds.add(id);
            }
        }
        return r;
    }

    private Runnable removeWorkId(Runnable r) {
        if (r instanceof WorkHolder) {
            WorkHolder wh = (WorkHolder) r;
            String id = WorkHolder.getWork(wh).getId();
            synchronized (workIds) {
                workIds.remove(id);
            }
        }
        return r;
    }

    @Override
    public int getQueueSize() {
        return queue.size();
    }

    @Override
    public void putElement(Runnable r) throws InterruptedException {
        queue.put(r);
        addWorkId(r);
    }

    @Override
    public Runnable pollElement() {
        Runnable r = queue.poll();
        removeWorkId(r);
        return r;
    }

    @Override
    public Runnable take() throws InterruptedException {
        Runnable r = queue.take();
        removeWorkId(r);
        return r;
    }

    /*
     * We can implement iterator, super doesn't have it.
     */
    @Override
    public Iterator<Runnable> iterator() {
        return new Itr(queue.iterator());
    }

    private class Itr implements Iterator<Runnable> {
        private Iterator<Runnable> it;

        private Runnable last;

        public Itr(Iterator<Runnable> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Runnable next() {
            return last = it.next();
        }

        @Override
        public void remove() {
            it.remove();
            removeWorkId(last);
        }
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        nanos = awaitActivation(nanos);
        if (nanos <= 0) {
            return null;
        }
        return queue.poll(nanos, TimeUnit.NANOSECONDS);
    }

}
