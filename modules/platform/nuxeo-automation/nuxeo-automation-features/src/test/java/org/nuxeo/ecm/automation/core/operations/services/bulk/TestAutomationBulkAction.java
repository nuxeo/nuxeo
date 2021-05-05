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
 *     bdelbosc
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;
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
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.3
 */
@Features({ CoreFeature.class, CoreBulkFeature.class })
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
        // param for the automation operation
        HashMap<String, Serializable> automationParams = new HashMap<>();
        automationParams.put("properties", "dc:title=foo");
        // param for the automation bulk action
        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "Document.Update");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, automationParams);

        // param for the automation BulkRunAction operation
        Map<String, Serializable> bulkActionParam = new HashMap<>();
        String nxql = "SELECT * FROM ComplexDoc";
        bulkActionParam.put("action", AutomationBulkAction.ACTION_NAME);
        bulkActionParam.put("query", nxql);
        bulkActionParam.put("bucketSize", "10");
        bulkActionParam.put("batchSize", "5");
        bulkActionParam.put("parameters", OBJECT_MAPPER.writeValueAsString(actionParams));
        BulkStatus runResult = (BulkStatus) service.run(ctx, BulkRunAction.ID, bulkActionParam);

        assertNotNull(runResult);
        // runResult is a json containing commandId
        String commandId = runResult.getId();

        boolean waitResult = (boolean) service.run(ctx, BulkWaitForAction.ID, singletonMap("commandId", commandId));
        assertTrue("Bulk action didn't finish", waitResult);

        txFeature.nextTransaction();
        for (DocumentModel doc : session.query(nxql)) {
            assertEquals("foo", doc.getTitle());
        }

    }

}
