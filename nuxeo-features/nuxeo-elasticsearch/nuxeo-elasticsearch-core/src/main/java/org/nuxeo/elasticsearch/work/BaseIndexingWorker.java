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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class for sharing the worker state
 *
 */
public abstract class BaseIndexingWorker extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final AtomicInteger activeWorker = new AtomicInteger(0);

    public static int getRunningWorkers() {
        return activeWorker.get();
    }

    public BaseIndexingWorker() {
        activeWorker.incrementAndGet();
    }

    @Override
    public String getCategory() {
        return "elasticSearchIndexing";
    }

    @Override
    public void work() {
        try {
            doWork();
        } finally {
            activeWorker.decrementAndGet();
        }
    }

    protected abstract void doWork();
}
