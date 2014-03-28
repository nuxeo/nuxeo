/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test.nxql;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.nxql.NXQLQueryConverter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Test that NXQL can be used to generate ES queries
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({"org.nuxeo.elasticsearch.core:elasticsearch-config-test-contrib.xml"})
public class TestNXQLConversion {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    protected void buildDocs() throws Exception {
        for (int i = 0; i < 10; i++) {
            String name = "doc" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i%2);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esi.flush();

        TransactionHelper.startTransaction();

    }

    @Test
    public void testQuery() throws Exception {

        buildDocs();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.queryString(" dc\\:nature:\"Nature1\" AND dc\\:title:\"File1\"")).setFrom(
                0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.queryString(" dc\\:nature:\"Nature2\" AND dc\\:title:\"File1\"")).setFrom(
                0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.queryString(" NOT dc\\:nature:\"Nature2\"")).setFrom(
                0).setSize(60).execute().actionGet();
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

        checkNXQL(
                "select * from Document where dc:nature='Nature2' and dc:title='File2'",
                1);
        checkNXQL(
                "select * from Document where dc:nature='Nature2' and dc:title='File1'",
                0);
        checkNXQL(
                "select * from Document where dc:nature='Nature2' or dc:title='File1'",
                2);

    }

    protected void checkNXQL(String nxql, int expectedNumberOfHis)
            throws Exception {
        //System.out.println(NXQLQueryConverter.toESQueryString(nxql));
        DocumentModelList docs = ess.query(session, nxql, 10, 0);
        Assert.assertEquals(expectedNumberOfHis, docs.size());
    }

    @Test
    public void testConverterSelect() throws Exception {
        String es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document").toString();
        Assert.assertEquals("{\n" +
                "  \"match_all\" : { }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from File, Document").toString();
        Assert.assertEquals("{\n" +
                "  \"match_all\" : { }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from File").toString();
        Assert.assertEquals("{\n" +
                "  \"filtered\" : {\n" +
                "    \"query\" : {\n" +
                "      \"match_all\" : { }\n" +
                "    },\n" +
                "    \"filter\" : {\n" +
                "      \"terms\" : {\n" +
                "        \"ecm:primaryType\" : [ \"File\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from File, Note").toString();
        Assert.assertEquals("{\n" +
                "  \"filtered\" : {\n" +
                "    \"query\" : {\n" +
                "      \"match_all\" : { }\n" +
                "    },\n" +
                "    \"filter\" : {\n" +
                "      \"terms\" : {\n" +
                "        \"ecm:primaryType\" : [ \"File\", \"Note\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterExpression() throws Exception {
        String es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1=1").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"f1\" : \"1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 IN (1, '2', 3)").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"terms\" : {\n" +
                "        \"f1\" : [ \"1\", \"2\", \"3\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 LIKE 'foo%'").toString();
        Assert.assertEquals("{\n" +
                "  \"regexp\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"value\" : \"foo%\"\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        String old = es;
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 ILIKE 'foo%'").toString();
        Assert.assertEquals(old, es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 NOT LIKE 'foo%'").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"not\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"query\" : {\n" +
                "            \"regexp\" : {\n" +
                "              \"f1\" : {\n" +
                "                \"value\" : \"foo%\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 IS NULL").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"missing\" : {\n" +
                "        \"field\" : \"f1\",\n" +
                "        \"null_value\" : true\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 IS NOT NULL").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"exists\" : {\n" +
                "        \"field\" : \"f1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 BETWEEN 1 AND 2").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"range\" : {\n" +
                "        \"f1\" : {\n" +
                "          \"from\" : \"1\",\n" +
                "          \"to\" : \"2\",\n" +
                "          \"include_lower\" : true,\n" +
                "          \"include_upper\" : true\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1 NOT BETWEEN 1 AND 2").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"not\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"range\" : {\n" +
                "            \"f1\" : {\n" +
                "              \"from\" : \"1\",\n" +
                "              \"to\" : \"2\",\n" +
                "              \"include_lower\" : true,\n" +
                "              \"include_upper\" : true\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:path STARTSWITH '/the/path'").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"ecm:path.children\" : \"/the/path\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);

    }

    @Test
    public void testConverterWhereCombination() throws Exception {
            String  es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1=1 AND f2=2").toString();
        Assert.assertEquals("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [ {\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"f1\" : \"1\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"f2\" : \"2\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}", es);
        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1=1 OR f2=2").toString();
        Assert.assertEquals("{\n" +
                "  \"bool\" : {\n" +
                "    \"should\" : [ {\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"f1\" : \"1\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"f2\" : \"2\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}", es);

        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1=1 AND f2=2 AND f3=3").toString();
        //Assert.assertEquals("foo", es);

        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1=1 OR f2=2 OR f3=3").toString();
        // Assert.assertEquals("foo", es);

        es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1=1 OR f2 LIKE 'foo' OR f3=3").toString();
        //Assert.assertEquals("foo", es);

        es = NXQLQueryConverter
                .toESQueryBuilder(
                        "select * from Document where (f1=1 OR f2=2) AND f3=3")
                .toString();
        Assert.assertEquals("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [ {\n" +
                "      \"bool\" : {\n" +
                "        \"should\" : [ {\n" +
                "          \"constant_score\" : {\n" +
                "            \"filter\" : {\n" +
                "              \"term\" : {\n" +
                "                \"f1\" : \"1\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"constant_score\" : {\n" +
                "            \"filter\" : {\n" +
                "              \"term\" : {\n" +
                "                \"f2\" : \"2\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"f3\" : \"3\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterComplex() throws Exception {
        String es = NXQLQueryConverter
                .toESQueryBuilder(
                        "select * from Document where (f1 LIKE '1%' OR f2 LIKE '2%') AND f3=3")
                .toString();
        Assert.assertEquals("{\n" +
                "  \"bool\" : {\n" +
                "    \"must\" : [ {\n" +
                "      \"bool\" : {\n" +
                "        \"should\" : [ {\n" +
                "          \"regexp\" : {\n" +
                "            \"f1\" : {\n" +
                "              \"value\" : \"1%\"\n" +
                "            }\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"regexp\" : {\n" +
                "            \"f2\" : {\n" +
                "              \"value\" : \"2%\"\n" +
                "            }\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"term\" : {\n" +
                "            \"f3\" : \"3\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}", es);
        //Assert.assertEquals("foo", es);
        es = NXQLQueryConverter
                .toESQueryBuilder(
                        "select * from Document where ecm:fulltext='foo bar' AND ecm:path STARTSWITH '/foo/bar' OR ecm:path='/foo/'")
               .toString();
        //Assert.assertEquals("foo", es);

        es = NXQLQueryConverter
                .toESQueryBuilder(
                        "select * from File, Note, Workspace where f1 IN ('foo', 'bar', 'foo') AND NOT f2>=3")
               .toString();
        Assert.assertEquals("{\n" +
                "  \"filtered\" : {\n" +
                "    \"query\" : {\n" +
                "      \"bool\" : {\n" +
                "        \"must\" : [ {\n" +
                "          \"constant_score\" : {\n" +
                "            \"filter\" : {\n" +
                "              \"terms\" : {\n" +
                "                \"f1\" : [ \"foo\", \"bar\", \"foo\" ]\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }, {\n" +
                "          \"bool\" : {\n" +
                "            \"must_not\" : {\n" +
                "              \"constant_score\" : {\n" +
                "                \"filter\" : {\n" +
                "                  \"range\" : {\n" +
                "                    \"f2\" : {\n" +
                "                      \"from\" : \"3\",\n" +
                "                      \"to\" : null,\n" +
                "                      \"include_lower\" : true,\n" +
                "                      \"include_upper\" : true\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"filter\" : {\n" +
                "      \"terms\" : {\n" +
                "        \"ecm:primaryType\" : [ \"File\", \"Note\", \"Workspace\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
    }

    @Test
    public void testConverterIsVersion() throws Exception {
        String es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:isVersion = 1").toString();
        Assert.assertEquals("{\n" +
                "  \"constant_score\" : {\n" +
                "    \"filter\" : {\n" +
                "      \"term\" : {\n" +
                "        \"ecm:isVersion\" : \"1\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
        String es2 = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:isCheckedInVersion = 1").toString();
        Assert.assertEquals(es, es2);
    }

    @Test
    public void testConverterFulltext() throws Exception {
        // Given a search on a fulltext field
        String es = NXQLQueryConverter.toESQueryBuilder(
                "select * from Document where f1='foo bar'", Arrays.asList(new String[] { "f1", "f2" })).toString();
        // then we have a query and not a filter
        Assert.assertEquals("{\n" +
                "  \"match\" : {\n" +
                "    \"f1\" : {\n" +
                "      \"query\" : \"foo bar\",\n" +
                "      \"type\" : \"boolean\",\n" +
                "      \"operator\" : \"AND\"\n" +
                "    }\n" +
                "  }\n" +
                "}", es);
    }
}
