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
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.test.aggregates;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDescriptor;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;
import org.nuxeo.ecm.platform.query.core.FieldDescriptor;
import org.nuxeo.elasticsearch.aggregate.AggregateFactory;
import org.nuxeo.elasticsearch.aggregate.DateHistogramAggregate;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:schemas-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestAggregates {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    protected void buildDocs() throws Exception {
        DateTime yesterdayNoon = new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1).plusHours(12);
        for (int i = 0; i < 10; i++) {
            String name = "doc" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:source", "Source" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i % 2);
            doc.setPropertyValue("dc:coverage", "Coverage" + i % 3);
            doc.setPropertyValue("file:content", new DummyLengthBlob(1024 * i));
            doc.setPropertyValue("dc:created", new Date(yesterdayNoon.minusWeeks(i).getMillis()));
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        // wait for async jobs
        WorkManager wm = Framework.getService(WorkManager.class);
        assertTrue(wm.awaitCompletion(60, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        esa.refresh();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testAggregateTermsQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("source");
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");
        aggDef.setProperty("size", "10");
        aggDef.setProperty("exclude", "foo*");
        aggDef.setProperty("include", "bar*");
        aggDef.setProperty("order", "count asc");
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));

        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows(
                "{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"source_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"source\":{\"terms\":{\"field\":\"dc:source\",\"size\":10,\"min_doc_count\":10,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"asc\"},{\"_key\":\"asc\"}],\"include\":\"bar*\",\"exclude\":\"foo*\"}}}}}}", //
                request.toString());
    }

    @Test
    public void testAggregateTermsFulltextQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("fulltext");
        aggDef.setDocumentField("ecm:fulltext");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "fulltext_agg"));
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));
        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);
        assertEqualsEvenUnderWindows(
                "{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"fulltext_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"fulltext\":{\"terms\":{\"field\":\"all_field\",\"size\":10,\"min_doc_count\":1,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateSignificantTermsQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("significant_terms");
        aggDef.setId("source");
        aggDef.setDocumentField("dc:source");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");

        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));

        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"source_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"source\":{\"significant_terms\":{\"field\":\"dc:source\",\"size\":10,\"min_doc_count\":10,\"shard_min_doc_count\":0,\"jlh\":{}}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateRangeQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("range");
        aggDef.setId("source");
        aggDef.setDocumentField("common:size");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "size_agg"));
        List<AggregateRangeDefinition> ranges = new ArrayList<>();
        ranges.add(new AggregateRangeDescriptor("small", null, 2048.0));
        ranges.add(new AggregateRangeDescriptor("medium", 2048.0, 6144.0));
        ranges.add(new AggregateRangeDescriptor("big", 6144.0, null));
        aggDef.setRanges(ranges);
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));

        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows(
                "{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"source_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"source\":{\"range\":{\"field\":\"common:size\",\"ranges\":[{\"key\":\"small\",\"to\":2048.0},{\"key\":\"medium\",\"from\":2048.0,\"to\":6144.0},{\"key\":\"big\",\"from\":6144.0}],\"keyed\":false}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateRangeDateQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("date_range");
        aggDef.setId("created");
        aggDef.setDocumentField("dc:created");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "created_agg"));
        List<AggregateRangeDateDefinition> ranges = new ArrayList<>();
        ranges.add(new AggregateRangeDateDescriptor("10monthAgo", null, "now-10M/M"));
        ranges.add(new AggregateRangeDateDescriptor("1monthAgo", "now-10M/M", "now-1M/M"));
        ranges.add(new AggregateRangeDateDescriptor("thisMonth", "now-1M/M", null));
        aggDef.setDateRanges(ranges);
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));

        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"created_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"created\":{\"date_range\":{\"field\":\"dc:created\",\"ranges\":[{\"key\":\"10monthAgo\",\"to\":\"now-10M/M\"},{\"key\":\"1monthAgo\",\"from\":\"now-10M/M\",\"to\":\"now-1M/M\"},{\"key\":\"thisMonth\",\"from\":\"now-1M/M\"}],\"keyed\":false}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateHistogramQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("histogram");
        aggDef.setId("size");
        aggDef.setDocumentField("common:size");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "size_agg"));
        aggDef.setProperty("interval", "1024");
        aggDef.setProperty("extendedBoundsMin", "0");
        aggDef.setProperty("extendedBoundsMax", "10240");
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));
        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);
        assertEqualsEvenUnderWindows("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"size_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"size\":{\"histogram\":{\"field\":\"common:size\",\"interval\":1024.0,\"offset\":0.0,\"order\":{\"_key\":\"asc\"},\"keyed\":false,\"min_doc_count\":0,\"extended_bounds\":{\"min\":0.0,\"max\":10240.0}}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateDateHistogramQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("date_histogram");
        aggDef.setId("created");
        aggDef.setDocumentField("dc:created");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "created_agg"));
        aggDef.setProperty("interval", "month");
        aggDef.setProperty("format", "yyy-MM");
        aggDef.setProperty("timeZone", "UTC");
        aggDef.setProperty("order", "count desc");
        aggDef.setProperty("minDocCounts", "5");
        DateHistogramAggregate agg = (DateHistogramAggregate) AggregateFactory.create(aggDef, null);
        agg.setSelection(Collections.singletonList("2016-08"));
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document").addAggregate(agg);
        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"post_filter\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"range\":{\"dc:created\":{\"from\":1470009600000,\"to\":1472688000000,\"include_lower\":true,\"include_upper\":false,\"format\":\"epoch_millis\",\"boost\":1.0}}}],\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"created_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"created\":{\"date_histogram\":{\"field\":\"dc:created\",\"format\":\"yyy-MM\",\"time_zone\":\"UTC\",\"interval\":\"month\",\"offset\":0,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}],\"keyed\":false,\"min_doc_count\":0}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateDateHistogramQueryWithoutTimeZone() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("date_histogram");
        aggDef.setId("created");
        aggDef.setDocumentField("dc:created");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "created_agg"));
        aggDef.setProperty("interval", "month");
        aggDef.setProperty("order", "count desc");
        aggDef.setProperty("minDocCounts", "5");
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));
        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);
        // The request will use the JVM time_zone
        assertTrue(request.toString(), request.toString().startsWith("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"created_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"created\":{\"date_histogram\":{\"field\":\"dc:created\",\"time_zone\":"));
        assertTrue(request.toString(), request.toString().endsWith("\"interval\":\"month\",\"offset\":0,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}],\"keyed\":false,\"min_doc_count\":0}}}}}}"));
    }

    @Test
    public void testAggregateMultiAggregatesQuery() throws Exception {

        AggregateDefinition aggDef1 = new AggregateDescriptor();
        aggDef1.setType("terms");
        aggDef1.setId("source");
        aggDef1.setDocumentField("dc:source");
        aggDef1.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));

        AggregateDefinition aggDef2 = new AggregateDescriptor();
        aggDef2.setType("terms");
        aggDef2.setId("nature");
        aggDef2.setDocumentField("dc:nature");
        aggDef2.setSearchField(new FieldDescriptor("advanced_search", "nature_agg"));
        aggDef2.setProperty("size", "10");

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sources = { "foo", "bar" };
        model.setProperty("advanced_search", "source_agg", sources);
        // String[] natures = { "foobar" };
        // model.setProperty("advanced_search", "nature_agg", natures);
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef1, model))
                                                       .addAggregate(AggregateFactory.create(aggDef2, model));

        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"post_filter\":{\"bool\":{\"must\":[{\"terms\":{\"dc:source\":[\"foo\",\"bar\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"source_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"source\":{\"terms\":{\"field\":\"dc:source\",\"size\":10,\"min_doc_count\":1,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}},\"nature_filter\":{\"filter\":{\"bool\":{\"must\":[{\"terms\":{\"dc:source\":[\"foo\",\"bar\"],\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}},\"aggregations\":{\"nature\":{\"terms\":{\"field\":\"dc:nature\",\"size\":10,\"min_doc_count\":1,\"shard_min_doc_count\":0,\"show_term_doc_count_error\":false,\"order\":[{\"_count\":\"desc\"},{\"_key\":\"asc\"}]}}}}}}",
                request.toString());
    }

    @Test
    public void testAggregateOnComplexTypeQuery() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("significant_terms");
        aggDef.setId("source");
        aggDef.setDocumentField("prefix:foo/bar");
        aggDef.setSearchField(new FieldDescriptor("advanced_search", "source_agg"));
        aggDef.setProperty("minDocCount", "10");

        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .addAggregate(AggregateFactory.create(aggDef, null));

        SearchSourceBuilder request = new SearchSourceBuilder();
        qb.updateRequest(request);

        assertEqualsEvenUnderWindows("{\"from\":0,\"size\":10,\"query\":{\"match_all\":{\"boost\":1.0}},\"_source\":{\"includes\":[\"_id\"],\"excludes\":[]},\"aggregations\":{\"source_filter\":{\"filter\":{\"match_all\":{\"boost\":1.0}},\"aggregations\":{\"source\":{\"significant_terms\":{\"field\":\"prefix:foo.bar\",\"size\":10,\"min_doc_count\":10,\"shard_min_doc_count\":0,\"jlh\":{}}}}}}}",
                request.toString());
    }

    @Test
    public void testPageProvider() throws Exception {
        buildDocs();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sources = { "Source1", "Source2" };
        model.setProperty("advanced_search", "source_agg", sources);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        Assert.assertEquals(7, pp.getAggregates().size());
        Assert.assertEquals(2, pp.getResultsCount());
        Assert.assertEquals(
                "Aggregate(source, terms, dc:source, [Source1, Source2], [BucketTerm(Source0, 1), BucketTerm(Source1, 1), BucketTerm(Source2, 1), BucketTerm(Source3, 1), BucketTerm(Source4, 1)])",
                pp.getAggregates().get("source").toString());
        Assert.assertEquals(
                "Aggregate(coverage, terms, dc:coverage, [], [BucketTerm(Coverage1, 1), BucketTerm(Coverage2, 1)])",
                pp.getAggregates().get("coverage").toString());
        Assert.assertEquals("Aggregate(nature, terms, dc:nature, [], [BucketTerm(Nature0, 1), BucketTerm(Nature1, 1)])",
                pp.getAggregates().get("nature").toString());
        Assert.assertEquals(
                "Aggregate(size, range, file:content.length, [], [BucketRange(small, 1, -Infinity, 2048.00), BucketRange(medium, 1, 2048.00, 6144.00), BucketRange(big, 0, 6144.00, Infinity)])",
                pp.getAggregates().get("size").toString());
        Assert.assertEquals(
                "Aggregate(size_histo, histogram, file:content.length, [], [BucketRange(1024.0, 1, 1024.00, 2048.00), BucketRange(2048.0, 1, 2048.00, 3072.00)])",
                pp.getAggregates().get("size_histo").toString());
        Assert.assertEquals(3, pp.getAggregates().get("created").getBuckets().size());
        Assert.assertEquals(2, pp.getAggregates().get("created_histo").getBuckets().size());
        // output depends on current date
        // Assert.assertEquals("Aggregate(created, date_range, dc:created, [], [BucketRangeDate(long_time_ago, 0, null,
        // 2014-07-11T14:26:32.590+02:00), BucketRangeDate(some_time_ago, 0, 2014-07-11T14:26:32.590+02:00,
        // 2014-08-29T14:26:32.590+02:00), BucketRangeDate(last_month, 2, 2014-08-29T14:26:32.590+02:00, null)])",
        // pp.getAggregates().get("created").toString());
        // Assert.assertEquals("Aggregate(created_histo, date_histogram, dc:created, [], [BucketRangeDate(31-08-2014, 1,
        // 2014-08-31T23:30:00.000+02:00, 2014-09-07T23:30:00.000+02:00), BucketRangeDate(07-09-2014, 1,
        // 2014-09-07T23:30:00.000+02:00, 2014-09-14T23:30:00.000+02:00)])",
        // pp.getAggregates().get("created_histo").toString());
    }

    @Test
    public void testPageProviderWithRangeSelection() throws Exception {
        buildDocs();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sizes = { "big", "medium" };
        model.setProperty("advanced_search", "size_agg", sizes);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        Assert.assertEquals(7, pp.getAggregates().size());
        Assert.assertEquals(8, pp.getResultsCount());
        Assert.assertEquals(
                "Aggregate(source, terms, dc:source, [], [BucketTerm(Source2, 1), BucketTerm(Source3, 1), BucketTerm(Source4, 1), BucketTerm(Source5, 1), BucketTerm(Source6, 1)])",
                pp.getAggregates().get("source").toString());
        Assert.assertEquals(
                "Aggregate(coverage, terms, dc:coverage, [], [BucketTerm(Coverage0, 3), BucketTerm(Coverage2, 3), BucketTerm(Coverage1, 2)])",
                pp.getAggregates().get("coverage").toString());
        Assert.assertEquals("Aggregate(nature, terms, dc:nature, [], [BucketTerm(Nature0, 4), BucketTerm(Nature1, 4)])",
                pp.getAggregates().get("nature").toString());
        Assert.assertEquals(
                "Aggregate(size, range, file:content.length, [big, medium], [BucketRange(small, 2, -Infinity, 2048.00), BucketRange(medium, 4, 2048.00, 6144.00), BucketRange(big, 4, 6144.00, Infinity)])",
                pp.getAggregates().get("size").toString());

    }

    @Test
    public void testPageProviderWithDateRangeSelection() throws Exception {
        buildDocs();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);

        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] created = { "long_time_ago", "some_time_ago" };
        model.setProperty("advanced_search", "created_agg", created);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        Assert.assertEquals(7, pp.getAggregates().size());
        Assert.assertEquals(7, pp.getResultsCount());
        Assert.assertEquals(
                "Aggregate(coverage, terms, dc:coverage, [], [BucketTerm(Coverage0, 3), BucketTerm(Coverage1, 2), BucketTerm(Coverage2, 2)])",
                pp.getAggregates().get("coverage").toString());
        Assert.assertEquals("Aggregate(nature, terms, dc:nature, [], [BucketTerm(Nature1, 4), BucketTerm(Nature0, 3)])",
                pp.getAggregates().get("nature").toString());
        @SuppressWarnings("unchecked")
        List<BucketRangeDate> buckets = (List<BucketRangeDate>) pp.getAggregates().get("created").getBuckets();
        Assert.assertEquals(3, buckets.size());
        Assert.assertEquals("long_time_ago", buckets.get(0).getKey());
        Assert.assertEquals(0, buckets.get(0).getDocCount());
        Assert.assertEquals(7, buckets.get(1).getDocCount());
        Assert.assertEquals("last_month", buckets.get(2).getKey());
        Assert.assertEquals(3, buckets.get(2).getDocCount());
    }

    @Test
    public void testPageProviderWithHistogramSelection() throws Exception {
        buildDocs();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        String[] sizes = { "1024", "4096" };
        model.setProperty("advanced_search", "size_histo_agg", sizes);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        Assert.assertEquals(7, pp.getAggregates().size());
        Assert.assertEquals(2, pp.getResultsCount());
        Assert.assertEquals(
                "Aggregate(size_histo, histogram, file:content.length, [1024, 4096], [BucketRange(0.0, 1, 0.00, 1024.00), BucketRange(1024.0, 1, 1024.00, 2048.00), BucketRange(2048.0, 1, 2048.00, 3072.00), BucketRange(3072.0, 1, 3072.00, 4096.00), BucketRange(4096.0, 1, 4096.00, 5120.00), BucketRange(5120.0, 1, 5120.00, 6144.00), BucketRange(6144.0, 1, 6144.00, 7168.00), BucketRange(7168.0, 1, 7168.00, 8192.00), BucketRange(8192.0, 1, 8192.00, 9216.00), BucketRange(9216.0, 1, 9216.00, 10240.00)])",
                pp.getAggregates().get("size_histo").toString());
        Assert.assertEquals("Aggregate(source, terms, dc:source, [], [BucketTerm(Source1, 1), BucketTerm(Source4, 1)])",
                pp.getAggregates().get("source").toString());
    }

    @Test
    public void testPageProviderWithDateHistogramSelection() throws Exception {
        buildDocs();

        PageProviderService pps = Framework.getService(PageProviderService.class);
        Assert.assertNotNull(pps);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition("aggregates_1");
        Assert.assertNotNull(ppdef);
        DocumentModel model = session.createDocumentModel("/", "doc", "AdvancedSearch");
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyy");
        DateTime yesterdayNoon = new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1).plusHours(12);
        String[] created = { fmt.print(new DateTime(yesterdayNoon.minusWeeks(3).getMillis())),
                fmt.print(new DateTime(yesterdayNoon.minusWeeks(6).getMillis())) };
        model.setProperty("advanced_search", "created_histo_agg", created);
        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("aggregates_1", ppdef, model, null, null, 0L, props);

        Assert.assertEquals(7, pp.getAggregates().size());
        Assert.assertEquals(2, pp.getResultsCount());
        Assert.assertEquals(
                "Aggregate(size_histo, histogram, file:content.length, [], [BucketRange(3072.0, 1, 3072.00, 4096.00), BucketRange(6144.0, 1, 6144.00, 7168.00)])",
                pp.getAggregates().get("size_histo").toString());
        Assert.assertEquals("Aggregate(source, terms, dc:source, [], [BucketTerm(Source3, 1), BucketTerm(Source6, 1)])",
                pp.getAggregates().get("source").toString());
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

    private static class DummyLengthBlob extends AbstractBlob {

        private final long length;

        public DummyLengthBlob(long length) {
            this.length = length;
        }

        @Override
        public long getLength() {
            return length;
        }

        @Override
        public InputStream getStream() throws IOException {
            return new ByteArrayInputStream(getByteArray());
        }

        @Override
        public byte[] getByteArray() throws IOException {
            return String.valueOf(length).getBytes(getEncoding() == null ? UTF_8 : getEncoding());
        }

        @Override
        public String getString() {
            return String.valueOf(length);
        }

    }

}
