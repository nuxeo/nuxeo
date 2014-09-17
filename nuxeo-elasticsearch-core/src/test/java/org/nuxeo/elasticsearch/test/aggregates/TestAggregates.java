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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateQueryImpl;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDescriptor;
import org.nuxeo.ecm.platform.query.core.FieldDescriptor;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
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
            doc.setPropertyValue("dc:source", "Source" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i % 2);
            doc.setPropertyValue("dc:coverage", "Coverage" + i % 3);
            doc.setPropertyValue("common:size", 1024*i);
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
    public void testAggregateTermsQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("source");
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search",
                "source_agg"));
        aggDef.setProperty("minDocCount", "10");
        aggDef.setProperty("size", "10");
        aggDef.setProperty("exclude", "foo*");
        aggDef.setProperty("include", "bar*");
        aggDef.setProperty("order", "count asc");

        NxQueryBuilder qb = new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document").addAggregate(
                new AggregateQueryImpl(aggDef, null));

        SearchRequestBuilder request = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME);
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\n" //
                        + "  \"from\" : 0,\n" //
                        + "  \"size\" : 10,\n" //
                        + "  \"query\" : {\n" //
                        + "    \"match_all\" : { }\n" //
                        + "  },\n" //
                        + "  \"aggregations\" : {\n" //
                        + "    \"source_filter\" : {\n" //
                        + "      \"filter\" : {\n" //
                        + "        \"match_all\" : { }\n" //
                        + "      },\n" //
                        + "      \"aggregations\" : {\n" //
                        + "        \"source\" : {\n" //
                        + "          \"terms\" : {\n" //
                        + "            \"field\" : \"dc:source\",\n" //
                        + "            \"size\" : 10,\n" //
                        + "            \"min_doc_count\" : 10,\n" //
                        + "            \"order\" : {\n" //
                        + "              \"_count\" : \"asc\"\n" //
                        + "            },\n" //
                        + "            \"include\" : \"bar*\",\n" //
                        + "            \"exclude\" : \"foo*\"\n" //
                        + "          }\n" //
                        + "        }\n" //
                        + "      }\n" //
                        + "    }\n" //
                        + "  }\n" //
                        + "}", //
                request.toString());
    }

    @Test
    public void testAggregateSignificantTermsQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("significant_terms");
        aggDef.setId("source");
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search",
                "source_agg"));
        aggDef.setProperty("minDocCount", "10");

        NxQueryBuilder qb = new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document").addAggregate(
                new AggregateQueryImpl(aggDef, null));

        SearchRequestBuilder request = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME);
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\n" //
                + "  \"from\" : 0,\n" //
                + "  \"size\" : 10,\n" //
                + "  \"query\" : {\n" //
                + "    \"match_all\" : { }\n" //
                + "  },\n" //
                + "  \"aggregations\" : {\n" //
                + "    \"source_filter\" : {\n" //
                + "      \"filter\" : {\n" //
                + "        \"match_all\" : { }\n" //
                + "      },\n" //
                + "      \"aggregations\" : {\n" //
                + "        \"source\" : {\n" //
                + "          \"significant_terms\" : {\n" //
                + "            \"field\" : \"dc:source\",\n" //
                + "            \"minDocCount\" : 10\n" //
                + "          }\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }\n" //
                + "}", //
                request.toString());
    }

    @Test
    public void testAggregateRangeQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("range");
        aggDef.setId("source");
        aggDef.setDocumentField("common:size");
        aggDef.setSearchField(new FieldDescriptor("advanced_search",
                "size_agg"));
        List<AggregateRangeDescriptor> ranges = new ArrayList<AggregateRangeDescriptor>();
        ranges.add(new AggregateRangeDescriptor("small", null, 2048.0));
        ranges.add(new AggregateRangeDescriptor("medium", 2048.0, 6144.0));
        ranges.add(new AggregateRangeDescriptor("big", 6144.0, null));
        aggDef.setRanges((List<AggregateRangeDefinition>) (List<?>) ranges);

        NxQueryBuilder qb = new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document").addAggregate(
                new AggregateQueryImpl(aggDef, null));

        SearchRequestBuilder request = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME);
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\n" //
                        + "  \"from\" : 0,\n" //
                        + "  \"size\" : 10,\n" //
                        + "  \"query\" : {\n" //
                        + "    \"match_all\" : { }\n" //
                        + "  },\n" //
                        + "  \"aggregations\" : {\n" //
                        + "    \"source_filter\" : {\n" //
                        + "      \"filter\" : {\n" //
                        + "        \"match_all\" : { }\n" //
                        + "      },\n" //
                        + "      \"aggregations\" : {\n" //
                        + "        \"source\" : {\n" //
                        + "          \"range\" : {\n" //
                        + "            \"field\" : \"common:size\",\n" //
                        + "            \"ranges\" : [ {\n" //
                        + "              \"key\" : \"small\",\n" //
                        + "              \"to\" : 2048.0\n" //
                        + "            }, {\n" //
                        + "              \"key\" : \"medium\",\n" //
                        + "              \"from\" : 2048.0,\n" //
                        + "              \"to\" : 6144.0\n" //
                        + "            }, {\n" //
                        + "              \"key\" : \"big\",\n" //
                        + "              \"from\" : 6144.0\n" //
                        + "            } ]\n" //
                        + "          }\n" //
                        + "        }\n" //
                        + "      }\n" //
                        + "    }\n" //
                        + "  }\n" //
                        + "}", //
                request.toString());
    }

    @Test
    public void testAggregateQuery() throws Exception {

        AggregateDefinition aggDef1 = new AggregateDescriptor();
        aggDef1.setType("terms");
        aggDef1.setId("source");
        aggDef1.setDocumentField("dc:source");
        aggDef1.setSearchField(new FieldDescriptor("advanced_search",
                "source_agg"));

        AggregateDefinition aggDef2 = new AggregateDescriptor();
        aggDef2.setType("terms");
        aggDef2.setId("nature");
        aggDef2.setDocumentField("dc:nature");
        aggDef2.setSearchField(new FieldDescriptor("advanced_search",
                "nature_agg"));
        aggDef2.setProperty("size", "10");

        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        String[] sources = { "foo", "bar" };
        model.setProperty("advanced_search", "source_agg", sources);
        // String[] natures = { "foobar" };
        // model.setProperty("advanced_search", "nature_agg", natures);
        NxQueryBuilder qb = new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document")
                .addAggregate(new AggregateQueryImpl(aggDef1, model))
                .addAggregate(new AggregateQueryImpl(aggDef2, model));

        SearchRequestBuilder request = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME);
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\n" + "  \"from\" : 0,\n" //
                        + "  \"size\" : 10,\n" //
                        + "  \"query\" : {\n" //
                        + "    \"match_all\" : { }\n" //
                        + "  },\n" //
                        + "  \"post_filter\" : {\n" //
                        + "    \"and\" : {\n" //
                        + "      \"filters\" : [ {\n" //
                        + "        \"terms\" : {\n" //
                        + "          \"dc:source\" : [ \"foo\", \"bar\" ]\n" //
                        + "        }\n" //
                        + "      } ]\n" //
                        + "    }\n" //
                        + "  },\n" //
                        + "  \"aggregations\" : {\n" //
                        + "    \"source_filter\" : {\n" //
                        + "      \"filter\" : {\n" //
                        + "        \"match_all\" : { }\n" //
                        + "      },\n" //
                        + "      \"aggregations\" : {\n" //
                        + "        \"source\" : {\n" //
                        + "          \"terms\" : {\n" //
                        + "            \"field\" : \"dc:source\"\n" //
                        + "          }\n" //
                        + "        }\n" //
                        + "      }\n" //
                        + "    },\n" //
                        + "    \"nature_filter\" : {\n" //
                        + "      \"filter\" : {\n" //
                        + "        \"and\" : {\n" //
                        + "          \"filters\" : [ {\n" //
                        + "            \"terms\" : {\n" //
                        + "              \"dc:source\" : [ \"foo\", \"bar\" ]\n" //
                        + "            }\n" //
                        + "          } ]\n" //
                        + "        }\n" //
                        + "      },\n" //
                        + "      \"aggregations\" : {\n" //
                        + "        \"nature\" : {\n" //
                        + "          \"terms\" : {\n" //
                        + "            \"field\" : \"dc:nature\",\n" //
                        + "            \"size\" : 10\n" //
                        + "          }\n" //
                        + "        }\n" //
                        + "      }\n" //
                        + "    }\n" //
                        + "  }\n" //
                        + "}", //
                request.toString());
    }

    @Test
    public void testPageProvider() throws Exception {
        buildDocs();

        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps
                .getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);

        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model,
                null, null, (long) 0, props);

        Assert.assertEquals(4,  pp.getAggregates().size());
        Assert.assertEquals(
                "AggregateImpl(source, terms, [BucketTerm(Source0, 1), BucketTerm(Source1, 1), BucketTerm(Source2, 1), BucketTerm(Source3, 1), BucketTerm(Source4, 1)])",
                pp.getAggregates().get("source").toString());
        Assert.assertEquals(
                "AggregateImpl(coverage, terms, [BucketTerm(Coverage1, 1), BucketTerm(Coverage2, 1)])",
                pp.getAggregates().get("coverage").toString());
        Assert.assertEquals("AggregateImpl(nature, terms, [BucketTerm(Nature0, 1), BucketTerm(Nature1, 1)])",
                pp.getAggregates().get("nature").toString());
        Assert.assertEquals("AggregateImpl(size, range, [BucketRange(small, 1, -Infinity, 2048,00), BucketRange(medium, 1, 2048,00, 6144,00), BucketRange(big, 0, 6144,00, Infinity)])",
                pp.getAggregates().get("size").toString());

    }

    @Test
    public void testPageProviderRange() throws Exception {
        buildDocs();

        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps
                .getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);

        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        String[] sizes = { "big", "medium" };
        model.setProperty("advanced_search", "size_agg", sizes);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model,
                null, null, (long) 0, props);

        Assert.assertEquals(4,  pp.getAggregates().size());
        Assert.assertEquals("AggregateImpl(source, terms, [BucketTerm(Source2, 1), BucketTerm(Source3, 1), BucketTerm(Source4, 1), BucketTerm(Source5, 1), BucketTerm(Source6, 1)])",
                pp.getAggregates().get("source").toString());
        Assert.assertEquals(
                "AggregateImpl(coverage, terms, [BucketTerm(Coverage0, 3), BucketTerm(Coverage2, 3), BucketTerm(Coverage1, 2)])",
                pp.getAggregates().get("coverage").toString());
        Assert.assertEquals("AggregateImpl(nature, terms, [BucketTerm(Nature0, 4), BucketTerm(Nature1, 4)])",
                pp.getAggregates().get("nature").toString());
        Assert.assertEquals("AggregateImpl(size, range, [BucketRange(small, 2, -Infinity, 2048,00), BucketRange(medium, 4, 2048,00, 6144,00), BucketRange(big, 4, 6144,00, Infinity)])",
                pp.getAggregates().get("size").toString());

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
