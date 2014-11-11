/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.test;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.query
        .DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.operations.services.query
        .ResultSetPaginatedQuery;
import org.nuxeo.ecm.automation.core.util.PaginableRecordSet;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features",
        "org.nuxeo.ecm.platform.query.api", "org.nuxeo.runtime.management" })
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
    }


    /**
     * Query | Update.
     */
    @Test
    public void iCanPerformDocumentQueryInChain() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testChain");
        chain.add(DocumentPaginatedQuery.ID).set("query", "SELECT * FROM Workspace");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set(
                "value", "samedesc");
        chain.add(SaveDocument.ID);
        DocumentModelList list = (DocumentModelList) service.run(ctx, chain);
        assertEquals(2, list.size());
        assertEquals("samedesc", list.get(0).getPropertyValue
                ("dc:description"));
        assertEquals("samedesc", list.get(0).getPropertyValue
                ("dc:description"));
        assertEquals(
                "samedesc",
                session.getDocument(src.getRef()).getPropertyValue(
                        "dc:description"));
        assertEquals(
                "samedesc",
                session.getDocument(dst.getRef()).getPropertyValue(
                        "dc:description"));
    }

    @Test
    public void iCanPerformResultSetQuery() throws Exception {
        // Given an operation context and following parameters
        OperationContext ctx = new OperationContext(session);

        // When I give a query on all Workspace documents
        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM Workspace");
        PaginableRecordSet list = (PaginableRecordSet) service.run(ctx,
                ResultSetPaginatedQuery.ID, params);

        // And verify number results and id entry
        assertEquals(2, list.size());
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
        DocumentModelList list = (DocumentModelList) service.run(ctx,
                DocumentPaginatedQuery.ID, params);

        // And verify number results and id entry
        assertEquals(2, list.size());
        assertEquals("Destination",list.get(0).getTitle());

        params.put("query", "SELECT * FROM Workspace");
        params.put("sortBy", "dc:title");
        params.put("sortOrder", "DESC");
        list = (DocumentModelList) service.run(ctx,
                DocumentPaginatedQuery.ID, params);

        // And verify number results and id entry
        assertEquals(2, list.size());
        assertEquals("Destination",list.get(1).getTitle());
    }
}
