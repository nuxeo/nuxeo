/*
 * (C) Copyright 2018-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.3
 */
@Features({ CoreFeature.class, CoreBulkFeature.class, LogFeature.class })
@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class, cleanup = Granularity.CLASS)
public class TestAutomationBulkAction {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    protected CoreSession session;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected AutomationService service;

    @Inject
    public TransactionalFeature txFeature;

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testSetPropertyActionFromAutomation() throws Exception {
        String nxql = "SELECT * FROM ComplexDoc";
        String title = "title set from automation";
        doTestSetPropertyActionFromAutomation(AutomationBulkAction.ACTION_NAME, nxql, title);
        for (DocumentModel doc : session.query(nxql)) {
            assertEquals(title, doc.getTitle());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.features.tests:test-configuration-service-contrib.xml")
    public void testSetPropertyActionFromAutomationUi() throws Exception {
        String nxql = "SELECT * FROM ComplexDoc WHERE ecm:isProxy = 0";
        String title = "title set from automation UI";
        doTestSetPropertyActionFromAutomation(AutomationBulkActionUi.ACTION_NAME, nxql, title);
        // The configuration service sets a queryLimit=3 for this operationId
        int count = 0;
        for (DocumentModel doc : session.query(nxql)) {
            if (title.equals(doc.getTitle())) {
                count++;
            }
        }
        assertEquals(3, count);
    }

    public void doTestSetPropertyActionFromAutomation(String action, String nxql, String title) throws Exception {
        // param for the automation operation
        var automationParams = new HashMap<>();
        automationParams.put("properties", "dc:title=" + title);
        // param for the automation bulk action
        var actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "Document.Update");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, automationParams);

        // param for the automation BulkRunAction operation
        var bulkActionParam = new HashMap<String, Serializable>();
        bulkActionParam.put("action", action);
        bulkActionParam.put("query", nxql);
        bulkActionParam.put("bucketSize", "10");
        bulkActionParam.put("batchSize", "5");
        bulkActionParam.put("parameters", OBJECT_MAPPER.writeValueAsString(actionParams));
        var runResult = (BulkStatus) service.run(ctx, BulkRunAction.ID, bulkActionParam);

        assertNotNull(runResult);
        // runResult is a json containing commandId
        String commandId = runResult.getId();

        var waitResult = (boolean) service.run(ctx, BulkWaitForAction.ID, Map.of("commandId", commandId));
        assertTrue("Bulk action didn't finish", waitResult);

        txFeature.nextTransaction();
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.features:test-failing-operation-contrib.xml")
    @ConsoleLogLevelThreshold("ERROR") // hide automation trace logs which are verbose
    public void testFailingAutomationErrorHandling() throws Exception {
        // param for the automation bulk action
        var actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "TestFailingOperation");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, Map.of());

        // param for the automation BulkRunAction operation
        var bulkActionParam = new HashMap<String, Serializable>();
        bulkActionParam.put("action", AutomationBulkActionUi.ACTION_NAME);
        bulkActionParam.put("query", "SELECT * FROM ComplexDoc");
        bulkActionParam.put("bucketSize", "10");
        bulkActionParam.put("batchSize", "5");
        bulkActionParam.put("parameters", OBJECT_MAPPER.writeValueAsString(actionParams));
        var runResult = (BulkStatus) service.run(ctx, BulkRunAction.ID, bulkActionParam);

        assertNotNull(runResult);
        // runResult is a json containing commandId
        String commandId = runResult.getId();

        var waitResult = (boolean) service.run(ctx, BulkWaitForAction.ID, Map.of("commandId", commandId));
        assertTrue("Bulk action didn't finish", waitResult);

        txFeature.nextTransaction();

        var nbDocuments = session.query("SELECT * FROM ComplexDoc", null, 0, 0, true).totalSize();

        var status = bulkService.getStatus(commandId);
        assertEquals(State.COMPLETED, status.getState());
        assertEquals(nbDocuments, status.getErrorCount());
        assertTrue(status.getErrorMessage()
                         .startsWith(String.format("Bulk Action Operation with commandId: %s fails on documents: ",
                                 commandId)));
    }

}
