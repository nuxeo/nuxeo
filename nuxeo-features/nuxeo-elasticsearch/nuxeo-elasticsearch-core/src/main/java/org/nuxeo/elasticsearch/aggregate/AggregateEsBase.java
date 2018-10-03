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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.AggregateBase;

/**
 * @since 6.0
 */
public abstract class AggregateEsBase<A extends Aggregation, B extends Bucket> extends AggregateBase<B> {

    public static final char XPATH_SEP = '/';

    public static final char ES_MUTLI_LEVEL_SEP = '.';

    public static final int MAX_AGG_SIZE = 1000;

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
     * Extract the aggregation from the Elasticsearch response
     * @since 10.3
     */
    public abstract void parseAggregation(A aggregation);

    @Override
    public String getField() {
        String ret = super.getField();
        if (NXQL.ECM_FULLTEXT.equals(ret)) {
            ret = FULLTEXT_FIELD;
        }
        ret = ret.replace(XPATH_SEP, ES_MUTLI_LEVEL_SEP);
        return ret;
    }

    protected int getAggSize(String prop) {
        // handle the size = 0 which means all terms in ES 2 and which is not supported in ES 5
        int size = Integer.parseInt(prop);
        return size == 0 ? MAX_AGG_SIZE : size;
    }

}
