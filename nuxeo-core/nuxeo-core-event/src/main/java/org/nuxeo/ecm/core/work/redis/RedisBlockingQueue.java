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
package org.nuxeo.ecm.core.work.redis;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.api.Work;

/**
 * Redis-based {@link BlockingQueue}.
 * <p>
 * It has unlimited capacity, so never blocks on {@link #put} and {@link #offer}
 * always returns {@code true}.
 *
 * @since 5.8
 */
public class RedisBlockingQueue extends AbstractQueue<Runnable>
        implements BlockingQueue<Runnable> {

    private static final Log log = LogFactory.getLog(RedisBlockingQueue.class);

    protected final String queueId;

    protected final RedisWorkQueuing queuing;

    public RedisBlockingQueue(String queueId, RedisWorkQueuing queuing) {
        this.queueId = queueId;
        this.queuing = queuing;
    }

    @Override
    public boolean offer(Runnable r) {
        Work work = WorkHolder.getWork(r);
        try {
            queuing.addScheduledWork(queueId, work);
            return true;
        } catch (IOException e) {
            log.error("Failed to add Work: " + work, e);
            return false;
        }
    }

    @Override
    public boolean offer(Runnable r, long timeout, TimeUnit unit)
            throws InterruptedException {
        // not needed for ThreadPoolExecutor
        return offer(r);
    }

    @Override
    public void put(Runnable r) throws InterruptedException {
        offer(r);
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
        Work work = queuing.removeScheduled(queueId);
        return work == null ? null : new WorkHolder(work);
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        long expiry = System.currentTimeMillis() + unit.toMillis(timeout);
        for (;;) {
            Runnable r = poll();
            if (r != null) {
                return r;
            }
            if (System.currentTimeMillis() >= expiry) {
                return null;
            }
            // TODO replace by wakeup from Redis when something is available
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
        return queuing.getScheduledSize(queueId);
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Iterator<Runnable> iterator() {
        return new Iter();
    }

    /**
     * Used by drainQueue/purge methods of ThreadPoolExector.
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

}
