/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;

/**
 * @since 6.0
 */
public class DateHistogramAggregate extends AggregateEsBase<BucketRangeDate> {

    Long intervalMillis;

    public DateHistogramAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public DateHistogramBuilder getEsAggregate() {
        DateHistogramBuilder ret = AggregationBuilders.dateHistogram(getId())
                                                      .field(getField())
                                                      .timeZone(DateTimeZone.getDefault().getID());
        Map<String, String> props = getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            ret.interval(new DateHistogram.Interval(props.get(AGG_INTERVAL_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
            case AGG_ORDER_COUNT_DESC:
                ret.order(Histogram.Order.COUNT_DESC);
                break;
            case AGG_ORDER_COUNT_ASC:
                ret.order(Histogram.Order.COUNT_ASC);
                break;
            case AGG_ORDER_KEY_DESC:
                ret.order(Histogram.Order.KEY_DESC);
                break;
            case AGG_ORDER_KEY_ASC:
                ret.order(Histogram.Order.KEY_ASC);
                break;
            default:
                throw new IllegalArgumentException("Invalid order: " + props.get(AGG_ORDER_PROP));
            }
        }
        if (props.containsKey(AGG_EXTENDED_BOUND_MAX_PROP) && props.containsKey(AGG_EXTENDED_BOUND_MIN_PROP)) {
            ret.extendedBounds(props.get(AGG_EXTENDED_BOUND_MIN_PROP), props.get(AGG_EXTENDED_BOUND_MAX_PROP));
        }
        if (props.containsKey(AGG_TIME_ZONE_PROP)) {
            ret.timeZone(props.get(AGG_TIME_ZONE_PROP));
        }
        if (props.containsKey(AGG_PRE_ZONE_PROP)) {
            ret.timeZone(props.get(AGG_PRE_ZONE_PROP));
        }
        if (props.containsKey(AGG_FORMAT_PROP)) {
            ret.format(props.get(AGG_FORMAT_PROP));
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public FilterBuilder getEsFilter() {
        if (getSelection().isEmpty()) {
            return null;
        }
        OrFilterBuilder ret = FilterBuilders.orFilter();
        for (String sel : getSelection()) {
            RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter(getField());
            long from = convertStringToDate(sel);
            long to = from + getIntervalInMillis();
            rangeFilter.gte(from).lt(to);
            ret.add(rangeFilter);
        }
        return ret;
    }

    private long convertStringToDate(String date) {
        Map<String, String> props = getProperties();
        DateTimeFormatter fmt;
        if (props.containsKey(AGG_FORMAT_PROP)) {
            fmt = DateTimeFormat.forPattern(props.get(AGG_FORMAT_PROP));
        } else {
            throw new IllegalArgumentException("format property must be defined for " + toString());
        }
        // TODO should take in account all the locale zone stuff ...
        return fmt.parseDateTime(date).getMillis();
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketRangeDate> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            DateHistogram.Bucket dateHistoBucket = (DateHistogram.Bucket) bucket;
            DateTime from = getDateTime(dateHistoBucket.getKeyAsDate());
            DateTime to = addInterval(from);
            nxBuckets.add(new BucketRangeDate(bucket.getKey(), from, to, dateHistoBucket.getDocCount()));
        }
        this.buckets = nxBuckets;
    }

    private DateTime addInterval(DateTime from) {
        return new DateTime(from.getMillis() + getIntervalInMillis());
    }

    public long getIntervalInMillis() {
        if (intervalMillis == null) {
            String interval;
            Map<String, String> props = getProperties();
            if (props.containsKey(AGG_INTERVAL_PROP)) {
                interval = props.get(AGG_INTERVAL_PROP);
            } else {
                throw new IllegalArgumentException("interval property must be defined for " + toString());
            }
            interval = convertToTimeValueString(interval);
            intervalMillis = (long) TimeValue.parseTimeValue(interval, null).getMillis();
        }
        return intervalMillis;
    }

    private String convertToTimeValueString(String interval) {
        switch (interval.toLowerCase()) {
        case "second":
            return "1s";
        case "minute":
            return "1m";
        case "hour":
            return "1h";
        case "day":
            return "1d";
        case "week":
            return "7d";
        case "year":
            return "365d";
            // may be wrong here ...
        case "month":
            return "30d";
        case "quarter":
            return "91d";
        default:
            // already in ms
        }
        return interval;
    }

}
