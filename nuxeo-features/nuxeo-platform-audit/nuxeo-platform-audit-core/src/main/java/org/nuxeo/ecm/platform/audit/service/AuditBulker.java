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
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class AuditBulker {

    final Log log = LogFactory.getLog(AuditBulker.class);

    final AbstractAuditBackend backend;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter queuedCount = registry.counter(MetricRegistry.name("nuxeo", "audit", "queued"));

    protected final Counter drainedCount = registry.counter(MetricRegistry.name("nuxeo", "audit", "drained"));


    Thread thread;

    AuditBulker(AbstractAuditBackend backend) {
        this.backend = backend;
    }

    void startup() {
        thread = new Thread(new Flusher(), "Nuxeo-Audit-Bulk");
        thread.start();
    }

    void shutdown() {
        stopped = true;
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

    boolean stopped;

    void offer(LogEntry entry) {
        if (log.isDebugEnabled()) {
            log.debug("offered " + entry);
        }
        queue.add(entry);
        queuedCount.inc();
        if (queue.size() >= 1000) {
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
        backend.addLogEntries(entries);
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


    class Flusher implements Runnable {

        @Override
        public void run() {
            log.info("bulk audit logger started");
            while(!stopped) {
                log.debug("waiting for events");

                lock.lock();
                try {
                    isFilled.await(10, TimeUnit.SECONDS);
                    if (queue.isEmpty()) {
                        continue;
                    }
                } catch (InterruptedException cause) {
                    Thread.currentThread().interrupt();
                    LogFactory.getLog(AuditBulker.class).warn("bulk loggger interrupted", cause);
                    return;
                } finally {
                    lock.unlock();
                }
                int count = drain();
                log.debug("flushed " + count + " events");
            }
        }

    }
}
