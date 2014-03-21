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

package org.nuxeo.elasticsearch.test;

import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
import org.nuxeo.elasticsearch.provider.ElasticSearchQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.core:pageprovider-test-contrib.xml",
        "org.nuxeo.elasticsearch.core:schemas-test-contrib.xml",
        "org.nuxeo.elasticsearch.core:elasticsearch-config-test-contrib.xml"})
public class TestPageProvider {

    @Inject
    protected CoreSession session;

    @Inject
    ElasticSearchIndexing esi;

    @Test
    public void ICanUseThePageProvider() throws Exception {
        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps
                .getPageProviderDefinition("NATIVE_ES_PP_1");
        Assert.assertNotNull(ppdef);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        long pageSize = 5;
        PageProvider<?> pp = pps.getPageProvider("NATIVE_ES_PP_1", ppdef, null,
                null, pageSize, Long.valueOf(0), props);
        Assert.assertNotNull(pp);

        // create 10 docs
        ElasticSearchService ess = Framework
                .getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();

        ElasticSearchAdmin esa = Framework
                .getLocalService(ElasticSearchAdmin.class);
        Assert.assertNotNull(esa);
        Assert.assertTrue(esa.getPendingDocs() > 0);
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();
        esi.flush();

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
    public void testBuildInQuery() throws Exception {
        Principal principal = null;
        ElasticSearchService ess = Framework
                .getLocalService(ElasticSearchService.class);
        SearchRequestBuilder qb = new SearchRequestBuilder(ess.getClient());
        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "TEST_IN").getWhereClause();
        DocumentModel model = new DocumentModelImpl("/", "doc", "File");
        model.setPropertyValue("dc:subjects", new String[] { "foo", "bar" });
        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, null);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : { }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"dc:title\" : [ \"foo\", \"bar\" ]\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", qb.toString());

        model.setPropertyValue("dc:subjects", new String[] { "foo" });
        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, null);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : { }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"dc:title\" : [ \"foo\" ]\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", qb.toString());

        // criteria with no values are removed
        model.setPropertyValue("dc:subjects", new String[] {});
        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, null);

        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : { }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : { }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", qb.toString());
    }

    @Test
    public void testBuildInIntegersQuery() throws Exception {
        Principal principal = null;
        ElasticSearchService ess = Framework
                .getLocalService(ElasticSearchService.class);
        SearchRequestBuilder qb = new SearchRequestBuilder(ess.getClient());
        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "TEST_IN_INTEGERS").getWhereClause();
        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        @SuppressWarnings("boxing")
        Integer[] array1 = new Integer[] { 1, 2, 3 };
        model.setPropertyValue("search:integerlist", array1);
        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, null);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : { }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"size\" : [ 1, 2, 3 ]\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", qb.toString());

        // lists work too
        @SuppressWarnings("boxing")
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        model.setPropertyValue("search:integerlist", (Serializable) list);
        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, null);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : { }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : {\n" +
                "            \"terms\" : {\n" +
                "              \"size\" : [ 1, 2, 3 ]\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}", qb.toString());


    }


    @Test
    public void testBuildIsNullQuery() throws Exception {
        Principal principal = null;
        ElasticSearchService ess = Framework
                .getLocalService(ElasticSearchService.class);
        SearchRequestBuilder qb = new SearchRequestBuilder(ess.getClient());
        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "ADVANCED_SEARCH").getWhereClause();
        SortInfo sortInfos = new SortInfo("dc:title", true);
        String[] params = { "foo" };
        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        model.setPropertyValue("search:title", "bar");

        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, params,
                sortInfos);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : [ {\n" +
                "            \"query_string\" : {\n" +
                "              \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"regexp\" : {\n" +
                "              \"dc:title\" : {\n" +
                "                \"value\" : \"bar\"\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : { }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"dc:title\" : {\n" +
                "      \"order\" : \"asc\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}", qb.toString());

        model.setPropertyValue("search:isPresent", Boolean.TRUE);

        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, params,
                sortInfos);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : [ {\n" +
                "            \"query_string\" : {\n" +
                "              \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"regexp\" : {\n" +
                "              \"dc:title\" : {\n" +
                "                \"value\" : \"bar\"\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must_not\" : {\n" +
                "            \"exists\" : {\n" +
                "              \"field\" : \"dc:modified\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"dc:title\" : {\n" +
                "      \"order\" : \"asc\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}",
                qb.toString());

        // only boolean available in schema without default value
        model.setPropertyValue("search:isPresent", Boolean.FALSE);
        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, model, whereClause, params,
                sortInfos);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"query\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : [ {\n" +
                "            \"query_string\" : {\n" +
                "              \"query\" : \"ecm\\\\:parentId: \\\"foo\\\"\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"regexp\" : {\n" +
                "              \"dc:title\" : {\n" +
                "                \"value\" : \"bar\"\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      },\n" +
                "      \"filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must_not\" : {\n" +
                "            \"exists\" : {\n" +
                "              \"field\" : \"dc:modified\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"dc:title\" : {\n" +
                "      \"order\" : \"asc\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}",
                qb.toString());

        qb = new SearchRequestBuilder(ess.getClient());
        ElasticSearchQueryBuilder.makeQuery(qb, principal, "SELECT * FROM ? WHERE ? = '?'",
                new Object[] { "Document", "dc:title", null }, false, true);
        Assert.assertEquals("{\n" +
                "  \"query\" : {\n" +
                "    \"query_string\" : {\n" +
                "      \"query\" : \"SELECT * FROM \\\"Document\\\" WHERE \\\"dc:title\\\" = ''\"\n" +
                "    }\n" +
                "  }\n" +
                "}",
                qb.toString());
    }


}
