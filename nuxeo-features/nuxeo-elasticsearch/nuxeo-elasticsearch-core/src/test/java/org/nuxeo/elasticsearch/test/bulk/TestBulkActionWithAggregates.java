/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.elasticsearch.test.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.bulk.BulkRunAction;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.action.SetPropertiesAction;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, CoreBulkFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.elasticsearch.core:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:schemas-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBulkActionWithAggregates {

    @Inject
    protected CoreSession session;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected AutomationService automationService;

    protected ZonedDateTime now;

    protected ZonedDateTime dayBeforYesterday;

    protected ZonedDateTime lastWeek;

    protected OperationContext ctx;

    @Before
    public void before() throws Exception {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }

        now = ZonedDateTime.now();
        dayBeforYesterday = now.minusDays(2);
        lastWeek = now.minusWeeks(1);

        // Create some documents
        for (int i = 0; i < 20; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:nature", "nature" + i);
            doc.setPropertyValue("file:content", i % 2 == 0 ? new StringBlob("I am a blob !") : new StringBlob(""));
            doc.setPropertyValue("dc:modified",
                    i % 3 == 0 ? GregorianCalendar.from(dayBeforYesterday) : GregorianCalendar.from(now));
            doc.setPropertyValue("dc:created",
                    i % 5 == 0 ? GregorianCalendar.from(dayBeforYesterday) : GregorianCalendar.from(lastWeek));
            session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // wait for async jobs
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testAction() throws Exception {

        String errorMessage = "Bulk action didn't finish";

        // username and repository are retrieved from CoreSession
        Map<String, Serializable> params = new HashMap<>();
        params.put("action", SetPropertiesAction.ACTION_NAME);
        params.put("providerName", "BULK_ACTION_WITH_AGGREGATES");

        Properties namedParameters = new Properties();

        // Test with term aggregate and one value
        namedParameters.put("nature_agg", "[\"nature1\"]");
        params.put("namedParameters", namedParameters);

        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put("dc:title", "dummy title");
        params.put("parameters", new ObjectMapper().writeValueAsString(actionParams));

        BulkStatus result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(result.getId());
        assertEquals(1, status.getProcessed());

        // Test with term aggregate and multiple values
        namedParameters.put("nature_agg", "[\"nature1\",\"nature2\",\"nature3\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(3, status.getProcessed());

        // Test with range aggregate and one value
        namedParameters.remove("nature_agg");
        namedParameters.put("size_agg", "[\"big\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(10, status.getProcessed());

        // Test with range aggregate and multiple values
        namedParameters.put("size_agg", "[\"small\",\"big\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(20, status.getProcessed());

        // Test with date range aggregate and one value
        namedParameters.remove("size_agg");
        namedParameters.put("modified_agg", "[\"lastWeek\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(7, status.getProcessed());

        // Test with date range aggregate and multiple values
        namedParameters.put("modified_agg", "[\"last24h\",\"lastWeek\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(20, status.getProcessed());

        // Test with date histogram aggregate and one value
        namedParameters.remove("modified_agg");

        String formattedDayBeforeYesterday = dayBeforYesterday.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        namedParameters.put("created_agg", "[\"" + formattedDayBeforeYesterday + "\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(4, status.getProcessed());

        // Test with date histogram aggregate and multiple values
        String formattedLastWeek = lastWeek.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        namedParameters.put("created_agg", "[\"" + formattedDayBeforeYesterday + "\",\"" + formattedLastWeek + "\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(20, status.getProcessed());

        // Test with multiple aggregates
        namedParameters.remove("created_agg");
        namedParameters.put("nature_agg", "[\"nature0\",\"nature1\",\"nature2\",\"nature3\",\"nature4\",\"nature6\"]");
        namedParameters.put("modified_agg", "[\"lastWeek\"]");
        namedParameters.put("size_agg", "[\"big\"]");
        params.put("namedParameters", namedParameters);

        result = (BulkStatus) automationService.run(ctx, BulkRunAction.ID, params);
        assertTrue(errorMessage, bulkService.await(result.getId(), Duration.ofSeconds(60)));
        status = bulkService.getStatus(result.getId());
        assertEquals(2, status.getProcessed());

    }

}
