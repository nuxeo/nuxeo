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

package org.nuxeo.elasticsearch.api;

import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.EsHint;

/**
 * Converts an NXQL Elasticsearch Hint into {@link org.elasticsearch.index.query.QueryBuilder}.
 * 
 * @since 11.1
 */
public interface ESHintQueryBuilder {

    /**
     * Builds the Elasticsearch {@link org.elasticsearch.index.query.QueryBuilder}.
     *
     * @param hint the elasticsearch hint
     * @param fieldName the elasticsearch field name
     * @param value the value that we are looking for
     * @return the {@link QueryBuilder} corresponding to the <code>elasticsearch hint</code>
     */
    QueryBuilder make(EsHint hint, String fieldName, Object value);
}
