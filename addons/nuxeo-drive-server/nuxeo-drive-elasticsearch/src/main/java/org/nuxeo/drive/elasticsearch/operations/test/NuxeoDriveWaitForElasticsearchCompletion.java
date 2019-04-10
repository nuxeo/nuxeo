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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch.operations.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.elasticsearch.ElasticsearchException;
import org.nuxeo.drive.operations.test.NuxeoDriveWaitForAsyncCompletion;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * Waits for Elasticsearch audit completion.
 *
 * @since 7.3
 */
@Operation(id = NuxeoDriveWaitForElasticsearchCompletion.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Wait for Elasticsearch audit completion")
public class NuxeoDriveWaitForElasticsearchCompletion extends NuxeoDriveWaitForAsyncCompletion {

    public static final String ID = "NuxeoDrive.WaitForElasticsearchCompletion";

    @Override
    @OperationMethod
    public void run() throws InterruptedException, ExecutionException, TimeoutException {
        super.run();
        waitForElasticIndexing();
    }

    protected void waitForElasticIndexing() throws InterruptedException, ExecutionException, TimeoutException, ElasticsearchException {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        // Wait for indexing
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        // Explicit refresh
        esa.refresh();
        // Explicit refresh for the audit index until it is handled by esa.refresh
        esa.getClient().admin().indices().prepareRefresh(esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE)).get();
    }

}
