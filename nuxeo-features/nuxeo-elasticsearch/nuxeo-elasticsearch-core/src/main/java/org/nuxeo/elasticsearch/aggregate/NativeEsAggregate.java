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

import java.util.function.Consumer;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;

/**
 * This class is intended for internal/advanced use.
 * It supports any ElasticSearch aggregate builder as a constructor parameter.
 * However, it doesn't support Nuxeo page providers or the aggregation factory.
 * The other aggregate classes are the preferred approach.
 *
 * @since 10.3
 */
public class NativeEsAggregate extends AggregateEsBase<Aggregation, Bucket> {

    protected final AggregationBuilder nativeAggregation;

    protected final Consumer<Aggregation> parser;

    public NativeEsAggregate(AggregateDefinition definition, AggregationBuilder nativeAggregation,
            Consumer<Aggregation> parser) {
        super(definition, null);
        this.nativeAggregation = nativeAggregation;
        this.parser = parser;
    }

    /**
     * Construct the aggregate using an ElasticSearch aggregate builder and a parser that will
     * consume the response.
     */
    public NativeEsAggregate(AggregationBuilder nativeAggregation, Consumer<Aggregation> parser) {
        this(makeDefinition(nativeAggregation), nativeAggregation, parser);
    }

    /**
     * For backwards compatibility make an AggregateDefinition.
     */
    protected static AggregateDefinition makeDefinition(AggregationBuilder nativeAggregation) {
        AggregateDescriptor descriptor = new AggregateDescriptor();
        descriptor.setId(nativeAggregation.getName());
        return descriptor;
    }

    @Override
    public AggregationBuilder getEsAggregate() {
        return nativeAggregation;
    }

    @Override
    public QueryBuilder getEsFilter() {
        if (getSelection().isEmpty()) {
            return null;
        }
        return QueryBuilders.termsQuery(getField(), getSelection());
    }

    @Override
    public void parseAggregation(Aggregation aggregation) {
        parser.accept(aggregation);
    }

    @Override
    public String toString() {
        return nativeAggregation.toString();
    }
}
