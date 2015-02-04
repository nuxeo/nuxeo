package org.nuxeo.elasticsearch.core;/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor indexing state
 *
 * @since 7.2
 */
public class IndexingMonitor {

    private AtomicInteger totalWorkerCounter = new AtomicInteger(0);

    private AtomicInteger runningCounter = new AtomicInteger(0);

    private static final ReentrantLock lock = new ReentrantLock();

    public static final Condition indexingDone = lock.newCondition();

    public int getTotalWorkerCount() {
        return totalWorkerCounter.get();
    }

    public int getPendingWorkerCount() {
        return totalWorkerCounter.get() - runningCounter.get();
    }

    public int getRunningWorkerCount() {
        return runningCounter.get();
    }

    public void incrementWorker() {
        totalWorkerCounter.incrementAndGet();
    }

    public void incrementRunningWorker() {
        runningCounter.incrementAndGet();
    }

    /**
     * Decrement worker and running worker counts
     */
    public void decrementWorker() {
        runningCounter.decrementAndGet();
        if (totalWorkerCounter.decrementAndGet() == 0) {
            lock.lock();
            try {
                indexingDone.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Return when there are no active worker.
     */
    public void waitForWorkerToComplete() {
        if (totalWorkerCounter.get() == 0) {
            return;
        }
        lock.lock();
        try {
            indexingDone.await();
        } catch (InterruptedException e) {
            //
        } finally {
            lock.unlock();
        }
    }
}
