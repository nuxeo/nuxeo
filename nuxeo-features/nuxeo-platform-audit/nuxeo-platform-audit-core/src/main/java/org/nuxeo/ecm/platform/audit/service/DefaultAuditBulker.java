/*
 * (C) Copyright 2010, 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBulkerDescriptor;
import org.nuxeo.ecm.platform.audit.service.management.AuditBulkerMBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class DefaultAuditBulker implements AuditBulkerMBean, AuditBulker {

    final Log log = LogFactory.getLog(DefaultAuditBulker.class);

    final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    final Gauge<Integer> sizeGauge = new Gauge<Integer>() {

        @Override
        public Integer getValue() {
            return queue.size();
        }

    };

    final AuditBackend backend;

    final Counter queuedCount = registry.counter(MetricRegistry.name("nuxeo", "audit", "queued"));

    final Counter drainedCount = registry.counter(MetricRegistry.name("nuxeo", "audit", "drained"));

    int timeout;

    int bulksize;

    Thread thread;

    DefaultAuditBulker(AuditBackend backend, AuditBulkerDescriptor config) {
        this.backend = backend;
        timeout = config.timeout;
        bulksize = config.size;
    }

    @Override
    public void onApplicationStarted() {
        thread = new Thread(new Consumer(), "Nuxeo-Audit-Bulker");
        thread.start();
        ResourcePublisher publisher = Framework.getService(ResourcePublisher.class);
        if (publisher != null) {
            publisher.registerResource("audit-bulker", "audit-bulker", AuditBulkerMBean.class, this);
        }
        registry.register(MetricRegistry.name("nuxeo", "audit", "size"), sizeGauge);
    }

    @Override
    public void onApplicationStopped() {
        registry.remove(MetricRegistry.name("nuxeo", "audit", "size"));
        ResourcePublisher publisher = Framework.getService(ResourcePublisher.class);
        if (publisher != null) {
            publisher.unregisterResource("audit-bulker", "audit-bulker");
        }
        stopped = true;
        try {
            thread.interrupt();
        } finally {
            thread = null;
        }
    }

    final AtomicInteger size = new AtomicInteger(0);

    final ReentrantLock lock = new ReentrantLock();

    final Condition isEmpty = lock.newCondition();

    final Condition isFilled = lock.newCondition();

    final Queue<LogEntry> queue = new ConcurrentLinkedQueue<>();

    volatile boolean stopped;

    @Override
    public void offer(LogEntry entry) {
        if (log.isDebugEnabled()) {
            log.debug("offered " + entry);
        }
        queue.add(entry);
        queuedCount.inc();

        if (size.incrementAndGet() >= bulksize) {
            lock.lock();
            try {
                isFilled.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            isFilled.signalAll();
            long nanos = unit.toNanos(time);
            while (!queue.isEmpty()) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = isEmpty.awaitNanos(nanos);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    int drain() {
        List<LogEntry> entries = new LinkedList<>();
        while (!queue.isEmpty()) {
            entries.add(queue.remove());
        }
        backend.addLogEntries(entries);
        int delta = entries.size();
        size.addAndGet(-delta);
        drainedCount.inc(delta);
        return delta;
    }

    class Consumer implements Runnable {

        @Override
        public void run() {
            log.info("bulk audit logger started");
            while (!stopped) {
                lock.lock();
                try {
                    isFilled.await(timeout, TimeUnit.MILLISECONDS); // NOSONAR (spurious wakeups don't matter)
                    if (queue.isEmpty()) {
                        continue;
                    }
                    int count = drain();
                    if (log.isDebugEnabled()) {
                        log.debug("flushed " + count + " events");
                    }
                } catch (InterruptedException cause) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    try {
                        isEmpty.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            }
            log.info("bulk audit logger stopped");
        }

    }

    @Override
    public int getBulkTimeout() {
        return timeout;
    }

    @Override
    public void setBulkTimeout(int value) {
        timeout = value;
    }

    @Override
    public int getBulkSize() {
        return bulksize;
    }

    @Override
    public void setBulkSize(int value) {
        bulksize = value;
    }

    @Override
    public void resetMetrics() {
        queuedCount.dec(queuedCount.getCount());
        drainedCount.dec(drainedCount.getCount());
    }
}
