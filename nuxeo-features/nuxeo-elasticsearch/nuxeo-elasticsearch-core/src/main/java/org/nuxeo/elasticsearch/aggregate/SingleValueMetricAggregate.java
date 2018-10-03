/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.elasticsearch.aggregate;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_AVG;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_CARDINALITY;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_COUNT;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MAX;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_SUM;

import java.util.Collections;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketTerm;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An aggregate that returns a single value.
 *
 * @since 10.3
 */
public class SingleValueMetricAggregate extends AggregateEsBase<NumericMetricsAggregation.SingleValue, BucketTerm> {

    protected final AggregationBuilder aggregationBuilder;

    protected Double value;

    public SingleValueMetricAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
        this.aggregationBuilder = toBuilder(definition.getType());
    }

    /**
     * Creates an AggregationBuilder for the supplied type
     */
    public AggregationBuilder toBuilder(String type) {
        switch (type) {
        case AGG_CARDINALITY:
            return AggregationBuilders.cardinality(getId()).field(getField());
        case AGG_COUNT:
            return AggregationBuilders.count(getId()).field(getField());
        case AGG_SUM:
            return AggregationBuilders.sum(getId()).field(getField());
        case AGG_AVG:
            return AggregationBuilders.avg(getId()).field(getField());
        case AGG_MAX:
            return AggregationBuilders.max(getId()).field(getField());
        case AGG_MIN:
            return AggregationBuilders.min(getId()).field(getField());
        default:
            throw new IllegalArgumentException("Unknown aggregate type: " + type);
        }
    }

    @JsonIgnore
    @Override
    public AggregationBuilder getEsAggregate() {
        return aggregationBuilder;
    }

    @JsonIgnore
    @Override
    public QueryBuilder getEsFilter() {
        if (getSelection().isEmpty()) {
            return null;
        }
        return QueryBuilders.termsQuery(getField(), getSelection());
    }

    @Override
    public void parseAggregation(NumericMetricsAggregation.SingleValue aggregation) {
        this.value = aggregation.value();
        this.buckets = Collections.singletonList(new BucketTerm(definition.getType(), value.longValue()));
    }
    
    public Double getValue() {
        return value;
    }
}
