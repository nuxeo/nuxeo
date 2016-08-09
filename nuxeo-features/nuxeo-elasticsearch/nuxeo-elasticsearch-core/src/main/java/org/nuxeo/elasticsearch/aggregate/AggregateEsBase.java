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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.FULLTEXT_FIELD;

import java.util.Collection;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateBase;

/**
 * @since 6.0
 */
public abstract class AggregateEsBase<B extends Bucket> extends AggregateBase<B> {

    public final static char XPATH_SEP = '/';

    public final static char ES_MUTLI_LEVEL_SEP = '.';

    public AggregateEsBase(AggregateDefinition definition, DocumentModel searchDocument) {
        super(definition, searchDocument);
    }

    /**
     * Return the Elasticsearch aggregate builder
     */
    public abstract AggregationBuilder getEsAggregate();

    /**
     * Return the Elasticsearch aggregate filter corresponding to the selection
     */
    public abstract QueryBuilder getEsFilter();

    /**
     * Extract the buckets from the Elasticsearch response
     */
    public abstract void parseEsBuckets(Collection<? extends MultiBucketsAggregation.Bucket> buckets);

    @Override
    public String getField() {
        String ret = super.getField();
        if (NXQL.ECM_FULLTEXT.equals(ret)) {
            ret = FULLTEXT_FIELD;
        }
        ret = ret.replace(XPATH_SEP, ES_MUTLI_LEVEL_SEP);
        return ret;
    }

}
