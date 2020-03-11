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

import java.util.Collection;

import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;

/**
 * An aggregate that returns multiple buckets.
 *
 * @since 10.3
 */
public abstract class MultiBucketAggregate<B extends Bucket> extends AggregateEsBase<MultiBucketsAggregation, B> {

    public MultiBucketAggregate(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    @Override
    public void parseAggregation(MultiBucketsAggregation aggregation) {
        parseEsBuckets(aggregation.getBuckets());
    }

    /**
     * Extract the buckets from the Elasticsearch response
     */
    public abstract void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets);

}
