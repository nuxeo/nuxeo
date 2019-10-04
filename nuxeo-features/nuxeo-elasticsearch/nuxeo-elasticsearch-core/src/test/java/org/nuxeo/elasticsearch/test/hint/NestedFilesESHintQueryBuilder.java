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

package org.nuxeo.elasticsearch.test.hint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.model.EsHint;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;

/**
 * Allows to make a nested ES queries on {@code "files:files"} of a given document. Note that to be able to query ES
 * using nested, the field should be a {@code nested} type.
 *
 * @since 11.1
 */
public class NestedFilesESHintQueryBuilder implements ESHintQueryBuilder {

    @Override
    public QueryBuilder make(EsHint hint, String fieldName, Object value) {
        // Get fields
        List<String> fields = hint.getIndex().stream().map(EsHint.FieldHint::getField).collect(Collectors.toList());

        // Get the values
        Object[] values = (Object[]) value;

        if (fields.size() != values.length) {
            throw new NuxeoException("Fields size and values length should be the same", SC_BAD_REQUEST);
        }

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (int index = 0; index < values.length; index++) {
            boolQueryBuilder.must(QueryBuilders.termQuery(fields.get(index), values[index]));
        }

        return QueryBuilders.nestedQuery("files:files.file", boolQueryBuilder, ScoreMode.None);
    }
}
