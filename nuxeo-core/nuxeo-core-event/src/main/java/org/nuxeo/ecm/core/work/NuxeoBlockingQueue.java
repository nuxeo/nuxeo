/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.work;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;

/**
 * An abstract {@link BlockingQueue} suitable for a fixed-sized {@link java.util.concurrent.ThreadPoolExecutor
 * ThreadPoolExecutor}, that can be implemented in terms of a few methods. {@link #offer} always succeeds.
 *
 * @since 5.8
 */
public abstract class NuxeoBlockingQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {

    /*
     * ThreadPoolExecutor uses a BlockingQueue but the Java 7 implementation only calls these methods on it:
     * - isEmpty()
     * - size()
     * - poll(timeout, unit): not used, as core pool size = max size and no core thread timeout
     * - take()
     * - offer(e)
     * - remove(e)
     * - toArray(), toArray(a): for purge and shutdown
     * - drainTo(c)
     * - iterator() : hasNext(), next(), remove() (called by toArray)
     */

    protected final ReentrantLock activationLock = new ReentrantLock();

    protected final Condition activation = activationLock.newCondition();

    protected volatile boolean active = false;

    protected final String queueId;

    protected final WorkQueuing queuing;

    protected NuxeoBlockingQueue(String queueId, WorkQueuing queuing) {
        this.queueId = queueId;
        this.queuing = queuing;
    }

    protected abstract WorkQueueMetrics metrics();

    /**
     * Sets the queue active or inactive. When deactivated, taking an element from the queue (take, poll, peek) behaves
     * as if the queue was empty. Elements can still be added when the queue is deactivated. When reactivated, all
     * elements are again available.
     *
     * @param active {@code true} to make the queue active, or {@code false} to deactivate it
     */
    public WorkQueueMetrics setActive(boolean active) {
        this.active = active;
        activationLock.lock();
        try {
            activation.signalAll();
        } finally {
            activationLock.unlock();
        }
        return metrics();
    }

    @Override
    public boolean offer(Runnable r) {
        try {
            put(r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean offer(Runnable r, long timeout, TimeUnit unit) throws InterruptedException {
        // not needed for ThreadPoolExecutor
        put(r);
        return true;
    }

    @Override
    public void put(Runnable r) throws InterruptedException {
        putElement(r);
    }

    @Override
    public Runnable peek() {
        // not needed for ThreadPoolExecutor
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Runnable poll() {
        if (!active) {
            return null;
        }
        Runnable runnable =  pollElement();
        if (runnable == null) {
            return null;
        }
        if (!active) {
            queuing.workReschedule(queueId, WorkHolder.getWork(runnable));
            return null;
        }
        return runnable;
    }

    protected long timeUntil(long end) {
        long timeout = end - System.currentTimeMillis();
        if (timeout < 0) {
            timeout = 0;
        }
        return timeout;
    }

    protected long awaitActivation(long nanos) throws InterruptedException {
        activationLock.lock();
        try {
            while (nanos > 0 && !active) {
                nanos = activation.awaitNanos(nanos);
            }

        } finally {
            activationLock.unlock();
        }
        return nanos;
    }

    @Override
    public boolean contains(Object o) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return getQueueSize();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Iterator<Runnable> iterator() {
        return new Iter();
    }

    /*
     * Used by drainQueue/purge methods of ThreadPoolExector through toArray.
     */
    private class Iter implements Iterator<Runnable> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Runnable next() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int drainTo(Collection<? super Runnable> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        for (int i = 0; i < maxElements; i++) {
            Runnable r = poll();
            if (r == null) {
                return i;
            }
            c.add(r);
        }
        return maxElements;
    }

    /**
     * Gets the size of the queue.
     */
    public abstract int getQueueSize();

    /**
     * Adds an element into this queue, waiting if necessary for space to become available.
     */
    public abstract void putElement(Runnable r) throws InterruptedException;

    /**
     * Retrieves and removes an element from the queue, or returns null if the queue is empty.
     */
    public abstract Runnable pollElement();

}
