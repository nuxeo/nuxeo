/*
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

import static java.lang.Math.max;

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
