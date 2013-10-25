/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.nuxeo.ecm.core.work.api.Work;

/**
 * A {@link LinkedBlockingQueue} that can be configured with a maximum capacity.
 * <p>
 * When using a capacity, for a reentrant call the queue blocks on "offer" to
 * prevent starvation deadlocks.
 * <p>
 * In addition, this implementation also keeps a set of {@link Work} ids in the
 * queue when the queue elements are {@link WorkHolder}s.
 */
public class NuxeoBlockingQueue<T> extends LinkedBlockingQueue<T> {

    /*
     * ThreadPoolExecutor uses a BlockingQueue but the Java 7 implementation
     * only calls these methods on it:
     *
     * - isEmpty()
     *
     * - size()
     *
     * - poll(timeout, unit)
     *
     * - take()
     *
     * - offer(e)
     *
     * - remove(e)
     *
     * - toArray(), toArray(a)
     *
     * - drainTo(c)
     *
     * - iterator() : hasNext(), next(), remove()
     */

    private static final long serialVersionUID = 1L;

    private final ReentrantLock limitedPutLock = new ReentrantLock();

    private final int limitedCapacity;

    // @GuardedBy("itself")
    private final Set<String> workIds;

    /**
     * Creates a {@link BlockingQueue} with a maximum capacity.
     * <p>
     * If the capacity is -1 then this is treated as a regular unbounded
     * {@link LinkedBlockingQueue}.
     *
     * @param capacity the capacity, or -1 for unbounded
     */
    public NuxeoBlockingQueue(int capacity) {
        // Allocate more space to prevent starvation dead lock
        // because a worker can add a new job to the queue.
        super(capacity < 0 ? Integer.MAX_VALUE : (2 * capacity));
        limitedCapacity = capacity;
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

    private T addWorkId(T e) {
        if (e instanceof WorkHolder) {
            WorkHolder wh = (WorkHolder) e;
            String id = WorkHolder.getWork(wh).getId();
            synchronized (workIds) {
                workIds.add(id);
            }
        }
        return e;
    }

    private boolean addWorkId(T e, boolean added) {
        if (added) {
            addWorkId(e);
        }
        return added;
    }

    private T removeWorkId(T e) {
        if (e instanceof WorkHolder) {
            WorkHolder wh = (WorkHolder) e;
            String id = WorkHolder.getWork(wh).getId();
            synchronized (workIds) {
                workIds.remove(id);
            }
        }
        return e;
    }

    private boolean removeWorkId(T e, boolean removed) {
        if (removed) {
            removeWorkId(e);
        }
        return removed;
    }

    private void clearWorkIds() {
        synchronized (workIds) {
            workIds.clear();
        }
    }

    /**
     * Blocks until there is enough remaining capacity to put the entry.
     */
    protected void limitedPut(T e) throws InterruptedException {
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
        if (limitedCapacity < 0) {
            return addWorkId(e, super.offer(e));
        } else {
            // turn non blocking offer into a blocking put
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

    @Override
    public T remove() {
        return removeWorkId(super.remove());
    }

    @Override
    public T poll() {
        return removeWorkId(super.poll());
    }

    // element(): no need to override

    // peek(): no need to override

    @Override
    public Iterator<T> iterator() {
        return new Itr(super.iterator());
    }

    private class Itr implements Iterator<T> {
        private Iterator<T> it;

        private T last;

        public Itr(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return last = it.next();
        }

        @Override
        public void remove() {
            it.remove();
            removeWorkId(last);
        }
    }


    // addAll(): no need to override, calls offer()

    // removeAll(): no need to override, calls iterator()

    // retainAll(): no need to override, calls iterator()

    @Override
    public void clear() {
        super.clear();
        clearWorkIds();
    }

    @Override
    public boolean add(T e) {
        return addWorkId(e, super.add(e));
    }

    @Override
    public void put(T e) throws InterruptedException {
        super.put(e);
        addWorkId(e);
    }

    @Override
    public boolean offer(T e, long timeout, TimeUnit unit)
            throws InterruptedException {
        return addWorkId(e, super.offer(e, timeout, unit));
    }

    @Override
    public T take() throws InterruptedException {
        return removeWorkId(super.take());
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return removeWorkId(super.poll(timeout, unit));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        return removeWorkId((T) o, super.remove(o));
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        clearWorkIds(); // super implementation always drains all
        return super.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        List<T> tmp = new LinkedList<T>();
        int n = super.drainTo(tmp, maxElements);
        c.addAll(tmp);
        for (T e : tmp) {
            removeWorkId(e);
        }
        return n;
    }

}
