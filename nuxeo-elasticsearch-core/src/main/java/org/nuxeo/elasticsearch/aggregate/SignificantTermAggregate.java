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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_MIN_DOC_COUNT_PROP;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_SIZE_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.BucketTerm;

/**
 * @since 5.9.6
 */
public class SignificantTermAggregate extends AggregateEsBase<BucketTerm> {

    public SignificantTermAggregate(AggregateDefinition definition,
            DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @Override
    public SignificantTermsBuilder getEsAggregate() {
        SignificantTermsBuilder ret = AggregationBuilders.significantTerms(
                getId()).field(getField());
        Map<String, String> props = getProperties();
        if (props.containsKey(AGG_SIZE_PROP)) {
            ret.size(Integer.parseInt(props.get(AGG_SIZE_PROP)));
        }
        if (props.containsKey(AGG_MIN_DOC_COUNT_PROP)) {
            ret.minDocCount(Integer.parseInt(props.get(AGG_MIN_DOC_COUNT_PROP)));
        }
        return ret;
    }

    @Override
    public TermsFilterBuilder getEsFilter() {
        if (getSelection().isEmpty()) {
            return null;
        }
        return FilterBuilders.termsFilter(getField(), getSelection());
    }

    @Override
    public void parseEsBuckets(
            Collection<? extends MultiBucketsAggregation.Bucket> buckets) {
        List<BucketTerm> nxBuckets = new ArrayList<BucketTerm>(buckets.size());
        for (MultiBucketsAggregation.Bucket bucket : buckets) {
            nxBuckets
                    .add(new BucketTerm(bucket.getKey(), bucket.getDocCount()));
        }
        this.buckets = nxBuckets;
    }

}
