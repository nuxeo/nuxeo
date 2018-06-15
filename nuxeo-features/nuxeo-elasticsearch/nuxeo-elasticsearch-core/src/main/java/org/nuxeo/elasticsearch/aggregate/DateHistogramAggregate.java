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
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_FORMAT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_INTERVAL_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_PRE_ZONE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_TIME_ZONE_PROP;

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
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;
import org.nuxeo.elasticsearch.ElasticSearchConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 6.0
 */
public class DateHistogramAggregate extends AggregateEsBase<BucketRangeDate> {

    public DateHistogramAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public DateHistogramAggregationBuilder getEsAggregate() {
        DateHistogramAggregationBuilder ret = AggregationBuilders.dateHistogram(getId()).field(getField()).timeZone(
                DateTimeZone.getDefault());
        Map<String, String> props = getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            ret.dateHistogramInterval(new DateHistogramInterval(props.get(AGG_INTERVAL_PROP)));
        }
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
            ret.extendedBounds(
                    new ExtendedBounds(props.get(AGG_EXTENDED_BOUND_MIN_PROP), props.get(AGG_EXTENDED_BOUND_MAX_PROP)));
        }
        if (props.containsKey(AGG_TIME_ZONE_PROP)) {
            ret.timeZone(DateTimeZone.forID(props.get(AGG_TIME_ZONE_PROP)));
        }
        if (props.containsKey(AGG_PRE_ZONE_PROP)) {
            ret.timeZone(DateTimeZone.forID(props.get(AGG_PRE_ZONE_PROP)));
        }
        if (props.containsKey(AGG_FORMAT_PROP)) {
            ret.format(props.get(AGG_FORMAT_PROP));
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
            DateTime from = convertStringToDate(sel);
            DateTime to = DateHelper.plusDuration(from, getInterval());
            rangeFilter.gte(from.getMillis()).lt(to.getMillis()).format(ElasticSearchConstants.EPOCH_MILLIS_FORMAT);
            ret.should(rangeFilter);
        }
        return ret;
    }

    private DateTime convertStringToDate(String date) {
        Map<String, String> props = getProperties();
        DateTimeFormatter fmt;
        if (props.containsKey(AGG_FORMAT_PROP)) {
            fmt = DateTimeFormat.forPattern(props.get(AGG_FORMAT_PROP));
        } else {
            throw new IllegalArgumentException("format property must be defined for " + toString());
        }
        if (props.containsKey(AGG_TIME_ZONE_PROP)) {
            fmt = fmt.withZone(DateTimeZone.forID(props.get(AGG_TIME_ZONE_PROP)));
        }
        return fmt.parseDateTime(date);
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketRangeDate> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            DateTime from = (DateTime) bucket.getKey();
            DateTime to = DateHelper.plusDuration(from, getInterval());
            nxBuckets.add(new BucketRangeDate(bucket.getKeyAsString(), from, to, bucket.getDocCount()));
        }
        this.buckets = nxBuckets;
    }

    private String getInterval() {
        String ret;
        Map<String, String> props = getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            ret = props.get(AGG_INTERVAL_PROP);
        } else {
            throw new IllegalArgumentException("interval property must be defined for " + toString());
        }
        return ret;
    }

}
