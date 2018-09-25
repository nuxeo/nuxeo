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
 *     pierre
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.bulk.actions.SetPropertiesAction;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.2
 */
@Features(CoreFeature.class)
@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class, cleanup = Granularity.CLASS)
public class TestRunBulkAction {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    public TransactionalFeature txFeature;

    @Test
    public void testSetPropertyActionFromAutomation() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from ComplexDoc where ecm:parentId='%s'", model.getId());

        String title = "test title";
        String description = "test description";
        String foo = "test foo";
        String bar = "test bar";

        HashMap<String, Serializable> complex = new HashMap<>();
        complex.put("foo", foo);
        complex.put("bar", bar);

        OperationContext ctx = new OperationContext(session);
        // username and repository are retrieved from CoreSession
        Map<String, Serializable> params = new HashMap<>();
        params.put("action", SetPropertiesAction.ACTION_NAME);
        params.put("query", nxql);
        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put("dc:title", title);
        actionParams.put("dc:description", description);
        actionParams.put("cpx:complex", complex);
        params.put("parameters", actionParams);

        Blob runResult = (Blob) service.run(ctx, RunBulkAction.ID, params);

        assertNotNull(runResult);
        // runResult is a json containing commandId
        String commandId = new ObjectMapper().readTree(runResult.getString()).get("commandId").asText();

        boolean waitResult = (boolean) service.run(ctx, BulkWaitForAction.ID, singletonMap("commandId", commandId));
        assertTrue("Bulk action didn't finish", waitResult);

        txFeature.nextTransaction();

        for (DocumentModel child : session.query(nxql)) {
            assertEquals(title, child.getTitle());
            assertEquals(description, child.getPropertyValue("dc:description"));
            assertEquals(foo, child.getPropertyValue("cpx:complex/foo"));
            assertEquals(bar, child.getPropertyValue("cpx:complex/bar"));
        }

    }

}
