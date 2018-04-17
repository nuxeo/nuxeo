/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.operations.services.PaginableRecordSetImpl;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.core.operations.services.query.ResultSetPaginatedQuery;
import org.nuxeo.ecm.automation.core.util.PaginableRecordSet;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.ecm.automation.core:test-providers.xml")
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
public class SearchOperationTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());

        DocumentModel ws1 = session.createDocumentModel("/", "ws1", "Workspace");
        ws1.setPropertyValue("dc:title", "WS1");
        ws1.setPropertyValue("dc:subjects", new Object[] { "Art/Culture" });
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2007);
        cal.set(Calendar.MONTH, 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, 17);
        ws1.setPropertyValue("dc:issued", cal);
        ws1 = session.createDocument(ws1);

        DocumentModel ws2 = session.createDocumentModel("/", "ws2", "Workspace");
        ws2.setPropertyValue("dc:title", "WS2");
        ws2 = session.createDocument(ws2);

        DocumentModel ws3 = session.createDocumentModel("/", "ws3", "Workspace");
        ws3.setPropertyValue("dc:title", "WS3");
        String[] fakeContributors = { session.getPrincipal().getName() };
        ws3.setPropertyValue("dc:contributors", fakeContributors);
        ws3.setPropertyValue("dc:creator", fakeContributors[0]);
        ws3 = session.createDocument(ws3);
        session.save();
    }

    /**
     * Query | Update.
     */
    @Test
    public void iCanPerformDocumentQueryInChain() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testChain");
        chain.add(DocumentPaginatedQuery.ID).set("query", "SELECT * FROM Workspace");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "samedesc");
        chain.add(SaveDocument.ID);
        DocumentModelList list = (DocumentModelList) service.run(ctx, chain);
        assertEquals(5, list.size());
        assertEquals("samedesc", list.get(0).getPropertyValue("dc:description"));
        assertEquals("samedesc", list.get(0).getPropertyValue("dc:description"));
        assertEquals("samedesc", session.getDocument(src.getRef()).getPropertyValue("dc:description"));
        assertEquals("samedesc", session.getDocument(dst.getRef()).getPropertyValue("dc:description"));
    }

    @Test
    public void iCanPerformResultSetQuery() throws Exception {
        // Given an operation context and following parameters
        OperationContext ctx = new OperationContext(session);

        // When I give a query on all Workspace documents
        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM Workspace");
        PaginableRecordSet list = (PaginableRecordSet) service.run(ctx, ResultSetPaginatedQuery.ID, params);

        // And verify number results and id entry
        assertEquals(5, list.size());
        assertNotNull(list.get(0).get("ecm:uuid"));
    }

    @Test
    public void iCanApplySortParametersWithQuery() throws Exception {
        // Given an operation context and following parameters
        OperationContext ctx = new OperationContext(session);

        // When I give a query and sort
        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM Workspace");
        params.put("sortBy", "dc:title");
        params.put("sortOrder", "ASC");
        DocumentModelList list = (DocumentModelList) service.run(ctx, DocumentPaginatedQuery.ID, params);

        // And verify number results and id entry
        assertEquals(5, list.size());
        assertEquals("Destination", list.get(0).getTitle());

        params.put("query", "SELECT * FROM Workspace");
        params.put("sortBy", "dc:title");
        params.put("sortOrder", "DESC");
        list = (DocumentModelList) service.run(ctx, DocumentPaginatedQuery.ID, params);

        // And verify number results and id entry
        assertEquals(5, list.size());
        assertEquals("WS2", list.get(1).getTitle());
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryWithNamedParametersInvalid() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps(null, null);
        params.put("query", "SELECT * FROM Document where dc:title=:foo ORDER BY dc:title");
        try {
            service.run(ctx, DocumentPaginatedQuery.ID, params);
            fail("Should have raised an OperationException");
        } catch (OperationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains(
                    "Failed to execute query: SELECT * FROM " + "Document " +
                            "where dc:title=:foo ORDER "
                            + "BY dc:title, Lexical Error: Illegal character <:> at offset 38"));
        }
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryWithNamedParametersAndDoc() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps("np:title", "WS1");
        params.put("query", "SELECT * FROM Document where dc:title=:np:title ORDER BY dc:title");
        DocumentModelList list = (DocumentModelList) service.run(ctx, DocumentPaginatedQuery.ID, params);

        // test size
        assertEquals(1, list.size());
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryWithNamedParametersAndDocInvalid() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps("np:title", "WS1");
        params.put("query", "SELECT * FROM Document where dc:title=:foo ORDER BY dc:title");
        try {
            service.run(ctx, DocumentPaginatedQuery.ID, params);
            fail("Should have raised an OperationException");
        } catch (OperationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage()
                        .contains(
                                "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38"));
        }
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryWithNamedParametersInWhereClause() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps("parameter1", "WS1");
        params.put("query", "SELECT * FROM Document where dc:title=:parameter1 ORDER BY dc:title");
        DocumentModelList list = (DocumentModelList) service.run(ctx, DocumentPaginatedQuery.ID, params);

        // test page size
        assertEquals(1, list.size());
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryWithNamedParametersInWhereClauseWithDoc() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps("np:title", "WS1");
        params.put("query", "SELECT * FROM Document where dc:title=:np:title ORDER BY dc:title");
        DocumentModelList list = (DocumentModelList) service.run(ctx, DocumentPaginatedQuery.ID, params);

        // test page size
        assertEquals(1, list.size());
    }

    protected Map<String, Object> getNamedParamsProps(String propName, String propValue) {
        Map<String, Object> params = new HashMap<String, Object>();
        if (propName != null) {
            Map<String, String> namedParameters = new HashMap<>();
            namedParameters.put(propName, propValue);
            Properties namedProperties = new Properties(namedParameters);
            params.put("namedParameters", namedProperties);
        }
        return params;
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryWithNamedParameters() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps("parameter1", "WS1");
        params.put("query", "SELECT * FROM Document where dc:title=:parameter1 ORDER BY dc:title");
        DocumentModelList list = (DocumentModelList) service.run(ctx, DocumentPaginatedQuery.ID, params);

        // test page size
        assertEquals(1, list.size());
    }

    /**
     * @since 8.2
     */
    @Test
    public void testQueryResultSetWithNamedParametersInWhereClause() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = getNamedParamsProps("parameter1", "WS1");
        params.put("query", "SELECT * FROM Document where dc:title=:parameter1 ORDER BY dc:title");
        PaginableRecordSetImpl list = (PaginableRecordSetImpl) service.run(ctx, ResultSetPaginatedQuery.ID, params);

        // test page size
        assertEquals(1, list.size());
    }
}
