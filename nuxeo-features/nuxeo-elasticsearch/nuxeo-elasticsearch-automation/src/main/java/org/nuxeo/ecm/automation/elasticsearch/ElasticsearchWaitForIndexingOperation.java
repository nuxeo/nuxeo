/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.automation.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Long.max;

/**
 * Wait for Elasticsearch indexing background job
 *
 * @since 8.1
 */
@Operation(id = ElasticsearchWaitForIndexingOperation.ID, category = Constants.CAT_SERVICES, label = "Wait for Elasticsearch Indexing",
        since = "8.1",
        description = "Wait until Elasticsearch indexing is done.")
public class ElasticsearchWaitForIndexingOperation {

    public static final String ID = "Elasticsearch.WaitForIndexing";

    private static final Log log = LogFactory.getLog(Log.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected ElasticSearchAdmin esa;

    @Context
    protected CoreSession repo;

    @Param(name = "timeoutSecond", required = false)
    protected Integer timeout = 60;

    @Param(name = "refresh", required = false)
    protected Boolean refresh = false;

    @OperationMethod
    public Boolean run() {
        long start = System.currentTimeMillis();
        WorkManager workManager = Framework.getService(WorkManager.class);
        try {
            if (!workManager.awaitCompletion(timeout, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
            esa.prepareWaitForIndexing().get(computeRemainingTime(start), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return Boolean.FALSE;
        } catch (InterruptedException e) {
            return Boolean.FALSE;
        } catch (ExecutionException e) {
            return Boolean.FALSE;
        }
        if (refresh) {
            esa.refreshRepositoryIndex(repo.getRepositoryName());
        }
        return Boolean.TRUE;
    }

    protected long computeRemainingTime(long start) {
        long elapsed = System.currentTimeMillis() - start;
        // at least one second
        return max(timeout - TimeUnit.MILLISECONDS.toSeconds(elapsed), 1);
    }

}
