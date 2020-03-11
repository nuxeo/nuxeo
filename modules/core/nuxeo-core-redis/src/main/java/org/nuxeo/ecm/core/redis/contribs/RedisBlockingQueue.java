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
package org.nuxeo.ecm.core.redis.contribs;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.NuxeoBlockingQueue;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;

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
    private static final long LOG_INTERVAL = Duration.ofSeconds(10).toMillis();

    private static AtomicLong LAST_IO_EXCEPTION = new AtomicLong(0);

    private static AtomicLong LAST_CONNECTION_EXCEPTION = new AtomicLong(0);

    private static final int REMOTE_POLL_INTERVAL_MS = 1000;

    private static final int REMOTE_POLL_INTERVAL_STDEV_MS = 200;

    protected final RedisWorkQueuing queuing;

    protected final Lock lock = new ReentrantLock();
    protected final Condition notEmpty = lock.newCondition();

    public RedisBlockingQueue(String queueId, RedisWorkQueuing queuing) {
        super(queueId, queuing);
        this.queuing = queuing;
    }

    @Override
    protected WorkQueueMetrics metrics() {
        return queuing.metrics(queueId);
    }

    @Override
    public int getQueueSize() {
        return queuing.metrics(queueId).scheduled.intValue();
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
                // wake up if our instance has submitted a new job or wait
                notEmpty.await(getRemotePollInterval(), TimeUnit.MILLISECONDS);
            } finally {
                lock.unlock();
            }

        }
    }

    private int getRemotePollInterval() {
        // add some randomness so we don't generate periodic spike when all workers are starving
        return REMOTE_POLL_INTERVAL_MS + ThreadLocalRandom.current().nextInt(-1 * REMOTE_POLL_INTERVAL_STDEV_MS,
                REMOTE_POLL_INTERVAL_STDEV_MS);
    }

    @Override
    public void putElement(Runnable r) {
        Work work = WorkHolder.getWork(r);
        lock.lock();
        try {
            queuing.workSetScheduled(queueId, work);
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
