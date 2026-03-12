/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_FULLTEXT_SCORE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter.REPOSITORY_PROP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.TotalHits;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.search.AbstractSearchResponseTransformer;
import org.nuxeo.ecm.core.search.SearchClient;
import org.nuxeo.ecm.core.search.SearchHit;
import org.nuxeo.ecm.core.search.SearchQuery;
import org.nuxeo.ecm.core.search.SearchResponse;
import org.nuxeo.ecm.core.search.SearchScrollContext;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateDateHistogramParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateDateRangeParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateHistogramParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateParserBase;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateRangeParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateSignificantTermParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateSingleValueMetricParser;
import org.nuxeo.ecm.core.search.client.opensearch1.aggregate.AggregateTermParser;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateAvg;
import org.nuxeo.ecm.platform.query.core.AggregateCardinality;
import org.nuxeo.ecm.platform.query.core.AggregateCount;
import org.nuxeo.ecm.platform.query.core.AggregateDateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateDateRange;
import org.nuxeo.ecm.platform.query.core.AggregateHistogram;
import org.nuxeo.ecm.platform.query.core.AggregateMax;
import org.nuxeo.ecm.platform.query.core.AggregateMin;
import org.nuxeo.ecm.platform.query.core.AggregateMissing;
import org.nuxeo.ecm.platform.query.core.AggregateRange;
import org.nuxeo.ecm.platform.query.core.AggregateSignificantTerm;
import org.nuxeo.ecm.platform.query.core.AggregateSum;
import org.nuxeo.ecm.platform.query.core.AggregateTerm;
import org.nuxeo.ecm.platform.query.core.BucketDouble;
import org.opensearch.common.text.Text;
import org.opensearch.search.SearchHits;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.Aggregations;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.opensearch.search.aggregations.bucket.filter.Filter;
import org.opensearch.search.aggregations.bucket.missing.ParsedMissing;
import org.opensearch.search.aggregations.metrics.NumericMetricsAggregation;

/**
 * @since 2025.0
 */
