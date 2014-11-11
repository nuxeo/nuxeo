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
import static org.junit.Assert.assertTrue;

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

import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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
@LocalDeploy({ "org.nuxeo.ecm.automation.core:test-providers.xml",
        "org.nuxeo.ecm.automation.core:test-operations.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class CoreProviderTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        DocumentModel ws1 = session.createDocumentModel("/", "ws1", "Workspace");
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
    public void testSimplePageProvider() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        String providerName = "simpleProviderTest1";

        params.put("providerName", providerName);

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, params);
        chain.add(oparams);

        PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(
                ctx, chain);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(2, result.getNumberOfPages());
        assertTrue(result.getProvider().isNextPageAvailable());

        // change page size
        chain = new OperationChain("fakeChain");
        params.put("pageSize", 4);
        oparams = new OperationParameters(DocumentPageProviderOperation.ID,
                params);
        chain.add(oparams);

        result = (PaginableDocumentModelListImpl) service.run(ctx, chain);
        assertEquals(4, result.getPageSize());
        assertEquals(1, result.getNumberOfPages());

    }

    @Test
    public void testSimplePageProviderWithParams() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        String providerName = "simpleProviderTest2";

        params.put("providerName", providerName);
        params.put("queryParams", "WS1");

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, params);
        chain.add(oparams);

        PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(
                ctx, chain);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(1, result.getNumberOfPages());
        assertEquals(1, result.size());

        providerName = "simpleProviderTest3";

        params.put("providerName", providerName);
        params.put("queryParams", "WS1,WS2");

        chain = new OperationChain("fakeChain");
        oparams = new OperationParameters(DocumentPageProviderOperation.ID,
                params);
        chain.add(oparams);

        result = (PaginableDocumentModelListImpl) service.run(ctx, chain);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(1, result.getNumberOfPages());
        assertEquals(2, result.size());

    }

    @Test
    public void testDirectNXQL() throws Exception {

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

    @Test
    public void testDirectNXQLWithDynUser() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        params.put("query", "select * from Document where dc:contributors = ?");
        params.put("pageSize", 2);
        params.put("queryParams", "$currentUser");

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, params);
        chain.add(oparams);

        PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(
                ctx, chain);

        assertEquals(1, result.size());
        assertEquals(2, result.getPageSize());
        assertEquals(1, result.getNumberOfPages());

    }

    @Test
    public void testRunOnPageProviderOperation() throws Exception {
        OperationContext context = new OperationContext(session);
        service.run(context, "runOnProviderTestchain");
        DocumentModelList list = (DocumentModelList) context.get("result");
        assertEquals(3, list.size());
    }

    @Test
    public void testDirectNXQLWithMaxResults() throws Exception {

        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<String, Object>();

        params.put("query", "select * from Document");
        params.put("pageSize", 2);
        params.put("maxResults", "2");

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, params);
        chain.add(oparams);

        PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(
                ctx, chain);

        assertEquals(2, result.getPageSize());
        // number of pages should not be set !!!
        assertEquals(0, result.getNumberOfPages());

    }

    /**
     * Covering the use case when contributing integer parameter value in xml
     */
    @Test
    public void testParameterType() throws Exception {
        OperationContext context = new OperationContext(session);
        service.run(context, "testChainParameterType");
        DocumentModelList list = (DocumentModelList) context.get("result");
        assertEquals(3, list.size());
    }

    /**
     * @since 5.9.6
     */
    @Test
    public void testPageProviderWithNamedParameters() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<String, Object>();
        String providerName = "namedParamProvider1";

        params.put("providerName", providerName);
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("parameter1", "WS1");
        Properties namedProperties = new Properties(namedParameters);
        params.put("namedParameters", namedProperties);

        PaginableDocumentModelListImpl result =
                (PaginableDocumentModelListImpl) service.run(ctx,
                        DocumentPageProviderOperation.ID, params);

        // test page size
        assertEquals(2, result.getPageSize());
        assertEquals(1, result.getNumberOfPages());
        assertEquals(1, result.size());
    }
}
