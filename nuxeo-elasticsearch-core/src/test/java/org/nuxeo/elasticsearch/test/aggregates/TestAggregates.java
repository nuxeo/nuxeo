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
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.test.aggregates;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.FieldDescriptor;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
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
        "org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml" })
public class TestAggregates {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

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
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esa.refresh();

        TransactionHelper.startTransaction();

    }

    @Test
    public void testAggregateQuery() throws Exception {

        AggregateDefinition aggDef1 = new AggregateDescriptor();
        aggDef1.setType("terms");
        aggDef1.setId("source");
        aggDef1.setDocumentField("dc:source");
        aggDef1.setSearchField(new FieldDescriptor("advanced_search",
                "sourge_agg"));

        AggregateDefinition aggDef2 = new AggregateDescriptor();
        aggDef2.setType("terms");
        aggDef2.setId("nature");
        aggDef2.setDocumentField("dc:nature");
        aggDef2.setSearchField(new FieldDescriptor("advanced_search",
                "nature_agg"));
        aggDef2.setProperty("size", "10");

        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        NxQueryBuilder qb = new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document")
                .addAggregate(new AggregateQuery(aggDef1, model))
                .addAggregate(new AggregateQuery(aggDef2, model));

        SearchRequestBuilder request = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\n"
                        + "  \"from\" : 0,\n"
                        + "  \"size\" : 10,\n"
                        + "  \"query\" : {\n"
                        + "    \"match_all\" : { }\n"
                        + "  },\n"
                        + "  \"post_filter\" : {\n"
                        + "    \"match_all\" : { }\n"
                        + "  },\n"
                        + "  \"aggregations\" : {\n"
                        + "    \"source\" : {\n"
                        + "      \"filter\" : {\n"
                        + "        \"match_all\" : { }\n"
                        + "      },\n"
                        + "      \"aggregations\" : {\n"
                        + "        \"source\" : {\n"
                        + "          \"terms\" : {\n"
                        + "            \"field\" : \"dc:source\"\n"
                        + "          }\n"
                        + "        }\n"
                        + "      }\n"
                        + "    },\n"
                        + "    \"nature\" : {\n"
                        + "      \"filter\" : {\n"
                        + "        \"match_all\" : { }\n"
                        + "      },\n"
                        + "      \"aggregations\" : {\n"
                        + "        \"nature\" : {\n"
                        + "          \"terms\" : {\n"
                        + "            \"field\" : \"dc:nature\",\n"
                        + "            \"size\" : 10\n"
                        + "          }\n"
                        + "        }\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                request.toString());
    }

    @Test
    public void testFoo() throws Exception {
        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps
                .getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, null,
                null, null, (long) 0, null);

        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        pp.setSearchDocumentModel(model);

        AggregateQuery[] aggs = pp.getAggregatesQuery();

        NxQueryBuilder qb = new NxQueryBuilder(session);
        qb.addAggregates(aggs);

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
