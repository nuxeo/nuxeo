/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.elasticsearch.hint;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;

/**
 * The implementation of {@link ESHintQueryBuilder} for the <strong>"simple_query_string"</strong> Elasticsearch hint
 * operator.
 *
 * @since 11.1
 */
public class SimpleQueryStringESHintQueryBuilder implements ESHintQueryBuilder {

    /**
     * {@inheritDoc}
     * <p>
     *
     * @return {@link org.elasticsearch.index.query.SimpleQueryStringBuilder}
     */
    @Override
    public QueryBuilder make(EsHint hint, String fieldName, Object value) {
        SimpleQueryStringBuilder querySimpleString = QueryBuilders.simpleQueryStringQuery((String) value);
        if (hint.index != null) {
            for (EsHint.FieldHint fieldHint : hint.getIndex()) {
                querySimpleString.field(fieldHint.getField(), fieldHint.getBoost());
            }
        } else {
            querySimpleString.field(fieldName);
        }
        querySimpleString.analyzer(hint.analyzer);
        return querySimpleString;
    }
}
