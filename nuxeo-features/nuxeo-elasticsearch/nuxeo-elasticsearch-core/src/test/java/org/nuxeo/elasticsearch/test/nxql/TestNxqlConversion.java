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

package org.nuxeo.elasticsearch.test.nxql;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test that NXQL can be used to generate ES queries
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features({RepositoryElasticSearchFeature.class})
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestNxqlConversion {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    protected void buildDocs() throws Exception {
        for (int i = 0; i < 10; i++) {
            String name = "doc" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        // wait for async jobs
        WorkManager wm = Framework.getService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingWorkerCount());

        esa.refresh();

        TransactionHelper.startTransaction();

    }

    protected SearchResponse searchAll() {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    protected SearchResponse search(QueryBuilder query) {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .source(new SearchSourceBuilder().from(0).size(60));
        request.source(new SearchSourceBuilder().query(query));
        return esa.getClient().search(request);
    }

    @Test
    public void testQuery() throws Exception {

        buildDocs();

        SearchResponse searchResponse = search(QueryBuilders.queryStringQuery(
                " dc\\:nature:\"Nature1\" AND dc\\:title:\"File1\""));
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = search(QueryBuilders.queryStringQuery(" dc\\:nature:\"Nature2\" AND dc\\:title:\"File1\""));
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = search(QueryBuilders.queryStringQuery(" NOT " + "dc\\:nature:\"Nature2\""));
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

        checkNXQL("select * from Document where dc:nature='Nature2' and dc:title='File2'", 1);
        checkNXQL("select * from Document where dc:nature='Nature2' and dc:title='File1'", 0);
        checkNXQL("select * from Document where dc:nature='Nature2' or dc:title='File1'", 2);
    }

    @Test
    public void testQueryLimits() throws Exception {
        buildDocs();

        // limit does not change the total size, only the returned number of docs
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(1));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(1, docs.size());
        // default is 10
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(10, docs.size());
        // only interested about totalSize
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(0));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(0, docs.size());
    }

    @Test
    public void testQueryWithSpecialCharacters() {
        // special character should not raise syntax error
        String specialChars = "^..*+ - && || ! ( ) { } [ ] )^ \" (~ * ? : \\ / \\t$";
        checkNXQL("select * from Document where dc:title = '" + specialChars + "'", 0);
        checkNXQL("select * from Document where ecm:fulltext.dc:title = '" + specialChars + "'", 0);
        checkNXQL("select * from Document where dc:title LIKE '" + specialChars + "'", 0);
        checkNXQL("select * from Document where dc:title IN ('" + specialChars + "')", 0);
        checkNXQL("select * from Document where dc:title STARTSWITH '" + specialChars + "'", 0);
    }

    protected void checkNXQL(String nxql, int expectedNumberOfHis) {
        // System.out.println(NXQLQueryConverter.toESQueryString(nxql));
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(nxql).limit(0));
        Assert.assertEquals(expectedNumberOfHis, docs.totalSize());
    }

    @Test
    public void testConverterSelect() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File, Document").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"match_all\" : {\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"terms\" : {\n" +
                "          \"ecm:primaryType\" : [\n" +
                "            \"File\"\n" +
                "          ],\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File, Note").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"match_all\" : {\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"terms\" : {\n" +
                "          \"ecm:primaryType\" : [\n" +
                "            \"File\",\n" +
                "            \"Note\"\n" +
                "          ],\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterEQUALS() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"f1\" : {\n" +
                "          \"value\" : \"1\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 != 1").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"value\" : \"1\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 <> 1").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"value\" : \"1\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

    }

    @Test
    public void testConverterIN() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 IN (1)").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"terms\" : {\n" +
                "        \"f1\" : [\n" +
                "          \"1\"\n" +
                "        ],\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT IN (1, '2', 3)").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"terms\" : {\n" +
                "              \"f1\" : [\n" +
                "                \"1\",\n" +
                "                \"2\",\n" +
                "                \"3\"\n" +
                "              ],\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterLIKE() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 LIKE 'foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_phrase_prefix\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"query\" : \"foo\",\n" +
                "      \"slop\" : 0,\n" +
                "      \"max_expansions\" : 50,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 LIKE '%Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"wildcard\" : \"*Foo*\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT LIKE 'Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"match_phrase_prefix\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"query\" : \"Foo\",\n" +
                "                \"slop\" : 0,\n" +
                "                \"max_expansions\" : 50,\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        // invalid input
        NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 LIKE '(foo.*$#@^'").toString();
    }

    @Test
    public void testConverterLIKEWildcard() {
        String es;
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '%foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"wildcard\" : \"*foo\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '_foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"wildcard\" : \"?foo\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '?foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"wildcard\" : \"\\\\?foo\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        // * is also accepted as a wildcard (compat)
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '*foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"wildcard\" : \"*foo\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        // NXQL escaping
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE 'foo\\_bar\\%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"wildcard\" : \"foo_bar%\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterILIKE() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 ILIKE 'Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_phrase_prefix\" : {\n" +
                "    \"f1.lowercase\" : {\n" +
                "      \"query\" : \"foo\",\n" +
                "      \"slop\" : 0,\n" +
                "      \"max_expansions\" : 50,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 ILIKE '%Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"f1.lowercase\" : {\n" +
                "      \"wildcard\" : \"*foo*\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT ILIKE 'Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"match_phrase_prefix\" : {\n" +
                "              \"f1.lowercase\" : {\n" +
                "                \"query\" : \"foo\",\n" +
                "                \"slop\" : 0,\n" +
                "                \"max_expansions\" : 50,\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterIsNULL() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 IS NULL").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"exists\" : {\n" +
                "              \"field\" : \"f1\",\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 IS NOT NULL").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"exists\" : {\n" +
                "        \"field\" : \"f1\",\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterBETWEEN() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 BETWEEN 1 AND 2").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"range\" : {\n" +
                "        \"f1\" : {\n" +
                "          \"from\" : \"1\",\n" +
                "          \"to\" : \"2\",\n" +
                "          \"include_lower\" : true,\n" +
                "          \"include_upper\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT BETWEEN 1 AND 2").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"range\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"from\" : \"1\",\n" +
                "                \"to\" : \"2\",\n" +
                "                \"include_lower\" : true,\n" +
                "                \"include_upper\" : true,\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterSTARTSWITH() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:path STARTSWITH '/the/path'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"ecm:path.children\" : {\n" +
                "                \"value\" : \"/the/path\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"ecm:path\" : {\n" +
                "                \"value\" : \"/the/path\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:path STARTSWITH '/'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"exists\" : {\n" +
                "        \"field\" : \"ecm:path.children\",\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:path STARTSWITH '/the/path/'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"ecm:path.children\" : {\n" +
                "                \"value\" : \"/the/path\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"ecm:path\" : {\n" +
                "                \"value\" : \"/the/path/\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        // for other field than ecm:path we want to match the root
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:coverage STARTSWITH 'Europe/France'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"dc:coverage.children\" : {\n" +
                "          \"value\" : \"Europe/France\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterAncestorId() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:ancestorId = 'c5904f77-299a-411e-8477-81d3102a81f9'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"exists\" : {\n" +
                "        \"field\" : \"ancestorid-without-session\",\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:ancestorId != 'c5904f77-299a-411e-8477-81d3102a81f9'", session)
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"exists\" : {\n" +
                "              \"field\" : \"ancestorid-not-found\",\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +

                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterIsVersion() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:isVersion = 1").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"ecm:isVersion\" : {\n" +
                "          \"value\" : \"true\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        String es2 = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:isCheckedInVersion = 1")
                .toString();
        assertEqualsEvenUnderWindows(es, es2);
    }

    @Test
    public void testConverterFulltext() {
        // Given a search on a fulltext field
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:fulltext='+foo -bar'")
                .toString();
        // then we have a simple query text and not a filter
        assertEqualsEvenUnderWindows("{\n" +
                "  \"simple_query_string\" : {\n" +
                "    \"query\" : \"+foo -bar\",\n" +
                "    \"fields\" : [\n" +
                "      \"all_field^1.0\"\n" +
                "    ],\n" +
                "    \"analyzer\" : \"fulltext\",\n" +
                "    \"flags\" : -1,\n" +
                "    \"default_operator\" : \"and\",\n" +
                "    \"analyze_wildcard\" : false,\n" +
                "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "    \"fuzzy_prefix_length\" : 0,\n" +
                "    \"fuzzy_max_expansions\" : 50,\n" +
                "    \"fuzzy_transpositions\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:fulltext_someindex LIKE '+foo -bar'")
                .toString();
        // don't handle nxql fulltext index definition, match to _all field
        assertEqualsEvenUnderWindows("{\n" +
                "  \"simple_query_string\" : {\n" +
                "    \"query\" : \"+foo -bar\",\n" +
                "    \"fields\" : [\n" +
                "      \"all_field^1.0\"\n" +
                "    ],\n" +
                "    \"analyzer\" : \"fulltext\",\n" +
                "    \"flags\" : -1,\n" +
                "    \"default_operator\" : \"and\",\n" +
                "    \"analyze_wildcard\" : false,\n" +
                "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "    \"fuzzy_prefix_length\" : 0,\n" +
                "    \"fuzzy_max_expansions\" : 50,\n" +
                "    \"fuzzy_transpositions\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:fulltext.dc:title!='+foo -bar'")
                .toString();
        // request on field match field.fulltext
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"simple_query_string\" : {\n" +
                "              \"query\" : \"+foo -bar\",\n" +
                "              \"fields\" : [\n" +
                "                \"dc:title.fulltext^1.0\"\n" +
                "              ],\n" +
                "              \"analyzer\" : \"fulltext\",\n" +
                "              \"flags\" : -1,\n" +
                "              \"default_operator\" : \"and\",\n" +
                "              \"analyze_wildcard\" : false,\n" +
                "              \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "              \"fuzzy_prefix_length\" : 0,\n" +
                "              \"fuzzy_max_expansions\" : 50,\n" +
                "              \"fuzzy_transpositions\" : true,\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterFulltextElasticsearchPrefix() {
        // Given a search on a fulltext field with the
        // elasticsearch-specific prefix
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE ecm:fulltext = 'es: foo bar'")
                .toString();
        // then we have a simple query text and not a filter
        // and we have the OR operator
        assertEqualsEvenUnderWindows("{\n" +
                "  \"simple_query_string\" : {\n" +
                "    \"query\" : \"foo bar\",\n" +
                "    \"fields\" : [\n" +
                "      \"all_field^1.0\"\n" +
                "    ],\n" +
                "    \"analyzer\" : \"fulltext\",\n" +
                "    \"flags\" : -1,\n" +
                "    \"default_operator\" : \"or\",\n" +
                "    \"analyze_wildcard\" : false,\n" +
                "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "    \"fuzzy_prefix_length\" : 0,\n" +
                "    \"fuzzy_max_expansions\" : 50,\n" +
                "    \"fuzzy_transpositions\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-property-override.xml")
    public void testConverterIsTrashedWithProperty() {
        String sqlNotTrashed = "SELECT * FROM Document WHERE ecm:isTrashed = 0";
        String sqlTrashed = "SELECT * FROM Document WHERE ecm:isTrashed = 1";

        String es = NxqlQueryConverter.toESQueryBuilder(sqlTrashed).toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"constant_score\" : {\n" //
                + "    \"filter\" : {\n" //
                + "      \"term\" : {\n" //
                + "        \"ecm:isTrashed\" : {\n" //
                + "          \"value\" : true,\n" //
                + "          \"boost\" : 1.0\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    \"boost\" : 1.0\n" //
                + "  }\n" //
                + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(sqlNotTrashed).toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"constant_score\" : {\n" //
                + "    \"filter\" : {\n" //
                + "      \"bool\" : {\n" //
                + "        \"must_not\" : [\n" //
                + "          {\n" //
                + "            \"term\" : {\n" //
                + "              \"ecm:isTrashed\" : {\n" //
                + "                \"value\" : true,\n" //
                + "                \"boost\" : 1.0\n" //
                + "              }\n" //
                + "            }\n" //
                + "          }\n" //
                + "        ],\n" //
                + "        \"adjust_pure_negative\" : true,\n" //
                + "        \"boost\" : 1.0\n" //
                + "      }\n" //
                + "    },\n" //
                + "    \"boost\" : 1.0\n" //
                + "  }\n" //
                + "}", es);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml")
    public void testConverterIsTrashedWithLifeCycle() {
        String sqlNotTrashed = "SELECT * FROM Document WHERE ecm:isTrashed = 0";
        String sqlTrashed = "SELECT * FROM Document WHERE ecm:isTrashed = 1";
        doTestTrashedWithLifeCycle(sqlNotTrashed, sqlTrashed);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml") // for consistency
    public void testConverterLifeCycleStateDeleted() {
        String sqlNotTrashed = "SELECT * FROM Document WHERE ecm:currentLifeCycleState <> 'deleted'";
        String sqlTrashed = "SELECT * FROM Document WHERE ecm:currentLifeCycleState = 'deleted'";
        doTestTrashedWithLifeCycle(sqlNotTrashed, sqlTrashed);
    }

    protected void doTestTrashedWithLifeCycle(String sqlNotTrashed, String sqlTrashed) {
        String es = NxqlQueryConverter.toESQueryBuilder(sqlTrashed).toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"constant_score\" : {\n" //
                + "    \"filter\" : {\n" //
                + "      \"term\" : {\n" //
                + "        \"ecm:currentLifeCycleState\" : {\n" //
                + "          \"value\" : \"deleted\",\n" //
                + "          \"boost\" : 1.0\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    \"boost\" : 1.0\n" //
                + "  }\n" //
                + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(sqlNotTrashed).toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"constant_score\" : {\n" //
                + "    \"filter\" : {\n" //
                + "      \"bool\" : {\n" //
                + "        \"must_not\" : [\n" //
                + "          {\n" //
                + "            \"term\" : {\n" //
                + "              \"ecm:currentLifeCycleState\" : {\n" //
                + "                \"value\" : \"deleted\",\n" //
                + "                \"boost\" : 1.0\n" //
                + "              }\n" //
                + "            }\n" //
                + "          }\n" //
                + "        ],\n" //
                + "        \"adjust_pure_negative\" : true,\n" //
                + "        \"boost\" : 1.0\n" //
                + "      }\n" //
                + "    },\n" //
                + "    \"boost\" : 1.0\n" //
                + "  }\n" //
                + "}", es);
    }

    @Test
    public void testConverterWhereCombination() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 AND f2=2").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"value\" : \"1\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"f2\" : {\n" +
                "                \"value\" : \"2\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 OR f2=2").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"should\" : [\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"value\" : \"1\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"f2\" : {\n" +
                "                \"value\" : \"2\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 AND f2=2 AND f3=3").toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 OR f2=2 OR f3=3").toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 OR f2 LIKE 'foo' OR f3=3")
                .toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where (f1=1 OR f2=2) AND f3=3").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"constant_score\" : {\n" +
                "                \"filter\" : {\n" +
                "                  \"term\" : {\n" +
                "                    \"f1\" : {\n" +
                "                      \"value\" : \"1\",\n" +
                "                      \"boost\" : 1.0\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"constant_score\" : {\n" +
                "                \"filter\" : {\n" +
                "                  \"term\" : {\n" +
                "                    \"f2\" : {\n" +
                "                      \"value\" : \"2\",\n" +
                "                      \"boost\" : 1.0\n" +
                "                    }\n" +
                "                  }\n" +
                "                },\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"f3\" : {\n" +
                "                \"value\" : \"3\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterComplex() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where (f1 LIKE '1%' OR f2 LIKE '2%') AND f3=3").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [\n" +
                "            {\n" +
                "              \"match_phrase_prefix\" : {\n" +
                "                \"f1\" : {\n" +
                "                  \"query\" : \"1\",\n" +
                "                  \"slop\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"match_phrase_prefix\" : {\n" +
                "                \"f2\" : {\n" +
                "                  \"query\" : \"2\",\n" +
                "                  \"slop\" : 0,\n" +
                "                  \"max_expansions\" : 50,\n" +
                "                  \"boost\" : 1.0\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"constant_score\" : {\n" +
                "          \"filter\" : {\n" +
                "            \"term\" : {\n" +
                "              \"f3\" : {\n" +
                "                \"value\" : \"3\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        // Assert.assertEquals("foo", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:fulltext='foo bar' AND ecm:path STARTSWITH '/foo/bar' OR ecm:path='/foo/'")
                .toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from File, Note, Workspace where f1 IN ('foo', 'bar', 'foo') AND NOT f2>=3").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : [\n" +
                "            {\n" +
                "              \"constant_score\" : {\n" +
                "                \"filter\" : {\n" +
                "                  \"terms\" : {\n" +
                "                    \"f1\" : [\n" +
                "                      \"foo\",\n" +
                "                      \"bar\",\n" +
                "                      \"foo\"\n" +
                "                    ],\n" +
                "                    \"boost\" : 1.0\n" +
                "                  }\n" +
                "                },\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"bool\" : {\n" +
                "                \"must_not\" : [\n" +
                "                  {\n" +
                "                    \"constant_score\" : {\n" +
                "                      \"filter\" : {\n" +
                "                        \"range\" : {\n" +
                "                          \"f2\" : {\n" +
                "                            \"from\" : \"3\",\n" +
                "                            \"to\" : null,\n" +
                "                            \"include_lower\" : true,\n" +
                "                            \"include_upper\" : true,\n" +
                "                            \"boost\" : 1.0\n" +
                "                          }\n" +
                "                        }\n" +
                "                      },\n" +
                "                      \"boost\" : 1.0\n" +
                "                    }\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"adjust_pure_negative\" : true,\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"adjust_pure_negative\" : true,\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"terms\" : {\n" +
                "          \"ecm:primaryType\" : [\n" +
                "            \"File\",\n" +
                "            \"Note\",\n" +
                "            \"Workspace\"\n" +
                "          ],\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterWhereWithoutSelect() {
        String es = NxqlQueryConverter.toESQueryBuilder("f1=1").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"f1\" : {\n" +
                "          \"value\" : \"1\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder(null).toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_all\" : {\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConvertComplexProperties() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where file:content/name = 'foo'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"file:content.name\" : {\n" +
                "          \"value\" : \"foo\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConvertComplexListProperties() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:subjects/* = 'foo'")
                .toString();
        // this is supported and match any element of the list
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"dc:subjects\" : {\n" +
                "          \"value\" : \"foo\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where files:files/*/file/length=123")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"files:files.file.length\" : {\n" +
                "          \"value\" : \"123\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

    }

    @Test
    public void testConvertComplexListPropertiesUnsupported() {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:subjects/3 = 'foo'")
                .toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"dc:subjects.3\" : {\n" +
                "          \"value\" : \"foo\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:subjects/*1 = 'foo'").toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"dc:subjects1\" : {\n" +
                "          \"value\" : \"foo\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where files:files/*1/file/length=123")
                .toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"files:files1.file.length\" : {\n" +
                "          \"value\" : \"123\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

    }

    @Test
    public void testOrderByFromNxql() {
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("name='foo' ORDER BY name DESC");
        String es = qb.makeQuery().toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"name\" : {\n" +
                "          \"value\" : \"foo\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        Assert.assertEquals(1, qb.getSortInfos().size());
        Assert.assertEquals("SortInfo [sortColumn=name, sortAscending=false]", qb.getSortInfos().get(0).toString());
    }

    @Test
    public void testOrderByWithComplexProperties() {
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM File ORDER BY file:content/name DESC");
        String es = qb.makeQuery().toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [\n" +
                "      {\n" +
                "        \"match_all\" : {\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"filter\" : [\n" +
                "      {\n" +
                "        \"terms\" : {\n" +
                "          \"ecm:primaryType\" : [\n" +
                "            \"File\"\n" +
                "          ],\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"adjust_pure_negative\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
        Assert.assertEquals(1, qb.getSortInfos().size());
        Assert.assertEquals("SortInfo [sortColumn=file:content.name, sortAscending=false]",
                qb.getSortInfos().get(0).toString());
    }

    @Test
    public void testConvertHint() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(some:field) */ dc:title = 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"some:field\" : {\n" +
                "          \"value\" : \"foo\",\n" +
                "          \"boost\" : 1.0\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(some:field) */ dc:title != 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must_not\" : [\n" +
                "          {\n" +
                "            \"term\" : {\n" +
                "              \"some:field\" : {\n" +
                "                \"value\" : \"foo\",\n" +
                "                \"boost\" : 1.0\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ],\n" +
                "        \"adjust_pure_negative\" : true,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConvertHintOperator() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(some:field) ANALYZER(my_analyzer) OPERATOR(match) */ dc:subjects = 'foo'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match\" : {\n" +
                "    \"some:field\" : {\n" +
                "      \"query\" : \"foo\",\n" +
                "      \"operator\" : \"OR\",\n" +
                "      \"analyzer\" : \"my_analyzer\",\n" +
                "      \"prefix_length\" : 0,\n" +
                "      \"max_expansions\" : 50,\n" +
                "      \"fuzzy_transpositions\" : true,\n" +
                "      \"lenient\" : false,\n" +
                "      \"zero_terms_query\" : \"NONE\",\n" +
                "      \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(match_phrase) */ dc:title = 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_phrase\" : {\n" +
                "    \"dc:title\" : {\n" +
                "      \"query\" : \"foo\",\n" +
                "      \"slop\" : 0,\n" +
                "      \"zero_terms_query\" : \"NONE\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(match_phrase_prefix) */ dc:title = 'this is a test'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_phrase_prefix\" : {\n" +
                "    \"dc:title\" : {\n" +
                "      \"query\" : \"this is a test\",\n" +
                "      \"slop\" : 0,\n" +
                "      \"max_expansions\" : 50,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(dc:title^3,dc:description) OPERATOR(multi_match) */ dc:title = 'this is a test'")
                .toString();
        // fields are not ordered
        assertIn(es,
                "{\n" +
                        "  \"multi_match\" : {\n" +
                        "    \"query\" : \"this is a test\",\n" +
                        "    \"fields\" : [\n" +
                        "      \"dc:description^1.0\",\n" +
                        "      \"dc:title^3.0\"\n" +
                        "    ],\n" +
                        "    \"type\" : \"best_fields\",\n" +
                        "    \"operator\" : \"OR\",\n" +
                        "    \"slop\" : 0,\n" +
                        "    \"prefix_length\" : 0,\n" +
                        "    \"max_expansions\" : 50,\n" +
                        "    \"zero_terms_query\" : \"NONE\",\n" +
                        "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                        "    \"fuzzy_transpositions\" : true,\n" +
                        "    \"boost\" : 1.0\n" +
                        "  }\n" +
                        "}",
                "{\n" + //
                        "  \"multi_match\" : {\n" + //
                        "    \"query\" : \"this is a test\",\n" + //
                        "    \"fields\" : [ \"dc:description\", \"dc:title^3\" ]\n" + //
                        "  }\n" + //
                        "}");

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(regex) */ dc:title = 's.*y'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"regexp\" : {\n" +
                "    \"dc:title\" : {\n" +
                "      \"value\" : \"s.*y\",\n" +
                "      \"flags_value\" : 65535,\n" +
                "      \"max_determinized_states\" : 10000,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(fuzzy) */ dc:title = 'ki'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"fuzzy\" : {\n" +
                "    \"dc:title\" : {\n" +
                "      \"value\" : \"ki\",\n" +
                "      \"fuzziness\" : \"AUTO\",\n" +
                "      \"prefix_length\" : 0,\n" +
                "      \"max_expansions\" : 50,\n" +
                "      \"transpositions\" : false,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(wildcard) */ dc:title = 'ki*y'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"dc:title\" : {\n" +
                "      \"wildcard\" : \"ki*y\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(simple_query_string) */ dc:title = '\"fried eggs\" +(eggplant | potato) -frittata'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"simple_query_string\" : {\n" +
                "    \"query\" : \"\\\"fried eggs\\\" +(eggplant | potato) -frittata\",\n" +
                "    \"fields\" : [\n" +
                "      \"dc:title^1.0\"\n" +
                "    ],\n" +
                "    \"flags\" : -1,\n" +
                "    \"default_operator\" : \"or\",\n" +
                "    \"analyze_wildcard\" : false,\n" +
                "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                "    \"fuzzy_prefix_length\" : 0,\n" +
                "    \"fuzzy_max_expansions\" : 50,\n" +
                "    \"fuzzy_transpositions\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(dc:title,dc:description) ANALYZER(fulltext) OPERATOR(query_string) */ dc:title = 'this AND that OR thus'")
                .toString();
        // fields are not ordered
        assertEqualsEvenUnderWindows("{\n" +
                "  \"query_string\" : {\n" +
                "    \"query\" : \"this AND that OR thus\",\n" +
                "    \"fields\" : [\n" +
                "      \"dc:description^1.0\",\n" +
                "      \"dc:title^1.0\"\n" +
                "    ],\n" +
                "    \"type\" : \"best_fields\",\n" +
                "    \"default_operator\" : \"or\",\n" +
                "    \"analyzer\" : \"fulltext\",\n" +
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
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(common) */ dc:title = 'this is bonsai cool'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"common\" : {\n" +
                "    \"dc:title\" : {\n" +
                "      \"query\" : \"this is bonsai cool\",\n" +
                "      \"high_freq_operator\" : \"OR\",\n" +
                "      \"low_freq_operator\" : \"OR\",\n" +
                "      \"cutoff_frequency\" : 0.01,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(dc:title.fulltext^2,dc:description.fulltext) OPERATOR(more_like_this) */ ecm:uuid = '1234'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"more_like_this\" : {\n" +
                "    \"fields\" : [\n" +
                "      \"dc:title.fulltext\",\n" +
                "      \"dc:description.fulltext\"\n" +
                "    ],\n" +
                "    \"like\" : [\n" +
                "      {\n" +
                "        \"_index\" : \"nxutest\",\n" +
                "        \"_type\" : \"doc\",\n" +
                "        \"_id\" : \"1234\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"max_query_terms\" : 12,\n" +
                "    \"min_term_freq\" : 1,\n" +
                "    \"min_doc_freq\" : 3,\n" +
                "    \"max_doc_freq\" : 2147483647,\n" +
                "    \"min_word_length\" : 0,\n" +
                "    \"max_word_length\" : 0,\n" +
                "    \"minimum_should_match\" : \"30%\",\n" +
                "    \"boost_terms\" : 0.0,\n" +
                "    \"include\" : false,\n" +
                "    \"fail_on_unsupported_field\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(all_field) OPERATOR(more_like_this) */ ecm:uuid IN ('1234', '4567')").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"more_like_this\" : {\n" +
                "    \"fields\" : [\n" +
                "      \"all_field\"\n" +
                "    ],\n" +
                "    \"like\" : [\n" +
                "      {\n" +
                "        \"_index\" : \"nxutest\",\n" +
                "        \"_type\" : \"doc\",\n" +
                "        \"_id\" : \"'1234', '4567'\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"max_query_terms\" : 12,\n" +
                "    \"min_term_freq\" : 1,\n" +
                "    \"min_doc_freq\" : 3,\n" +
                "    \"max_doc_freq\" : 2147483647,\n" +
                "    \"min_word_length\" : 0,\n" +
                "    \"max_word_length\" : 0,\n" +
                "    \"minimum_should_match\" : \"30%\",\n" +
                "    \"boost_terms\" : 0.0,\n" +
                "    \"include\" : false,\n" +
                "    \"fail_on_unsupported_field\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(more_like_this) */ ecm:uuid IN ('1234', '4567')").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"more_like_this\" : {\n" +
                "    \"fields\" : [\n" +
                "      \"ecm:uuid\"\n" +
                "    ],\n" +
                "    \"like\" : [\n" +
                "      {\n" +
                "        \"_index\" : \"nxutest\",\n" +
                "        \"_type\" : \"doc\",\n" +
                "        \"_id\" : \"'1234', '4567'\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"max_query_terms\" : 12,\n" +
                "    \"min_term_freq\" : 1,\n" +
                "    \"min_doc_freq\" : 3,\n" +
                "    \"max_doc_freq\" : 2147483647,\n" +
                "    \"min_word_length\" : 0,\n" +
                "    \"max_word_length\" : 0,\n" +
                "    \"minimum_should_match\" : \"30%\",\n" +
                "    \"boost_terms\" : 0.0,\n" +
                "    \"include\" : false,\n" +
                "    \"fail_on_unsupported_field\" : true,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

    }

    @Test
    public void testConvertHintLike() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(some:field) ANALYZER(my_analyzer) */ dc:subjects LIKE 'foo*'")
                .toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match_phrase_prefix\" : {\n" +
                "    \"some:field\" : {\n" +
                "      \"query\" : \"foo\",\n" +
                "      \"analyzer\" : \"my_analyzer\",\n" +
                "      \"slop\" : 0,\n" +
                "      \"max_expansions\" : 50,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(some:field) */ dc:subjects LIKE '%foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"wildcard\" : {\n" +
                "    \"some:field\" : {\n" +
                "      \"wildcard\" : \"*foo*\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

    }

    @Test
    public void testConvertHintFulltext() {
        // search on title and description, boost title
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: INDEX(dc:title.fulltext^4,dc:description.fulltext) */ ecm:fulltext = 'foo'")
                .toString();
        // fields are not ordered
        assertIn(es,"{\n" +
                        "  \"simple_query_string\" : {\n" +
                        "    \"query\" : \"foo\",\n" +
                        "    \"fields\" : [\n" +
                        "      \"dc:description.fulltext^1.0\",\n" +
                        "      \"dc:title.fulltext^4.0\"\n" +
                        "    ],\n" +
                        "    \"analyzer\" : \"fulltext\",\n" +
                        "    \"flags\" : -1,\n" +
                        "    \"default_operator\" : \"and\",\n" +
                        "    \"analyze_wildcard\" : false,\n" +
                        "    \"auto_generate_synonyms_phrase_query\" : true,\n" +
                        "    \"fuzzy_prefix_length\" : 0,\n" +
                        "    \"fuzzy_max_expansions\" : 50,\n" +
                        "    \"fuzzy_transpositions\" : true,\n" +
                        "    \"boost\" : 1.0\n" +
                        "  }\n" +
                        "}",
                "{\n" + //
                        "  \"simple_query_string\" : {\n" + //
                        "    \"query\" : \"foo\",\n" + //
                        "    \"fields\" : [ \"dc:description.fulltext\", \"dc:title.fulltext^3\" ],\n" + //
                        "    \"analyzer\" : \"fulltext\",\n" + //
                        "    \"default_operator\" : \"and\"\n" + //
                        "  }\n" + //
                        "}");
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        expected = normalizeString(expected);
        actual = normalizeString(actual);
        Assert.assertEquals(expected, actual);
    }

    private String normalizeString(String str) {
        if (SystemUtils.IS_OS_WINDOWS) {
            str = str.trim();
            str = str.replace("\n", "");
            str = str.replace("\r", "");
        }
        return str;
    }

    protected void assertIn(String actual, String... expected) {
        actual = normalizeString(actual);
        for (String exp : expected) {
            exp = normalizeString(exp);
            if (exp.equals(actual)) {
                return;
            }
        }
        // fail
        Assert.assertEquals(expected[0], actual);
    }

    @Test
    public void testConvertHintGeo() {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(geo_bounding_box) */ osm:location IN ('40.73, -74.1', '40.81, -71.12')")
                .toString();
        String response = "{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"geo_bounding_box\" : {\n" +
                "        \"osm:location\" : {\n" +
                "          \"top_left\" : [\n" +
                "            -74.1,\n" +
                "            40.81\n" +
                "          ],\n" +
                "          \"bottom_right\" : [\n" +
                "            -71.12,\n" +
                "            40.73\n" +
                "          ]\n" +
                "        },\n" +
                "        \"validation_method\" : \"STRICT\",\n" +
                "        \"type\" : \"MEMORY\",\n" +
                "        \"ignore_unmapped\" : false,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}";
        assertEqualsEvenUnderWindows(response, es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where /*+ES: OPERATOR(geo_bounding_box) */ osm:location IN ('drj7tee', 'dr5r9y')")
                .toString();
        // we cannot do this because lat and lon are not rounded to match the input
        // assertTruEqualsEvenUnderWindows(response, es);
        Assert.assertTrue(es.contains("geo_bounding_box"));
        Assert.assertTrue(es, es.contains("bottom_right"));

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(geo_distance) */ "
                + "osm:location IN ('40.73, -74.1', '20km')").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"geo_distance\" : {\n" +
                "        \"osm:location\" : [\n" +
                "          -74.1,\n" +
                "          40.73\n" +
                "        ],\n" +
                "        \"distance\" : 20000.0,\n" +
                "        \"distance_type\" : \"arc\",\n" +
                "        \"validation_method\" : \"STRICT\",\n" +
                "        \"ignore_unmapped\" : false,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(geo_shape) */"
                + "osm:location IN ('FRA', 'countries', 'shapes', 'location')").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"geo_shape\" : {\n" +
                "        \"osm:location\" : {\n" +
                "          \"indexed_shape\" : {\n" +
                "            \"id\" : \"FRA\",\n" +
                "            \"type\" : \"countries\",\n" +
                "            \"index\" : \"shapes\",\n" +
                "            \"path\" : \"location\"\n" +
                "          },\n" +
                "          \"relation\" : \"within\"\n" +
                "        },\n" +
                "        \"ignore_unmapped\" : false,\n" +
                "        \"boost\" : 1.0\n" +
                "      }\n" +
                "    },\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}", es);

    }
}
