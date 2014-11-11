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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_FORMAT_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRange;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;

/**
 * @since 6.0
 */
public class DateRangeAggregate extends AggregateEsBase<BucketRangeDate> {

    public DateRangeAggregate(AggregateDefinition definition,
            DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public DateRangeBuilder getEsAggregate() {
        DateRangeBuilder ret = AggregationBuilders.dateRange(getId()).field(
                getField());
        for (AggregateRangeDateDefinition range : getDateRanges()) {
            if (range.getFromAsString() != null) {
                if (range.getToAsString() != null) {
                    ret.addRange(range.getKey(), range.getFromAsString(),
                            range.getToAsString());
                } else {
                    ret.addUnboundedFrom(range.getKey(),
                            range.getFromAsString());
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
    public OrFilterBuilder getEsFilter() {
        if (getSelection().isEmpty()) {
            return null;
        }
        OrFilterBuilder ret = FilterBuilders.orFilter();
        for (AggregateRangeDateDefinition range : getDateRanges()) {
            if (getSelection().contains(range.getKey())) {
                RangeFilterBuilder rangeFilter = FilterBuilders
                        .rangeFilter(getField());
                if (range.getFromAsString() != null) {
                    rangeFilter.gte(range.getFromAsString());
                }
                if (range.getToAsString() != null) {
                    rangeFilter.lt(range.getToAsString());
                }
                ret.add(rangeFilter);
            }
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(
            Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketRangeDate> nxBuckets = new ArrayList<BucketRangeDate>(
                buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            DateRange.Bucket rangeBucket = (DateRange.Bucket) bucket;
            nxBuckets.add(new BucketRangeDate(bucket.getKey(),
                    getDateTime(rangeBucket.getFromAsDate()),
                    getDateTime(rangeBucket.getToAsDate()), rangeBucket
                            .getDocCount()));
        }
        Collections.sort(nxBuckets, new BucketRangeDateComparator());
        this.buckets = nxBuckets;
    }

    protected class BucketRangeDateComparator implements
            Comparator<BucketRangeDate> {
        @Override
        public int compare(BucketRangeDate arg0, BucketRangeDate arg1) {
            return definition.getAggregateDateRangeDefinitionOrderMap().get(
                    arg0.getKey()).compareTo(
                    definition.getAggregateDateRangeDefinitionOrderMap().get(
                            arg1.getKey()));
        }
    }

}
