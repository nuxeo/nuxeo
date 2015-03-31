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

import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.core.ElasticSearchAdminImpl;
import org.nuxeo.elasticsearch.core.IndexingMonitor;
import org.nuxeo.runtime.api.Framework;

import javax.validation.constraints.NotNull;

/**
 * Abstract class for sharing the worker state
 */
public abstract class BaseIndexingWorker extends AbstractWork {

    private static final long serialVersionUID = 1L;
    protected transient IndexingMonitor monitor;

    BaseIndexingWorker(IndexingMonitor monitor) {
        monitor.incrementWorker();
        this.monitor = monitor;
    }

    @Override
    public String getCategory() {
        return "elasticSearchIndexing";
    }

    @Override
    public void work() {
        getMonitor().incrementRunningWorker();
        try {
            doWork();
        } finally {
            getMonitor().decrementWorker();
        }
    }

    protected abstract void doWork();

    public @NotNull IndexingMonitor getMonitor() {
        if (monitor == null) {
            ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
            monitor = esa.getIndexingMonitor();
        }
        return monitor;
    }
}
