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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_EXCLUDE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_INCLUDE_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_COUNT_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_TERM_ASC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_ORDER_TERM_DESC;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_SIZE_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketTerm;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 6.0
 */
public class TermAggregate extends MultiBucketAggregate<BucketTerm> {

    public TermAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @JsonIgnore
    @Override
    public TermsAggregationBuilder getEsAggregate() {
        TermsAggregationBuilder ret = AggregationBuilders.terms(getId()).field(getField());
        Map<String, String> props = getProperties();
        if (props.containsKey(AGG_SIZE_PROP)) {
            ret.size(getAggSize(props.get(AGG_SIZE_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Long.parseLong(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        if (props.containsKey(AGG_EXCLUDE_PROP) || props.containsKey(AGG_INCLUDE_PROP)) {
            String include = props.get(AGG_INCLUDE_PROP);
            String exclude = props.get(AGG_EXCLUDE_PROP);
            ret.includeExclude(new IncludeExclude(include, exclude));
        }
        if (props.containsKey(AGG_ORDER_PROP)) {
            switch (props.get(AGG_ORDER_PROP).toLowerCase()) {
            case AGG_ORDER_COUNT_DESC:
                ret.order(BucketOrder.count(false));
                break;
            case AGG_ORDER_COUNT_ASC:
                ret.order(BucketOrder.count(true));
                break;
            case AGG_ORDER_TERM_DESC:
                ret.order(BucketOrder.key(false));
                break;
            case AGG_ORDER_TERM_ASC:
                ret.order(BucketOrder.key(true));
                break;
            default:
                throw new IllegalArgumentException("Invalid order: " + props.get(AGG_ORDER_PROP));
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
        return QueryBuilders.termsQuery(getField(), getSelection());
    }

    @JsonIgnore
    @Override
    public void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketTerm> nxBuckets = new ArrayList<>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            nxBuckets.add(new BucketTerm(bucket.getKeyAsString(), bucket.getDocCount()));
        }
        this.buckets = nxBuckets;
    }
}
