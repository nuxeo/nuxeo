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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRange;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 6.0
 */
public class RangeAggregate extends MultiBucketAggregate<BucketRange> {

    public RangeAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public RangeAggregationBuilder getEsAggregate() {
        RangeAggregationBuilder ret = AggregationBuilders.range(getId()).field(getField());
        for (AggregateRangeDefinition range : getRanges()) {
            if (range.getFrom() != null) {
                if (range.getTo() != null) {
                    ret.addRange(range.getKey(), range.getFrom(), range.getTo());
                } else {
                    ret.addUnboundedFrom(range.getKey(), range.getFrom());
                }
            } else if (range.getTo() != null) {
                ret.addUnboundedTo(range.getKey(), range.getTo());
            }
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
        for (AggregateRangeDefinition range : getRanges()) {
            if (getSelection().contains(range.getKey())) {
                RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(getField());
                if (range.getFrom() != null) {
                    rangeFilter.gte(range.getFrom());
                }
                if (range.getTo() != null) {
                    rangeFilter.lt(range.getTo());
                }
                ret.should(rangeFilter);
            }
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketRange> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            Range.Bucket rangeBucket = (Range.Bucket) bucket;
            double from = (double) rangeBucket.getFrom();
            double to = (double) rangeBucket.getTo();
            nxBuckets.add(new BucketRange(bucket.getKeyAsString(), from, to, rangeBucket.getDocCount()));
        }
        nxBuckets.sort(new BucketRangeComparator());
        this.buckets = nxBuckets;
    }

    protected class BucketRangeComparator implements Comparator<BucketRange> {
        @Override
        public int compare(BucketRange arg0, BucketRange arg1) {
            return definition.getAggregateRangeDefinitionOrderMap().get(arg0.getKey()).compareTo(
                    definition.getAggregateRangeDefinitionOrderMap().get(arg1.getKey()));
        }
    }

}