public class OpenSearchResponseTransformer
        extends AbstractSearchResponseTransformer<org.opensearch.action.search.SearchResponse> {

    private static final Logger log = LogManager.getLogger(OpenSearchResponseTransformer.class);

    protected OpenSearchResponseTransformer(Set<SearchClient.Capability> supportedCapabilities) {
        super(supportedCapabilities);
    }

    @Override
    public SearchResponse apply(SearchQuery searchQuery, org.opensearch.action.search.SearchResponse osSearchResponse) {
        // TODO check error
        var osHits = osSearchResponse.getHits();
        var searchHits = makeSearchHits(searchQuery, osHits);
        var responseBuilder = SearchResponse.builder(searchHits);
        // total hits
        var osTotalHits = osHits.getTotalHits();
        if (osTotalHits != null) {
            responseBuilder.total(osTotalHits.value)
                           .totalAccurate(osTotalHits.relation.equals(TotalHits.Relation.EQUAL_TO));
        }
        // scroll
        if (searchQuery.isScrollSearch()) {
            responseBuilder.scroll(new SearchScrollContext(searchQuery, osSearchResponse.getScrollId()));
        }
        // aggregations
        responseBuilder.aggregates(makeAggregates(searchQuery, osSearchResponse.getAggregations()));
        responseBuilder.limitations(getLimitations(searchQuery));
        return responseBuilder.build();
    }

    protected List<SearchHit> makeSearchHits(SearchQuery searchQuery, SearchHits osHits) {
        var hits = new ArrayList<SearchHit>(osHits.getHits().length);
        for (var osHit : osHits) {
            var source = osHit.getSourceAsMap();
            if (source == null) {
                log.warn("Got hit without sources: {}", osHit);
                hits.add(SearchHit.builder(osHit.getIndex(), osHit.getId()).build());
            } else {
                String repository = source.containsKey(REPOSITORY_PROP) ? source.get(REPOSITORY_PROP).toString() : null;
                String docId = source.containsKey(ECM_UUID) ? source.get(ECM_UUID).toString() : null;
                hits.add(SearchHit.builder(osHit.getIndex(), osHit.getId())
                                  .repository(repository)
                                  .docId(docId)
                                  .fields(makeFields(searchQuery, source, osHit.getScore()))
                                  .highlights(makeHighlights(osHit))
                                  .build());
            }
        }
        return hits;
    }

    protected Map<String, Serializable> makeFields(SearchQuery searchQuery, Map<String, Object> source, double score) {
        var fields = new HashMap<String, Serializable>(searchQuery.getSelectFields().size());
        for (var selectedFieldEntry : searchQuery.getSelectFields().entrySet()) {
            String name = selectedFieldEntry.getKey();
            Type type = selectedFieldEntry.getValue();
            Object value = source.get(name);
            // type conversion
            if (value instanceof String stringValue && type instanceof DateType) {
                // convert back to calendar
                value = type.decode(stringValue);
            } else if (ECM_FULLTEXT_SCORE.equals(name)) {
                value = score;
            }
            fields.put(name, (Serializable) value);
        }
        return fields;
    }

    protected Map<String, List<String>> makeHighlights(org.opensearch.search.SearchHit osHit) {
        var highlights = new HashMap<String, List<String>>(osHit.getHighlightFields().size());
        for (var highlightEntry : osHit.getHighlightFields().entrySet()) {
            List<String> list = new ArrayList<>(highlightEntry.getValue().getFragments().length);
            for (Text fragment : highlightEntry.getValue().getFragments()) {
                list.add(fragment.toString());
            }
            highlights.put(highlightEntry.getKey(), list);
        }
        return highlights;
    }

    protected List<Aggregate<? extends Bucket>> makeAggregates(SearchQuery searchQuery, Aggregations osAggregations) {
        for (Aggregate<? extends Bucket> agg : searchQuery.getAggregates()) {
            String aggName = AggregateParserBase.getFilterId(agg);
            Filter filter = osAggregations.get(aggName);
            if (filter == null) {
                continue;
            }
            Aggregation aggregation = filter.getAggregations().get(agg.getId());
            if (aggregation == null) {
                continue;
            }
            switch (agg) {
                case AggregateTerm a -> a.setBuckets(
                        AggregateTermParser.parseBuckets(((MultiBucketsAggregation) aggregation).getBuckets()));
                case AggregateSignificantTerm a -> a.setBuckets(AggregateSignificantTermParser.parseBuckets(
                        ((MultiBucketsAggregation) aggregation).getBuckets()));
                case AggregateRange a -> a.setBuckets(
                        AggregateRangeParser.parseBuckets(((MultiBucketsAggregation) aggregation).getBuckets()));
                case AggregateDateRange a -> a.setBuckets(
                        AggregateDateRangeParser.parseBuckets(((MultiBucketsAggregation) aggregation).getBuckets()));
                case AggregateHistogram a -> a.setBuckets(
                        AggregateHistogramParser.parseBuckets(((MultiBucketsAggregation) aggregation).getBuckets(), a));
                case AggregateDateHistogram a -> a.setBuckets(AggregateDateHistogramParser.parseBuckets(
                        ((MultiBucketsAggregation) aggregation).getBuckets(), a));
                case AggregateSum a -> a.setBuckets(AggregateSingleValueMetricParser.parseBuckets(
                        (NumericMetricsAggregation.SingleValue) aggregation));
                case AggregateMax a -> a.setBuckets(AggregateSingleValueMetricParser.parseBuckets(
                        (NumericMetricsAggregation.SingleValue) aggregation));
                case AggregateMin a -> a.setBuckets(AggregateSingleValueMetricParser.parseBuckets(
                        (NumericMetricsAggregation.SingleValue) aggregation));
                case AggregateAvg a -> a.setBuckets(AggregateSingleValueMetricParser.parseBuckets(
                        (NumericMetricsAggregation.SingleValue) aggregation));
                case AggregateCount a -> a.setBuckets(AggregateSingleValueMetricParser.parseBuckets(
                        (NumericMetricsAggregation.SingleValue) aggregation));
                case AggregateCardinality a -> a.setBuckets(AggregateSingleValueMetricParser.parseBuckets(
                        (NumericMetricsAggregation.SingleValue) aggregation));
                case AggregateMissing a ->
                    a.setBuckets(List.of(new BucketDouble("", (double) ((ParsedMissing) aggregation).getDocCount())));
                default -> throw new IllegalStateException("Unexpected value: " + agg);
            }
        }
        return searchQuery.getAggregates();
    }
}
