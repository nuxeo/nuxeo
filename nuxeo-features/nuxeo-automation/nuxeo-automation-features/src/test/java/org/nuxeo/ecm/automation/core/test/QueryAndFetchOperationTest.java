/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.PaginableRecordSetImpl;
import org.nuxeo.ecm.automation.core.operations.services.ResultSetPageProviderOperation;

import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.automation.features" })
@LocalDeploy({ "org.nuxeo.ecm.automation.core:test-qf-providers.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class QueryAndFetchOperationTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    protected DocumentModel ws1;

    @Before
    public void initRepo() throws Exception {
        ws1 = session.createDocumentModel("/", "ws1", "Workspace");
        ws1.setPropertyValue("dc:title", "WS1");
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

    @Test
    public void testSimplePageProviderWithParams() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        String providerName = "CURRENT_DOCUMENT_CHILDREN_FETCH";

        params.put("providerName", providerName);
        params.put("queryParams", session.getRootDocument().getId());

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                ResultSetPageProviderOperation.ID, params);
        chain.add(oparams);

        PaginableRecordSetImpl result = (PaginableRecordSetImpl) service.run(
                ctx, chain);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(2, result.getNumberOfPages());
        assertEquals(2, result.size());

        // test comlumn
        assertEquals("WS1", result.get(0).get("dc:title"));
        assertEquals(ws1.getId(), result.get(0).get("ecm:uuid"));

        providerName = "simpleProviderTest3";

    }

    @Test
    public void testResultSetPageProviderWithNamedParams() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        String providerName = "CURRENT_DOCUMENT_CHILDREN_FETCH_NAMED_PARAMS";

        params.put("providerName", providerName);
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("parentIdVar", session.getRootDocument().getId());
        Properties namedProperties = new Properties(namedParameters);
        params.put("namedParameters", namedProperties);

        PaginableRecordSetImpl result = (PaginableRecordSetImpl) service.run(
                ctx, ResultSetPageProviderOperation.ID, params);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(2, result.getNumberOfPages());
        assertEquals(2, result.size());

        // test column
        assertEquals("WS1", result.get(0).get("dc:title"));
        assertEquals(ws1.getId(), result.get(0).get("ecm:uuid"));

    }

    // @Test
    public void XXXtestDirectNXQL() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        params.put("query", "select * from Document");
        params.put("pageSize", 2);

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, params);
        chain.add(oparams);

        PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(
                ctx, chain);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(2, result.getNumberOfPages());

    }
}
