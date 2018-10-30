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
 *     Thierry Delprat
 */
package org.nuxeo.elasticsearch.test.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;
import static org.nuxeo.elasticsearch.bulk.IndexAction.REFRESH_INDEX_PARAM;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.bulk.IndexAction;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, CoreBulkFeature.class})
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBulkIndex {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected BulkService bulkService;

    @Before
    public void initWorkingDocuments() throws Exception {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        for (int i = 0; i < 20; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // wait for async jobs
        Framework.getService(WorkManager.class).awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    @Test
    public void testIndexAction() throws InterruptedException {
        esa.initIndexes(true);
        String commandId = bulkService.submit(
                new BulkCommand.Builder(IndexAction.ACTION_NAME,
                        "SELECT * FROM Document").param(REFRESH_INDEX_PARAM, true)
                                                 .param(INDEX_UPDATE_ALIAS_PARAM, true)
                                                 .batch(2)
                                                 .bucket(2)
                                                 .build());
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(BulkStatus.State.COMPLETED, status.getState());
    }
}
