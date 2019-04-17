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
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.action.SetPropertiesAction;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.2
 */
@Features({ CoreFeature.class, CoreBulkFeature.class })
@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.features:test-providers.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class, cleanup = Granularity.CLASS)
public class TestBulkRunAction {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    public TransactionalFeature txFeature;

    @Inject
    protected PageProviderService ppService;

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
    public void testSetPropertyActionFromAutomationQuery() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from ComplexDoc where ecm:parentId='%s'", model.getId());

        String title = "test title";
        String description = "test description";
        String foo = "test foo";
        String bar = "test bar";

        HashMap<String, Serializable> complex = new HashMap<>();
        complex.put("foo", foo);
        complex.put("bar", bar);

        // username and repository are retrieved from CoreSession
        Map<String, Serializable> params = new HashMap<>();
        params.put("action", SetPropertiesAction.ACTION_NAME);
        params.put("query", nxql);
        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put("dc:title", title);
        actionParams.put("dc:description", description);
        actionParams.put("cpx:complex", complex);
        params.put("parameters", OBJECT_MAPPER.writeValueAsString(actionParams));

        BulkStatus runResult = (BulkStatus) service.run(ctx, BulkRunAction.ID, params);

        assertNotNull(runResult);

        String commandId = runResult.getId();

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

    @Test
    public void testSetPropertyActionFromAutomationProvider() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));

        String title = "test title pp";
        String description = "test description pp";
        String foo = "test foo pp";
        String bar = "test bar pp";

        HashMap<String, Serializable> complex = new HashMap<>();
        complex.put("foo", foo);
        complex.put("bar", bar);

        // username and repository are retrieved from CoreSession
        Map<String, Serializable> params = new HashMap<>();
        params.put("action", SetPropertiesAction.ACTION_NAME);

        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put("dc:title", title);
        actionParams.put("dc:description", description);
        actionParams.put("cpx:complex", complex);
        params.put("parameters", OBJECT_MAPPER.writeValueAsString(actionParams));

        params.put("providerName", "bulkPP");
        params.put("queryParams", model.getId());

        BulkStatus runResult = (BulkStatus) service.run(ctx, BulkRunAction.ID, params);

        assertNotNull(runResult);

        String commandId = runResult.getId();

        boolean waitResult = (boolean) service.run(ctx, BulkWaitForAction.ID, singletonMap("commandId", commandId));
        assertTrue("Bulk action didn't finish", waitResult);

        txFeature.nextTransaction();

        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider("bulkPP",
                null, null, null,
                Collections.singletonMap(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session),
                model.getId());

        for (DocumentModel child : pageProvider.getCurrentPage()) {
            assertEquals(title, child.getTitle());
            assertEquals(description, child.getPropertyValue("dc:description"));
            assertEquals(foo, child.getPropertyValue("cpx:complex/foo"));
            assertEquals(bar, child.getPropertyValue("cpx:complex/bar"));
        }
    }

    @Test
    public void testParametersHandling() throws Exception {
        // username and repository are retrieved from CoreSession
        Map<String, Serializable> params = new HashMap<>();

        // should not work without action
        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Expected exception");
        } catch (OperationException e) {
            assertTrue(e.getMessage().contains("Failed to inject parameter 'action'"));
        }

        // should not work with unknown action
        params.put("action", "foobar");
        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Expected exception");
        } catch (NuxeoException e) {
            assertEquals(HttpServletResponse.SC_NOT_FOUND, e.getStatusCode());
        }

        params.put("action", SetPropertiesAction.ACTION_NAME);

        // should not work without query and providerName
        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Expected exception");
        } catch (OperationException e) {
            assertEquals("Query and ProviderName cannot be both null", e.getMessage());
        }

        // should work with a query
        params.put("query", "SELECT * FROM Document");
        service.run(ctx, BulkRunAction.ID, params);

        // should not work with a parameterized query
        params.put("query", "SELECT * FROM Document where ecm:parentId=?");
        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Expected exception");
        } catch (OperationException e) {
            assertEquals("Query parameters could not be parsed", e.getMessage());
        }

        // should not work with unknown provider name
        params.remove("query");
        params.put("providerName", "unknow provider name");
        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Expected exception");
        } catch (OperationException e) {
            assertEquals("Could not get Provider Definition from either query or provider name", e.getMessage());
        }

        // should work with unparameterized simpleProviderTest1
        params.put("providerName", "simpleProviderTest1");
        service.run(ctx, BulkRunAction.ID, params);

        // should not work with invalid parameters
        params.put("query", "SELECT * FROM Document");
        params.put("parameters", "foobar");
        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Expected exception");
        } catch (OperationException e) {
            assertEquals("Could not parse parameters, expecting valid json value", e.getMessage());
        }
    }

    @Test
    public void testInvalidActionParameters() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from ComplexDoc where ecm:parentId='%s'", model.getId());

        Map<String, Serializable> params = new HashMap<>();
        params.put("action", SetPropertiesAction.ACTION_NAME);
        params.put("query", nxql);
        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put("unknown:path", "unknown");
        params.put("parameters", OBJECT_MAPPER.writeValueAsString(actionParams));

        try {
            service.run(ctx, BulkRunAction.ID, params);
            fail("Command should have failed");
        } catch (OperationException e) {
            // ok
        }
    }
}
