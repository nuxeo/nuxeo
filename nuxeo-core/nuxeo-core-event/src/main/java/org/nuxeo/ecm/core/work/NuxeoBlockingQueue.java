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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An abstract {@link BlockingQueue} suitable for a fixed-sized
 * {@link java.util.concurrent.ThreadPoolExecutor ThreadPoolExecutor}, that can
 * be implemented in terms of a few methods.
 *
 * {@link #offer} always succeeds.
 *
 * @since 5.8
 */
public abstract class NuxeoBlockingQueue extends AbstractQueue<Runnable>
        implements BlockingQueue<Runnable> {

    /*
     * ThreadPoolExecutor uses a BlockingQueue but the Java 7 implementation
     * only calls these methods on it:
     *
     * - isEmpty()
     *
     * - size()
     *
     * - poll(timeout, unit): not used, as core pool size = max size and no core
     * thread timeout
     *
     * - take()
     *
     * - offer(e)
     *
     * - remove(e)
     *
     * - toArray(), toArray(a): for purge and shutdown
     *
     * - drainTo(c)
     *
     * - iterator() : hasNext(), next(), remove() (called by toArray)
     */

    protected Object activeMonitor = new Object();

    protected volatile boolean active = true;

    /**
     * Sets the queue active or inactive.
     *
     * When deactivated, taking an element from the queue (take, poll, peek)
     * behaves as if the queue was empty. Elements can still be added when the
     * queue is deactivated. When reactivated, all elements are again available.
     *
     * @param active {@code true} to make the queue active, or {@code false} to
     *            deactivate it
     */
    public void setActive(boolean active) {
        this.active = active;
        synchronized (activeMonitor) {
            activeMonitor.notifyAll();
        }
    }

    @Override
    public boolean offer(Runnable r) {
        try {
            put(r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt status
            throw new RuntimeException("interrupted", e);
        }
        return true;
    }

    @Override
    public boolean offer(Runnable r, long timeout, TimeUnit unit)
            throws InterruptedException {
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
    public Runnable take() throws InterruptedException {
        for (;;) {
            Runnable r = poll(1, TimeUnit.DAYS);
            if (r != null) {
                return r;
            }
        }
    }

    @Override
    public Runnable poll() {
        if (!active) {
            return null;
        }
        return pollElement();
    }

    protected long timeUntil(long end) {
        long timeout = end - System.currentTimeMillis();
        if (timeout < 0) {
            timeout = 0;
        }
        return timeout;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        for (;;) {
            while (!active) {
                synchronized (activeMonitor) {
                    activeMonitor.wait(timeUntil(end));
                }
                if (timeUntil(end) == 0) {
                    return null;
                }
            }
            Runnable r = poll();
            if (r != null) {
                return r;
            }
            if (timeUntil(end) == 0) {
                return null;
            }
            // TODO replace by wakeup when an element is added
            Thread.sleep(100);
        }
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

        public Iter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        @Override
        public Runnable next() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            // TODO Auto-generated method stub
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
     * Adds an element into this queue, waiting if necessary for space to become
     * available.
     */
    public abstract void putElement(Runnable r) throws InterruptedException;

    /**
     * Retrieves and removes an element from the queue, or returns null if the
     * queue is empty.
     */
    public abstract Runnable pollElement();

}
