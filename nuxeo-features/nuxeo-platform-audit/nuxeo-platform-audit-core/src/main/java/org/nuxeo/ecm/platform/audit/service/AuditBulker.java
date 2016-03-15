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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBulkerDescriptor;
import org.nuxeo.ecm.platform.audit.service.management.AuditBulkerMBean;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class AuditBulker implements AuditBulkerMBean {

    final Log log = LogFactory.getLog(AuditBulker.class);

    final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    final Gauge<Integer> sizeGauge =  new Gauge<Integer>() {

        @Override
        public Integer getValue() {
            return queue.size();
        }

    };

    final NXAuditEventsService component;

    final Counter queuedCount = registry.counter(MetricRegistry.name("nuxeo", "audit", "queued"));

    final Counter drainedCount = registry.counter(MetricRegistry.name("nuxeo", "audit", "drained"));

    Thread thread;

    AuditBulker(NXAuditEventsService component) {
        this.component = component;
    }

    void startup() {
        thread = new Thread(new Consumer(), "Nuxeo-Audit-Bulker");
        registry.register(MetricRegistry.name("nuxeo", "audit", "size"), sizeGauge);
        thread.start();
    }

    void shutdown() {
        stopped = true;
        registry.remove(MetricRegistry.name("nuxeo", "audit", "size"));
        try {
            thread.interrupt();
        } finally {
            thread = null;
        }
    }

    final ReentrantLock lock = new ReentrantLock();

    final Condition isEmpty = lock.newCondition();

    final Condition isFilled = lock.newCondition();

    final Queue<LogEntry> queue = new ConcurrentLinkedQueue<>();

    volatile boolean stopped;

    void offer(LogEntry entry) {
        if (log.isDebugEnabled()) {
            log.debug("offered " + entry);
        }
        queue.add(entry);
        queuedCount.inc();
        if (queue.size() >= component.bulkerConfig.size) {
            lock.lock();
            try {
                isFilled.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    boolean await(long time, TimeUnit unit) throws InterruptedException {
        if (queue.isEmpty()) {
            return true;
        }
        lock.lock();
        try {
            isFilled.signalAll();
            return isEmpty.await(time, unit);
        } finally {
            lock.unlock();
        }
    }

    int drain() {
        List<LogEntry> entries = new LinkedList<>();
        while (!queue.isEmpty()) {
            entries.add(queue.remove());
        }
        component.backend.addLogEntries(entries);
        drainedCount.inc(entries.size());
        if (queue.isEmpty()) {
            lock.lock();
            try {
                isEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        }
        return entries.size();
    }


    class Consumer implements Runnable {

        @Override
        public void run() {
            log.info("bulk audit logger started");
            while(!stopped) {
                lock.lock();
                try {
                    isFilled.await(component.bulkerConfig.timeout, TimeUnit.SECONDS);
                    if (queue.isEmpty()) {
                        continue;
                    }
                } catch (InterruptedException cause) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }
                try {
                    int count = drain();
                    if (log.isDebugEnabled()) {
                        log.debug("flushed " + count + " events");
                    }
                } catch (RuntimeException cause) {
                    log.error("caught error while draining audit queue", cause);
                }
            }
            log.info("bulk audit logger stopped");
        }

    }


    @Override
    public AuditBulkerDescriptor getConfig() {
        return component.bulkerConfig;
    }

    @Override
    public void setBulkTimeout(int value) {
        component.bulkerConfig.timeout = value;
    }

    @Override
    public void setBulkSize(int value) {
        component.bulkerConfig.size = value;
    }

    @Override
    public void resetMetrics() {
        queuedCount.dec(queuedCount.getCount());
        drainedCount.dec(drainedCount.getCount());
    }
}
