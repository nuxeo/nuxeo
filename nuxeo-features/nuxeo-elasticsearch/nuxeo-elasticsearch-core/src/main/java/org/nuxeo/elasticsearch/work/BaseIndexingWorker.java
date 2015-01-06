/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.work;

import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * Abstract class for sharing the worker state
 */
public abstract class BaseIndexingWorker extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final AtomicInteger pendingWorkerCount = new AtomicInteger(0);

    private static final AtomicInteger runningWorkerCount = new AtomicInteger(0);


    public static int getPendingWorkerCount() {
        return pendingWorkerCount.get();
    }

    public static int getRunningWorkerCount() {
        return runningWorkerCount.get();
    }

    public BaseIndexingWorker() {
        pendingWorkerCount.incrementAndGet();
    }

    @Override
    public String getCategory() {
        return "elasticSearchIndexing";
    }

    @Override
    public void work() {
        runningWorkerCount.incrementAndGet();
        pendingWorkerCount.decrementAndGet();
        try {
            doWork();
        } finally {
            runningWorkerCount.decrementAndGet();
        }
    }

    protected abstract void doWork();
}
