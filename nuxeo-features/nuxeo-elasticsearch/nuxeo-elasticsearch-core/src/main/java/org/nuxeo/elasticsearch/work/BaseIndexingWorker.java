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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.INDEXING_QUEUE_ID;

/**
 * Abstract class for sharing the worker state
 */
public abstract class BaseIndexingWorker extends AbstractWork {

    private static final long serialVersionUID = 1L;

    @Override
    public String getCategory() {
        return INDEXING_QUEUE_ID;
    }

    @Override
    public int getRetryCount() {
        // even read-only threads may encounter concurrent update exceptions
        // when trying to read a previously deleted complex property
        // due to read committed semantics, cf NXP-17384
        return 1;
    }

    @Override
    public void work() {
        doWork();
    }

    protected abstract void doWork();

}
