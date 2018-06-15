/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.aggregate;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_EXTENDED_BOUND_MAX_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_EXTENDED_BOUND_MIN_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_INTERVAL_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRange;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 6.0
 */
public class HistogramAggregate extends AggregateEsBase<BucketRange> {

    private Integer interval;

    public HistogramAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public HistogramAggregationBuilder getEsAggregate() {
        HistogramAggregationBuilder ret = AggregationBuilders.histogram(getId()).field(getField());
        Map<String, String> props = getProperties();
        ret.interval(getInterval());
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
            case AGG_ORDER_COUNT_DESC:
                ret.order(BucketOrder.count(false));
                break;
            case AGG_ORDER_COUNT_ASC:
                ret.order(BucketOrder.count(true));
                break;
            case AGG_ORDER_KEY_DESC:
                ret.order(BucketOrder.key(false));
                break;
            case AGG_ORDER_KEY_ASC:
                ret.order(BucketOrder.key(true));
                break;
            default:
                throw new IllegalArgumentException("Invalid order: " + props.get(AGG_ORDER_PROP));
            }
        }
        if (props.containsKey(AGG_EXTENDED_BOUND_MAX_PROP) && props.containsKey(AGG_EXTENDED_BOUND_MIN_PROP)) {
            ret.extendedBounds(Long.parseLong(props.get(AGG_EXTENDED_BOUND_MIN_PROP)),
                    Long.parseLong(props.get(AGG_EXTENDED_BOUND_MAX_PROP)));
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public QueryBuilder getEsFilter() {
        if (getSelection().isEmpty()) {
            return null;
        }
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        for (String sel : getSelection()) {
            RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(getField());
            long from = Long.parseLong(sel);
            long to = from + getInterval();
            rangeFilter.gte(from).lt(to);
            ret.should(rangeFilter);
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketRange> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            Histogram.Bucket histoBucket = (Histogram.Bucket) bucket;
            int from = parseInt(histoBucket.getKeyAsString());
            nxBuckets.add(
                    new BucketRange(bucket.getKeyAsString(), from, from + getInterval(), histoBucket.getDocCount()));
        }
        this.buckets = nxBuckets;
    }

    public int getInterval() {
        if (interval == null) {
            Map<String, String> props = getProperties();
            if (props.containsKey(AGG_INTERVAL_PROP)) {
                interval = Integer.parseInt(props.get(AGG_INTERVAL_PROP));
            } else {
                throw new IllegalArgumentException("interval property must be defined for " + toString());
            }
        }
        return interval;
    }

    protected int parseInt(String key) {
        if ("-Infinity".equals(key)) {
            return Integer.MIN_VALUE;
        } else if ("+Infinity".equals(key)) {
            return Integer.MAX_VALUE;
        }
        return Math.round(Float.parseFloat(key));
    }
}
