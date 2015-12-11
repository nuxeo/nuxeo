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
package org.nuxeo.ecm.core.redis.contribs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.NuxeoBlockingQueue;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.api.Work;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Redis-based {@link BlockingQueue}.
 * <p>
 * It has unlimited capacity, so never blocks on {@link #put} and {@link #offer} always returns {@code true}.
 *
 * @since 5.8
 */
public class RedisBlockingQueue extends NuxeoBlockingQueue {

    private static final Log log = LogFactory.getLog(RedisBlockingQueue.class);

    // this is so that we don't spam the logs with too many errors
    private static final long LOG_INTERVAL = 1000 * 10; // 10s

    private static AtomicLong LAST_IO_EXCEPTION = new AtomicLong(0);

    private static AtomicLong LAST_CONNECTION_EXCEPTION = new AtomicLong(0);

    protected final String queueId;

    protected final RedisWorkQueuing queuing;

    protected final Lock lock = new ReentrantLock();
    protected final Condition notEmpty = lock.newCondition();

    public RedisBlockingQueue(String queueId, RedisWorkQueuing queuing) {
        this.queueId = queueId;
        this.queuing = queuing;
    }

    @Override
    public int getQueueSize() {
        return queuing.getScheduledSize(queueId);
    }

    @Override
    public Runnable take() throws InterruptedException {
        for (; ; ) {
            Runnable r = poll(1, TimeUnit.DAYS);
            if (r != null) {
                return r;
            }
        }
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        nanos = awaitActivation(nanos);
        if (nanos <= 0) {
            return null;
        }
        long end = System.currentTimeMillis() + TimeUnit.NANOSECONDS.toMillis(nanos);
        for (; ; ) {
            Runnable r = poll();
            if (r != null) {
                return r;
            }
            if (timeUntil(end) == 0) {
                return null;
            }
            lock.lock();
            try {
                notEmpty.await(1, TimeUnit.SECONDS);
            } finally {
                lock.unlock();
            }

        }
    }

    @Override
    public void putElement(Runnable r) {
        Work work = WorkHolder.getWork(r);
        lock.lock();
        try {
            queuing.addScheduledWork(queueId, work);
            notEmpty.signal();
        } catch (IOException e) {
            log.error("Failed to add Work: " + work, e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Runnable pollElement() {
        try {
            Work work = queuing.getWorkFromQueue(queueId);
            if (work != null) {
                log.debug("Remove scheduled " + work);
            }
            return work == null ? null : new WorkHolder(work);
        } catch (IOException e) {
            if (delayExpired(LAST_IO_EXCEPTION)) {
                // log full stacktrace
                log.error(e.getMessage(), e);
            }
            // for io errors make poll return no result
            return null;
        } catch (JedisConnectionException e) {
            if (delayExpired(LAST_CONNECTION_EXCEPTION)) {
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage().contains(ConnectException.class.getName())) {
                    log.error(e.getMessage() + ": " + cause.getMessage());
                    log.debug(e.getMessage(), e);
                } else {
                    // log full stacktrace
                    log.error(e.getMessage(), e);
                }
            }
            // for connection errors make poll return no result
            return null;
        }
    }

    protected static boolean delayExpired(AtomicLong atomic) {
        long now = System.currentTimeMillis();
        long last = atomic.get();
        if (now > last + LOG_INTERVAL) {
            if (atomic.compareAndSet(last, now)) {
                return true;
            } // else some other thread beat us to it
        }
        return false;
    }

}
