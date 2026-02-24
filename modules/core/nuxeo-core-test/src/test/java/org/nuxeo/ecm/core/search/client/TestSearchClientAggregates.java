/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_AVG;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_CARDINALITY;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_COUNT;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MAX;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_MIN;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_SUM;
import static org.nuxeo.ecm.platform.query.core.AggregateHelper.newAggregate;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.search.IgnoreIfSearchClientDoesNotHaveAggregateCapability;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.ecm.core.search.client.opensearch1.IgnoreIfNotOpenSearchBasedSearchClient;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateAvg;
import org.nuxeo.ecm.platform.query.core.AggregateCardinality;
import org.nuxeo.ecm.platform.query.core.AggregateCount;
import org.nuxeo.ecm.platform.query.core.AggregateDateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateDateRange;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateMax;
import org.nuxeo.ecm.platform.query.core.AggregateMin;
import org.nuxeo.ecm.platform.query.core.AggregateRange;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateRangeDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateSum;
import org.nuxeo.ecm.platform.query.core.AggregateTerm;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;

/**
 * @since 2025.0
 */
@ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveAggregateCapability.class)
public class TestSearchClientAggregates extends AbstractTestSearchClient {

    @Override
    protected String getSourceToIndex() {
        return "OSGI-INF/search/search-client-aggregation-documents.ndjson";
    }

