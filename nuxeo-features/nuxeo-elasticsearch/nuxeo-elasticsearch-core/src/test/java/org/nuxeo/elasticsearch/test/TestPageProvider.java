/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.elasticsearch.query.PageProviderQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@SuppressWarnings("unchecked")
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:schemas-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestPageProvider {

    @Inject
    protected LogFeature logFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchService ess;

    private int commandProcessed;

    // Number of processed command since the startTransaction
    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        Assert.assertEquals(processed, esa.getTotalCommandProcessed() - commandProcessed);
    }

    /**
     * Wait for async worker completion then wait for indexing completion
     */
    public void waitForCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    protected void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void ICanUseANativePageProvider() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("NATIVE_PP_PATTERN");
        Assert.assertNotNull(ppdef);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pps.getPageProvider("NATIVE_PP_PATTERN", ppdef, null, null, pageSize, 0L, props);
        Assert.assertNotNull(pp);

        // create 10 docs
        startTransaction();
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        // get current page
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertNotNull(p);
        Assert.assertEquals(pageSize, p.size());
        Assert.assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
        Assert.assertEquals("TestMe9", doc.getTitle());

        pp.nextPage();
        p = (List<DocumentModel>) pp.getCurrentPage();
        Assert.assertEquals(pageSize, p.size());
        doc = p.get((int) pageSize - 1);
        Assert.assertEquals("TestMe0", doc.getTitle());
    }

    @Test
    public void ICanUseANxqlPageProvider() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("NXQL_PP_PATTERN");
        Assert.assertNotNull(ppdef);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pps.getPageProvider("NXQL_PP_PATTERN", ppdef,
                null, null, pageSize, 0L, props);
        Assert.assertNotNull(pp);

        // create 10 docs
        startTransaction();
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        // get current page
        List<DocumentModel> p = pp.getCurrentPage();
        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertNotNull(p);
        Assert.assertEquals(pageSize, p.size());
        Assert.assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
        Assert.assertEquals("TestMe9", doc.getTitle());

        Assert.assertTrue(pp.isLastPageAvailable());
        Assert.assertTrue(pp.isNextPageAvailable());

        pp.nextPage();
        p = pp.getCurrentPage();
        Assert.assertEquals(pageSize, p.size());
        doc = p.get((int) pageSize - 1);
        Assert.assertEquals("TestMe0", doc.getTitle());

        pageSize = 10000;
        ppdef = pps.getPageProviderDefinition("NXQL_PP_PATTERN2");
        Assert.assertNotNull(ppdef);
        pp = (ElasticSearchNxqlPageProvider) pps.getPageProvider("NXQL_PP_PATTERN2", ppdef, null, null, pageSize, 0L,
                props);
        Assert.assertNotNull(pp);
        p = pp.getCurrentPage();
        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertEquals(10, p.size());
        doc = p.get(0);
        Assert.assertEquals("TestMe9", doc.getTitle());

    }

    @Test
    public void ICanUseANxqlPageProviderWithParameters() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("nxql_search");
        Assert.assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pps.getPageProvider("nxql_search", ppdef, null, null, pageSize, 0L, props);
        startTransaction();
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);
        startTransaction();

        // get current page
        String[] params = { "Select * from File where dc:title LIKE 'Test%'" };
        pp.setParameters(params);
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        String esquery = ((ElasticSearchNxqlPageProvider) pp).getCurrentQueryAsEsBuilder().toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"bool\" : {\n" + "    \"must\" : [\n" + "      {\n"
                + "        \"match_phrase_prefix\" : {\n" + "          \"dc:title\" : {\n"
                + "            \"query\" : \"Test\",\n" + "            \"slop\" : 0,\n"
                + "            \"max_expansions\" : 50,\n" + "            \"boost\" : 1.0\n" + "          }\n"
                + "        }\n" + "      }\n" + "    ],\n" + "    \"filter\" : [\n" + "      {\n"
                + "        \"terms\" : {\n" + "          \"ecm:primaryType\" : [\n" + "            \"File\"\n"
                + "          ],\n" + "          \"boost\" : 1.0\n" + "        }\n" + "      }\n" + "    ],\n"
                + "    \"adjust_pure_negative\" : true,\n"
                + "    \"boost\" : 1.0\n" + "  }\n" + "}", esquery);

        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertNotNull(p);
        Assert.assertEquals(pageSize, p.size());
        Assert.assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
    }

    @Test
    public void ICanUseANxqlPageProviderWithFixedPart() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("NXQL_PP_FIXED_PART");
        Assert.assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pps.getPageProvider("NXQL_PP_FIXED_PART", ppdef, model, null, pageSize, 0L, props);
        // create 10 docs
        startTransaction();
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);
        startTransaction();

        String[] params = { session.getRootDocument().getId() };
        pp.setParameters(params);

        // get current page
        List<DocumentModel> p = (List<DocumentModel>) pp.getCurrentPage();
        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertNotNull(p);
        Assert.assertEquals(pageSize, p.size());
        Assert.assertEquals(2, pp.getNumberOfPages());
        DocumentModel doc = p.get(0);
    }

    @Test
    public void ICanUseInvalidPageProvider() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("INVALID_PP");
        Assert.assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<?> pp = pps.getPageProvider("INVALID_PP", ppdef, null, null, 0L, 0L, props);
        assertNotNull(pp);
        logFeature.hideWarningFromConsoleLog();
        List<?> p = pp.getCurrentPage();
        logFeature.restoreConsoleLog();
        assertNotNull(p);
        assertEquals(0, p.size());
        assertEquals("Syntax error: Invalid token <ORDER BY> at offset 29", pp.getErrorMessage());
    }

    @Test
    public void testNativePredicateIn() throws Exception {
        QueryBuilder qb;
        PageProviderService pps = Framework.getService(PageProviderService.class);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition("TEST_IN").getWhereClause();
        DocumentModel model = session.createDocumentModel("/", "doc", "File");
        model.setPropertyValue("dc:subjects", new String[] { "foo", "bar" });

        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"dc:title\" : [\n" +
                "                \"foo\",\n" +
                "                \"bar\"\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        model.setPropertyValue("dc:subjects", new String[] { "foo" });
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"dc:title\" : [\n" +
                "                \"foo\"\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        // criteria with no values are removed
        model.setPropertyValue("dc:subjects", new String[] {});
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());
    }

    @Test
    public void testNativePredicateInIntegers() throws Exception {
        QueryBuilder qb;
        PageProviderService pps = Framework.getService(PageProviderService.class);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition("TEST_IN_INTEGERS").getWhereClause();
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        @SuppressWarnings("boxing")
        Integer[] array1 = new Integer[] { 1, 2, 3 };
        model.setPropertyValue("search:integerlist", array1);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"size\" : [\n" +
                "                1,\n" +
                "                2,\n" +
                "                3\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        // lists work too
        @SuppressWarnings("boxing")
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        model.setPropertyValue("search:integerlist", (Serializable) list);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"size\" : [\n" +
                "                1,\n" +
                "                2,\n" +
                "                3\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

    }

    @Test
    public void testNativePredicateInStringList() throws Exception {
        QueryBuilder qb;
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition("ADVANCED_SEARCH").getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] arrayString = new String[] { "1", "2", "3" };
        model.setPropertyValue("search:subjects", arrayString);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        String json = qb.toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"query_string\" : {\n" +
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" +
                "          \"fields\" : [ ],\n" +
                "          \"type\" : \"best_fields\",\n" +
                "          \"default_operator\" : \"or\",\n" +
                "          \"max_determinized_states\" : 10000,\n" +
                "          \"enable_position_increments\" : true,\n" +
                "          \"fuzziness\" : \"AUTO\",\n" +
                "          \"fuzzy_prefix_length\" : 0,\n" +
                "          \"fuzzy_max_expansions\" : 50,\n" +
                "          \"phrase_slop\" : 0,\n" +
                "          \"escape\" : false,\n" +
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "          \"fuzzy_transpositions\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"dc:subjects\" : [\n" +
                "                \"1\",\n" +
                "                \"2\",\n" +
                "                \"3\"\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        // lists work too
        @SuppressWarnings("boxing")
        List<String> list = Arrays.asList(arrayString);
        model.setPropertyValue("search:subjects", (Serializable) list);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows(json, qb.toString());

        // don't take into account empty list
        list = new ArrayList<>();
        model.setPropertyValue("search:subjects", (Serializable) list);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, null, true);
        assertEqualsEvenUnderWindows("{\n" + "  \"match_all\" : {\n" + "    \"boost\" : 1.0\n" + "  }\n" + "}",
                qb.toString());
    }

    @Test
    public void testNativePredicateIsNull() throws Exception {
        QueryBuilder qb;
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        WhereClauseDefinition whereClause = pps.getPageProviderDefinition("ADVANCED_SEARCH").getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setPropertyValue("search:title", "bar");

        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"query_string\" : {\n" +
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" +
                "          \"fields\" : [ ],\n" +
                "          \"type\" : \"best_fields\",\n" +
                "          \"default_operator\" : \"or\",\n" +
                "          \"max_determinized_states\" : 10000,\n" +
                "          \"enable_position_increments\" : true,\n" +
                "          \"fuzziness\" : \"AUTO\",\n" +
                "          \"fuzzy_prefix_length\" : 0,\n" +
                "          \"fuzzy_max_expansions\" : 50,\n" +
                "          \"phrase_slop\" : 0,\n" +
                "          \"escape\" : false,\n" +
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "          \"fuzzy_transpositions\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"wildcard\" : {\n" +
                "          \"dc:title\" : {\n" +
                "            \"wildcard\" : \"bar\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        model.setPropertyValue("search:isPresent", Boolean.TRUE);

        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"query_string\" : {\n" +
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" +
                "          \"fields\" : [ ],\n" +
                "          \"type\" : \"best_fields\",\n" +
                "          \"default_operator\" : \"or\",\n" +
                "          \"max_determinized_states\" : 10000,\n" +
                "          \"enable_position_increments\" : true,\n" +
                "          \"fuzziness\" : \"AUTO\",\n" +
                "          \"fuzzy_prefix_length\" : 0,\n" +
                "          \"fuzzy_max_expansions\" : 50,\n" +
                "          \"phrase_slop\" : 0,\n" +
                "          \"escape\" : false,\n" +
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "          \"fuzzy_transpositions\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"wildcard\" : {\n" +
                "          \"dc:title\" : {\n" +
                "            \"wildcard\" : \"bar\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"bool\" : {\n" +
                "              \"must_not\" : [\n" +
                "                {\n" +
                "                  \"exists\" : {\n" +
                "                    \"field\" : \"dc:modified\",\n" +
                "                    \"boost\" : 1.0\n" +
                "                  }\n" +
                "                }\n" +
                "              ],\n" +
                "              \"adjust_pure_negative\" : true,\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        // only boolean available in schema without default value
        model.setPropertyValue("search:isPresent", Boolean.FALSE);
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"query_string\" : {\n" +
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" +
                "          \"fields\" : [ ],\n" +
                "          \"type\" : \"best_fields\",\n" +
                "          \"default_operator\" : \"or\",\n" +
                "          \"max_determinized_states\" : 10000,\n" +
                "          \"enable_position_increments\" : true,\n" +
                "          \"fuzziness\" : \"AUTO\",\n" +
                "          \"fuzzy_prefix_length\" : 0,\n" +
                "          \"fuzzy_max_expansions\" : 50,\n" +
                "          \"phrase_slop\" : 0,\n" +
                "          \"escape\" : false,\n" +
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "          \"fuzzy_transpositions\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"wildcard\" : {\n" +
                "          \"dc:title\" : {\n" +
                "            \"wildcard\" : \"bar\",\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"bool\" : {\n" +
                "              \"must_not\" : [\n" +
                "                {\n" +
                "                  \"exists\" : {\n" +
                "                    \"field\" : \"dc:modified\",\n" +
                "                    \"boost\" : 1.0\n" +
                "                  }\n" +
                "                }\n" +
                "              ],\n" +
                "              \"adjust_pure_negative\" : true,\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

        qb = PageProviderQueryBuilder.makeQuery("SELECT * FROM ? WHERE ? = '?'",
                new Object[]{"Document", "dc:title", null}, false, true, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"query_string\" : {\n" +
                "    \"query\" : \"SELECT * FROM Document WHERE dc:title = ''\",\n" +
                "    \"fields\" : [ ],\n" +
                "    \"type\" : \"best_fields\",\n" +
                "    \"default_operator\" : \"or\",\n" +
                "    \"max_determinized_states\" : 10000,\n" +
                "    \"enable_position_increments\" : true,\n" +
                "    \"fuzziness\" : \"AUTO\",\n" +
                "    \"fuzzy_prefix_length\" : 0,\n" +
                "    \"fuzzy_max_expansions\" : 50,\n" +
                "    \"phrase_slop\" : 0,\n" +
                "    \"escape\" : false,\n" +
                "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "    \"fuzzy_transpositions\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());

    }

    @Test
    public void testNativeFulltext() throws Exception {
        QueryBuilder qb;
        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        WhereClauseDefinition whereClause = pps.getPageProviderDefinition("ADVANCED_SEARCH").getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setPropertyValue("search:fulltext_all", "you know for search");
        qb = PageProviderQueryBuilder.makeQuery(model, whereClause, params, true);
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"query_string\" : {\n" +
                "          \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\",\n" +
                "          \"fields\" : [ ],\n" +
                "          \"type\" : \"best_fields\",\n" +
                "          \"default_operator\" : \"or\",\n" +
                "          \"max_determinized_states\" : 10000,\n" +
                "          \"enable_position_increments\" : true,\n" +
                "          \"fuzziness\" : \"AUTO\",\n" +
                "          \"fuzzy_prefix_length\" : 0,\n" +
                "          \"fuzzy_max_expansions\" : 50,\n" +
                "          \"phrase_slop\" : 0,\n" +
                "          \"escape\" : false,\n" +
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "          \"fuzzy_transpositions\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"simple_query_string\" : {\n" +
                "          \"query\" : \"you know for search\",\n" +
                "          \"fields\" : [\n" +
                "            \"all_field^1.0\"\n" +
                "          ],\n" +
                "          \"analyzer\" : \"fulltext\",\n" +
                "          \"flags\" : -1,\n" +
                "          \"default_operator\" : \"and\",\n" +
                "          \"analyze_wildcard\" : false,\n" +
                "          \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "          \"fuzzy_prefix_length\" : 0,\n" +
                "          \"fuzzy_max_expansions\" : 50,\n" +
                "          \"fuzzy_transpositions\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", qb.toString());
    }

    @Test
    public void testNxqlPredicateWithHint() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProviderDefinition ppdef = pps.getPageProviderDefinition("NXQL_WITH_HINT");
        Assert.assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pps.getPageProvider("NXQL_WITH_HINT", ppdef,
                model, null, pageSize, 0L, props);
        Assert.assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        String esquery = pp.getCurrentQueryAsEsBuilder().toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"dc:title.fulltext\" : {\n" +
                "                \"value\" : \"you know\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"fuzzy\" : {\n" +
                "          \"my_field\" : {\n" +
                "            \"value\" : \"for search\",\n" +
                "            \"fuzziness\" : \"AUTO\",\n" +
                "            \"prefix_length\" : 0,\n" +
                "            \"max_expansions\" : 50,\n" +
                "            \"transpositions\" : false,\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"my_subject\" : [\n" +
                "                \"foo\",\n" +
                "                \"bar\"\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", esquery);
    }

    @Test
    public void testNxqlPredicateWithHintInParameter() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProviderDefinition ppdef = pps.getPageProviderDefinition("NXQL_WITH_HINT_IN_PARAMETER");
        Assert.assertNotNull(ppdef);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 5;
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        model.setProperty("advanced_search", "fulltext_all", "you know");
        model.setProperty("advanced_search", "description", "for search");
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pps.getPageProvider("NXQL_WITH_HINT", ppdef,
                model, null, pageSize, 0L, props);
        Assert.assertNotNull(pp);
        pp.getCurrentPage(); // This is needed to build the nxql query
        String esquery = pp.getCurrentQueryAsEsBuilder().toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"dc:title.fulltext\" : {\n" +
                "                \"value\" : \"you know\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"fuzzy\" : {\n" +
                "          \"my_field\" : {\n" +
                "            \"value\" : \"for search\",\n" +
                "            \"fuzziness\" : \"AUTO\",\n" +
                "            \"prefix_length\" : 0,\n" +
                "            \"max_expansions\" : 50,\n" +
                "            \"transpositions\" : false,\n" +
                "            \"boost\" : 1.0\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"my_subject\" : [\n" +
                "                \"foo\",\n" +
                "                \"bar\"\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", esquery);
    }

    @Test
    public void testMaxResultWindow() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        PageProviderDefinition ppdef = pps.getPageProviderDefinition("NXQL_PP_PATTERN");
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 2;
        ElasticSearchNxqlPageProvider pp = (ElasticSearchNxqlPageProvider) pps.getPageProvider("NXQL_PP_PATTERN", ppdef,
                null, null, pageSize, 0L, props);
        pp.setMaxResultWindow(6);
        Assert.assertEquals(6, pp.getMaxResultWindow());
        // create 10 docs
        startTransaction();
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // get current page
        List<DocumentModel> p = pp.getCurrentPage();
        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertEquals(5, pp.getNumberOfPages());
        Assert.assertTrue(pp.isNextPageAvailable());
        // last page is not accessible
        Assert.assertFalse(pp.isLastPageAvailable());
        // only 3 pages are navigable
        Assert.assertEquals(3, pp.getPageLimit());
        // page 2
        pp.nextPage();
        Assert.assertTrue(pp.isNextPageAvailable());
        // page 3 reach the max result window of 6 docs
        pp.nextPage();
        Assert.assertFalse(pp.isNextPageAvailable());
        Assert.assertFalse(pp.isLastPageAvailable());
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // make tests pass under Windows
            expected = expected.trim();
            expected = expected.replace("\n", "");
            expected = expected.replace("\r", "");
            actual = actual.trim();
            actual = actual.replace("\n", "");
            actual = actual.replace("\r", "");
        }
        Assert.assertEquals(expected, actual);
    }

}
