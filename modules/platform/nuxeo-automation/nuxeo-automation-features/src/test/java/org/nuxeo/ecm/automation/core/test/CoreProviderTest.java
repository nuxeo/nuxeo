/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.concurrent.ThreadFactories;
import org.nuxeo.common.function.ThrowableRunnable;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.features.AutomationFeaturesFeature;
import org.nuxeo.ecm.automation.io.rest.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeaturesFeature.class)
@Deploy("org.nuxeo.ecm.automation.core:test-providers.xml")
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class CoreProviderTest {

    @Inject
    protected AutomationService service;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void initRepo() throws Exception {
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

    @Test
    public void testSimplePageProvider() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {

            Map<String, Object> params = new HashMap<>();

            String providerName = "simpleProviderTest1";

            params.put("providerName", providerName);

            OperationChain chain = new OperationChain("fakeChain");
            OperationParameters oparams = new OperationParameters(DocumentPageProviderOperation.ID, params);
            chain.add(oparams);

            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx, chain);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(2, result.getNumberOfPages());
            assertTrue(result.getProvider().isNextPageAvailable());

            // change page size
            chain = new OperationChain("fakeChain");
            params.put("pageSize", 4);
            oparams = new OperationParameters(DocumentPageProviderOperation.ID, params);
            chain.add(oparams);

            result = (PaginableDocumentModelListImpl) service.run(ctx, chain);
            assertEquals(4, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
        }

    }

    @Test
    public void testSimplePageProviderWithParams() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {

            Map<String, Object> params = new HashMap<>();

            String providerName = "simpleProviderTest2";

            params.put("providerName", providerName);
            params.put("queryParams", "WS1");

            OperationChain chain = new OperationChain("fakeChain");
            OperationParameters oparams = new OperationParameters(DocumentPageProviderOperation.ID, params);
            chain.add(oparams);

            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx, chain);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(1, result.size());

            providerName = "simpleProviderTest3";

            params.put("providerName", providerName);
            params.put("queryParams", "WS1,WS2");

            chain = new OperationChain("fakeChain");
            oparams = new OperationParameters(DocumentPageProviderOperation.ID, params);
            chain.add(oparams);

            result = (PaginableDocumentModelListImpl) service.run(ctx, chain);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(2, result.size());
        }

    }

    @Test
    public void testDirectNXQL() throws Exception {

        PaginableDocumentModelListImpl result;

        try (OperationContext ctx = new OperationContext(session)) {
            // get paginated results
            Map<String, Object> params = new HashMap<>();
            params.put("query", "select * from Document");
            params.put("pageSize", 2);

            result = (PaginableDocumentModelListImpl) service.run(ctx, DocumentPaginatedQuery.ID, params);

            assertEquals(2, result.getPageSize());
            assertEquals(2, result.getNumberOfPages());

            params.clear();

            // params are quoted by default
            params.put("query", "select * from Document where dc:title = ?");
            params.put("queryParams", "WS1");

            result = (PaginableDocumentModelListImpl) service.run(ctx, DocumentPaginatedQuery.ID, params);

            assertEquals(1, result.size());

            params.clear();

            // unquote params fail to parse
            params.put("query", "select * from Document where dc:title = ?");
            params.put("queryParams", "WS1");
            params.put("quotePatternParameters", false);

            assertThrows(OperationException.class, () -> service.run(ctx, DocumentPaginatedQuery.ID, params));

            params.clear();

            // params are escaped by default
            params.put("query", "select * from Document where dc:title = ?");
            params.put("quotePatternParameters", false);
            params.put("queryParams", "'WS1'");

            assertThrows(OperationException.class, () -> service.run(ctx, DocumentPaginatedQuery.ID, params));

            // we can toggle escaping of parameters off
            params.put("escapePatternParameters", false);

            result = (PaginableDocumentModelListImpl) service.run(ctx, DocumentPaginatedQuery.ID, params);

            assertEquals(1, result.size());
        }

    }

    @Test
    public void testDirectNXQLWithDynUser() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {

            Map<String, Object> params = new HashMap<>();

            params.put("query", "select * from Document where dc:contributors = ?");
            params.put("pageSize", 2);
            params.put("queryParams", "$currentUser");

            OperationChain chain = new OperationChain("fakeChain");
            OperationParameters oparams = new OperationParameters(DocumentPaginatedQuery.ID, params);
            chain.add(oparams);

            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx, chain);

            assertEquals(1, result.size());
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
        }

    }

    @Test
    public void testRunOnPageProviderOperation() throws Exception {
        try (OperationContext context = new OperationContext(session)) {
            service.run(context, "runOnProviderTestChain");
            DocumentModelList list = (DocumentModelList) context.get("result");
            assertEquals(3, list.size());
        }
    }

    // NXP-32817
    @Test
    public void testRunOnPageProviderOperationDoNotLoopInfinitely() {
        // we want to check that the operation doesn't end in an infinite loop when there's concurrent update
        var providerService = Framework.getService(PageProviderService.class);
        var provider = providerService.getPageProviderDefinition("simpleProviderTest4");
        var pageSize = provider.getPageSize();
        var nbDocs = pageSize * 10;
        for (int i = 0; i < nbDocs; i++) {
            var doc = session.createDocumentModel("/", "file_%02d".formatted(i), "File");
            doc.setPropertyValue("dc:format", "before-chain-run");
            doc.setPropertyValue("dc:title", "file_%02d".formatted(i));
            session.createDocument(doc);
        }
        session.save();
        txFeature.nextTransaction(); // make the change visible to other threads
        try (var executor = Executors.newSingleThreadExecutor(
                ThreadFactories.newThreadFactory("concurrent-updater", true));
                OperationContext context = new OperationContext(session)) {
            executor.submit(createRunnableForConcurrentUpdate());
            var e = assertThrows(NuxeoException.class,
                    () -> service.run(context, "testRunOnProviderAChainThatChangeTheProviderResult"));
            assertTrue("Exception message incorrect, actual: " + e.getMessage(),
                    e.getMessage()
                     .contains(
                             "Infinite loop detected while running automation: testUpdateFormat on pageProvider: simpleProviderTest4"));
        }
    }

    protected Runnable createRunnableForConcurrentUpdate() {
        var atomicLong = new AtomicLong(0);
        return () -> TransactionHelper.runInTransaction(ThrowableRunnable.asRunnable(() -> {
            var keyValueService = Framework.getService(KeyValueService.class);
            var keyValueStore = keyValueService.getKeyValueStore("concurrentUpdateStore");
            // wait for the pagination to start
            long begin = System.currentTimeMillis();
            while (!"true".equals(keyValueStore.getString("waitForConcurrentUpdate"))) {
                if (System.currentTimeMillis() - begin > 1_000) {
                    throw new TimeoutException("Timed out waiting for: concurrentUpdateStore/waitForConcurrentUpdate");
                }
                Thread.sleep(100);
            }
            // do the update - increment should be in synced with doc under operation processing
            var doc = session.getDocument(new PathRef("/file_%02d".formatted(atomicLong.getAndIncrement())));
            doc.setPropertyValue("dc:format", "concurrent-update-outside-chain");
            session.saveDocument(doc);
            session.save();
            // let the chain knows the update is performed
            keyValueStore.compareAndSet("updateDone", keyValueStore.getString("updateDone"), "true");
        }));
    }

    @Test
    public void testDirectNXQLWithMaxResults() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {

            Map<String, Object> params = new HashMap<>();

            params.put("query", "select * from Document");
            params.put("pageSize", 2);
            params.put("maxResults", "2");

            OperationChain chain = new OperationChain("fakeChain");
            OperationParameters oparams = new OperationParameters(DocumentPaginatedQuery.ID, params);
            chain.add(oparams);

            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx, chain);

            assertEquals(2, result.getPageSize());
            // number of pages should not be set !!!
            assertEquals(0, result.getNumberOfPages());
        }

    }

    /**
     * Covering the use case when contributing integer parameter value in xml
     */
    @Test
    public void testParameterType() throws Exception {
        try (OperationContext context = new OperationContext(session)) {
            service.run(context, "testChainParameterType");
            DocumentModelList list = (DocumentModelList) context.get("result");
            assertEquals(3, list.size());
        }
    }

    protected Map<String, Object> getNamedParamsProps(String providerName, String propName, String propValue) {
        Map<String, Object> params = new HashMap<>();
        params.put("providerName", providerName);
        if (propName != null) {
            Map<String, String> namedParameters = new HashMap<>();
            namedParameters.put(propName, propValue);
            Properties namedProperties = new Properties(namedParameters);
            params.put("namedParameters", namedProperties);
        }
        return params;
    }

    /**
     * @since 6.0
     */
    @Test
    public void testPageProviderWithNamedParameters() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = getNamedParamsProps("namedParamProvider", "parameter1", "WS1");
            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx,
                    DocumentPageProviderOperation.ID, params);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(1, result.size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void testPageProviderWithNamedParametersInvalid() {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = getNamedParamsProps("namedParamProviderInvalid", null, null);
            var e = assertThrows(OperationException.class,
                    () -> service.run(ctx, DocumentPageProviderOperation.ID, params));
            assertNotNull(e.getMessage());
            assertTrue(
                    e.getMessage()
                     .contains(
                             "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38"));
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void testPageProviderWithNamedParametersAndDoc() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = getNamedParamsProps("namedParamProviderWithDoc", "np:title", "WS1");
            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx,
                    DocumentPageProviderOperation.ID, params);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(1, result.size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void testPageProviderWithNamedParametersAndDocInvalid() {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = getNamedParamsProps("namedParamProviderWithDocInvalid", "np:title", "WS1");
            var e = assertThrows(OperationException.class,
                    () -> service.run(ctx, DocumentPageProviderOperation.ID, params));
            assertNotNull(e.getMessage());
            assertTrue(
                    e.getMessage()
                     .contains(
                             "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38"));
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void testPageProviderWithNamedParametersInWhereClause() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = getNamedParamsProps("namedParamProviderWithWhereClause", "parameter1", "WS1");
            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx,
                    DocumentPageProviderOperation.ID, params);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(1, result.size());

            // retry without params
            params = getNamedParamsProps("namedParamProviderWithWhereClause", null, null);
            result = (PaginableDocumentModelListImpl) service.run(ctx, DocumentPageProviderOperation.ID, params);

            // test page size
            assertEquals(2, result.getPageSize());
            assertEquals(2, result.getNumberOfPages());
            assertEquals(2, result.size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void testPageProviderWithNamedParametersComplex() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("providerName", "namedParamProviderComplex");
            Map<String, String> namedParameters = new HashMap<>();
            namedParameters.put("parameter1", "WS1");
            namedParameters.put("np:isCheckedIn", Boolean.FALSE.toString());
            namedParameters.put("np:dateMin", "2007-01-30 01:02:03+04:00");
            namedParameters.put("np:dateMax", "2007-03-23 01:02:03+04:00");
            Properties namedProperties = new Properties(namedParameters);
            params.put("namedParameters", namedProperties);
            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx,
                    DocumentPageProviderOperation.ID, params);

            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(1, result.size());

            // remove filter on dates
            namedParameters.remove("np:dateMin");
            namedParameters.remove("np:dateMax");
            namedProperties = new Properties(namedParameters);
            params.put("namedParameters", namedProperties);
            result = (PaginableDocumentModelListImpl) service.run(ctx, DocumentPageProviderOperation.ID, params);

            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getNumberOfPages());
            assertEquals(1, result.size());

            // remove filter on title
            namedParameters.remove("parameter1");
            namedProperties = new Properties(namedParameters);
            params.put("namedParameters", namedProperties);
            result = (PaginableDocumentModelListImpl) service.run(ctx, DocumentPageProviderOperation.ID, params);

            assertEquals(2, result.getPageSize());
            assertEquals(2, result.getNumberOfPages());
            assertEquals(2, result.size());
        }
    }

    /**
     * @since 7.3
     */
    @Test
    public void canUseINOperatorWithQueryParams() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("providerName", "searchWithInOperatorAndQueryParams");
            StringList list = new StringList();
            list.add("\"Art/Architecture\", \"Art/Culture\"");
            params.put("queryParams", list);
            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx,
                    DocumentPageProviderOperation.ID, params);
            assertEquals(1, result.size());
        }
    }

    /**
     * @since 7.3
     */
    @Test
    public void canUseDateWrapperIntoNamedParameter() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) service.run(ctx, "dateWrapper");
            assertNotNull(result);
        }
    }
}
