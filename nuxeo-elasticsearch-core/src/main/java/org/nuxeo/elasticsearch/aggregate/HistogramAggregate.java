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
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_INTERVAL_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_KEY_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_PROP;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketTerm;

/**
 * @since 5.9.6
 */
public class HistogramAggregate extends BaseEsAggregate<BucketTerm> {

    public HistogramAggregate(AggregateDefinition definition,
            DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @Override
    public List<BucketTerm> getBuckets() {
        return super.getBuckets();
    }

    @Override
    public HistogramBuilder getEsAggregate() {
        HistogramBuilder ret = AggregationBuilders.histogram(getId()).field(
                getField());
        Map<String, String> props = getProperties();
        if (props.containsKey(AGG_INTERVAL_PROP)) {
            ret.interval(Integer.parseInt(props.get(AGG_INTERVAL_PROP)));
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
            }
        }
        if (props.containsKey(AGG_EXTENDED_BOUND_MAX_PROP)
                && props.containsKey(AGG_EXTENDED_BOUND_MIN_PROP)) {
            ret.extendedBounds(
                    Long.parseLong(props.get(AGG_EXTENDED_BOUND_MIN_PROP)),
                    Long.parseLong(props.get(AGG_EXTENDED_BOUND_MAX_PROP)));
        }
        return ret;
    }

    @Override public FilterBuilder getEsFilter() {
        // Not implemented
        return null;
    }

    @Override public void extractEsBuckets(
            Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        // TODO impl
    }
}
