/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_FORMAT_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.joda.time.DateTime;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 6.0
 */
public class DateRangeAggregate extends MultiBucketAggregate<BucketRangeDate> {

    public DateRangeAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public DateRangeAggregationBuilder getEsAggregate() {
        DateRangeAggregationBuilder ret = AggregationBuilders.dateRange(getId()).field(getField());
        for (AggregateRangeDateDefinition range : getDateRanges()) {
            if (range.getFromAsString() != null) {
                if (range.getToAsString() != null) {
                    ret.addRange(range.getKey(), range.getFromAsString(), range.getToAsString());
                } else {
                    ret.addUnboundedFrom(range.getKey(), range.getFromAsString());
                }
            } else if (range.getToAsString() != null) {
                ret.addUnboundedTo(range.getKey(), range.getToAsString());
            }
        }
        Map<String, String> props = getProperties();
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
        for (AggregateRangeDateDefinition range : getDateRanges()) {
            if (getSelection().contains(range.getKey())) {
                RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(getField());
                if (range.getFromAsString() != null) {
                    rangeFilter.gte(range.getFromAsString());
                }
                if (range.getToAsString() != null) {
                    rangeFilter.lt(range.getToAsString());
                }
                ret.should(rangeFilter);
            }
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketRangeDate> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            Range.Bucket rangeBucket = (Range.Bucket) bucket;
            nxBuckets.add(new BucketRangeDate(bucket.getKeyAsString(), (DateTime) rangeBucket.getFrom(),
                    (DateTime) rangeBucket.getTo(), rangeBucket.getDocCount()));
        }
        nxBuckets.sort(new BucketRangeDateComparator());
        this.buckets = nxBuckets;
    }

    protected class BucketRangeDateComparator implements Comparator<BucketRangeDate> {
        @Override
        public int compare(BucketRangeDate arg0, BucketRangeDate arg1) {
            return definition.getAggregateDateRangeDefinitionOrderMap().get(arg0.getKey()).compareTo(
                    definition.getAggregateDateRangeDefinitionOrderMap().get(arg1.getKey()));
        }
    }

}