    @Test
    public void testAggregateTerm() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:source");
        AggregateTerm agg = new AggregateTerm(aggDef, null);

        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(3, resultAgg.getBuckets().size());
        assertEquals("foo", resultAgg.getBuckets().get(0).getKey());
        assertEquals(4, resultAgg.getBuckets().get(0).getDocCount());
        assertEquals("bar", resultAgg.getBuckets().get(1).getKey());
        assertEquals(3, resultAgg.getBuckets().get(1).getDocCount());
        assertEquals("baz", resultAgg.getBuckets().get(2).getKey());
        assertEquals(2, resultAgg.getBuckets().get(2).getDocCount());
    }

    @Test
    @ConditionalIgnore(condition = IgnoreIfNotOpenSearchBasedSearchClient.class)
    public void testAggregateTermNumeric() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("common:size");
        AggregateTerm agg = new AggregateTerm(aggDef, null);

        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(6, resultAgg.getBuckets().size());
        assertEquals("100", resultAgg.getBuckets().get(0).getKey());
        assertEquals(2, resultAgg.getBuckets().get(0).getDocCount());
        assertEquals("10000", resultAgg.getBuckets().get(1).getKey());
        assertEquals(2, resultAgg.getBuckets().get(1).getDocCount());
    }

    @Test
    public void testAggregateTermLimit() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:source");
        aggDef.setProperty("size", "1");
        AggregateTerm agg = new AggregateTerm(aggDef, null);

        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(1, resultAgg.getBuckets().size());
        assertEquals("foo", resultAgg.getBuckets().getFirst().getKey());
        assertEquals(4, resultAgg.getBuckets().getFirst().getDocCount());
    }

    @Test
    public void testAggregateTermOrder() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:source");
        aggDef.setProperty("size", "1");
        aggDef.setProperty("order", "term asc");
        AggregateTerm agg = new AggregateTerm(aggDef, null);

        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(1, resultAgg.getBuckets().size());
        assertEquals("bar", resultAgg.getBuckets().getFirst().getKey());
        assertEquals(3, resultAgg.getBuckets().getFirst().getDocCount());
    }

    @Test
    public void testAggregateTermExclude() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("terms");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:source");
        aggDef.setProperty("size", "1");
        aggDef.setProperty("exclude", "foo|bar");
        AggregateTerm agg = new AggregateTerm(aggDef, null);

        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(1, resultAgg.getBuckets().size());
        assertEquals("baz", resultAgg.getBuckets().getFirst().getKey());
        assertEquals(2, resultAgg.getBuckets().getFirst().getDocCount());
    }

    @Test
    public void testAggregateRange() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("range");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("common:size");
        List<AggregateRangeDefinition> ranges = new ArrayList<>();
        ranges.add(new AggregateRangeDescriptor("small", null, 10000.0));
        ranges.add(new AggregateRangeDescriptor("medium", 10000.0, 100000.0));
        ranges.add(new AggregateRangeDescriptor("big", 100000.0, null));
        aggDef.setRanges(ranges);
        AggregateRange agg = new AggregateRange(aggDef, null);

        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(3, resultAgg.getBuckets().size());
        assertEquals("small", resultAgg.getBuckets().get(0).getKey());
        assertEquals(2, resultAgg.getBuckets().get(0).getDocCount());
        assertEquals("medium", resultAgg.getBuckets().get(1).getKey());
        assertEquals(2, resultAgg.getBuckets().get(1).getDocCount());
        assertEquals("big", resultAgg.getBuckets().get(2).getKey());
        assertEquals(6, resultAgg.getBuckets().get(2).getDocCount());
    }

    @Test
    public void testAggregateDateRange() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("date_range");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:modified");
        List<AggregateRangeDateDefinition> ranges = new ArrayList<>();
        ranges.add(new AggregateRangeDateDescriptor("before", null, "2024-04-01"));
        ranges.add(new AggregateRangeDateDescriptor("24Q2", "2024-04-01", "2024-07-01"));
        ranges.add(new AggregateRangeDateDescriptor("24Q3", "2024-07-01", "2024-10-01"));
        ranges.add(new AggregateRangeDateDescriptor("24Q4", "2024-10-01", "2025-01-01"));
        ranges.add(new AggregateRangeDateDescriptor("after", "2025-01-01", null));
        aggDef.setDateRanges(ranges);
        AggregateDateRange agg = new AggregateDateRange(aggDef, null);
        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(5, resultAgg.getBuckets().size());
        assertEquals("before", resultAgg.getBuckets().get(0).getKey());
        assertEquals(3, resultAgg.getBuckets().get(0).getDocCount());
        assertEquals("24Q2", resultAgg.getBuckets().get(1).getKey());
        assertEquals(4, resultAgg.getBuckets().get(1).getDocCount());
        assertEquals("24Q3", resultAgg.getBuckets().get(2).getKey());
        assertEquals(0, resultAgg.getBuckets().get(2).getDocCount());
        assertEquals("24Q4", resultAgg.getBuckets().get(3).getKey());
        assertEquals(1, resultAgg.getBuckets().get(3).getDocCount());
        assertEquals("after", resultAgg.getBuckets().get(4).getKey());
        assertEquals(2, resultAgg.getBuckets().get(4).getDocCount());
    }

    @Test
    public void testAggregateSum() {
        AggregateSum agg = newAggregate(AGG_SUM, "myAgg", "common:size");
        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        if (response.isMissingCapabilities()) {
            // search client doesn't support this aggregate
            return;
        }
        var resultAgg = response.getAggregates().getFirst();
        assertEquals(1, resultAgg.getBuckets().size());
        assertEquals(2112020200, resultAgg.getBuckets().getFirst().getValue(), 0.1);
    }

    @Test
    public void testAggregateMinMaxAvgCountCardinality() {
        AggregateMin aggMin = newAggregate(AGG_MIN, "myMin", "common:size");
        AggregateMax aggMax = newAggregate(AGG_MAX, "myMax", "common:size");
        AggregateAvg aggAvg = newAggregate(AGG_AVG, "myAvg", "common:size");
        AggregateCount aggCount = newAggregate(AGG_COUNT, "myCount", "common:size");
        AggregateCardinality aggCardinality = newAggregate(AGG_CARDINALITY, "myCard", "common:size");
        SearchResponse response = search("SELECT * FROM Document", aggMin, aggMax, aggAvg, aggCount, aggCardinality);

        assertTrue(response.getTotal() > 0);
        assertEquals(5, response.getAggregates().size());
        var r = response.getAggregates();
        if (response.isMissingCapabilities()) {
            // search client doesn't support this aggregate
            return;
        }
        assertEquals(100, r.get(0).getBuckets().getFirst().getValue(), 0.1);
        assertEquals(1000000000, r.get(1).getBuckets().getFirst().getValue(), 0.1);
        assertEquals(211202020, r.get(2).getBuckets().getFirst().getValue(), 0.1);
        assertEquals(10, r.get(3).getBuckets().getFirst().getValue(), 0.1);
        assertEquals(6, r.get(4).getBuckets().getFirst().getValue(), 0.1);
    }

    @Test
    public void testAggregateHistogram() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("histogram");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("common:size");
        aggDef.setProperty("interval", "250000000");
        aggDef.setProperty("extendedBoundsMin", "0");
        aggDef.setProperty("extendedBoundsMax", "1000000000");
        AggregateHistogram agg = new AggregateHistogram(aggDef, null);
        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(5, resultAgg.getBuckets().size());
        assertEquals(8, resultAgg.getBuckets().get(0).getDocCount());
        assertEquals(0, resultAgg.getBuckets().get(1).getDocCount());
        assertEquals(0, resultAgg.getBuckets().get(2).getDocCount());
        assertEquals(0, resultAgg.getBuckets().get(3).getDocCount());
        assertEquals(2, resultAgg.getBuckets().get(4).getDocCount());
    }

    @Test
    public void testAggregateDateHistogram() {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setType("date_histogram");
        aggDef.setId("myAgg");
        aggDef.setDocumentField("dc:modified");
        aggDef.setProperty("interval", "year");
        AggregateDateHistogram agg = new AggregateDateHistogram(aggDef, null);
        SearchResponse response = search("SELECT * FROM Document", agg);

        assertTrue(response.getTotal() > 0);
        assertEquals(1, response.getAggregates().size());
        var resultAgg = response.getAggregates().getFirst();
        assertEquals("myAgg", resultAgg.getId());
        assertEquals(3, resultAgg.getBuckets().size());
        assertEquals(1, resultAgg.getBuckets().get(0).getDocCount());
        assertEquals(7, resultAgg.getBuckets().get(1).getDocCount());
        assertEquals(2, resultAgg.getBuckets().get(2).getDocCount());
    }

    @SafeVarargs
    protected final SearchResponse search(String nxql, Aggregate<? extends Bucket> aggregate,
            Aggregate<? extends Bucket>... aggregates) {
        return getClient().search(SearchQuery.builder(nxql)
                                             .searchIndex(getIndex())
                                             .limit(1_000)
                                             .addAggregate(aggregate)
                                             .addAggregates(List.of(aggregates))
                                             .build());
    }
}
