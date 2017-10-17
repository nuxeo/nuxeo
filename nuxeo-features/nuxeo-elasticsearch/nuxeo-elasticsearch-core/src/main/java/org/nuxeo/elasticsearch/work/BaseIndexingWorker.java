/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.work;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.INDEXING_QUEUE_ID;

import org.nuxeo.ecm.core.work.AbstractWork;

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
