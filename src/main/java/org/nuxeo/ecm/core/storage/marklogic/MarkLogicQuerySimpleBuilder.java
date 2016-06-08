/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;

import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawQueryDefinition;
import com.marklogic.client.query.RawStructuredQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;

/**
 * Simple query builder for a MarkLogic query.
 *
 * @since 8.3
 */
class MarkLogicQuerySimpleBuilder {

    private final QueryManager queryManager;

    private final StructuredQueryBuilder sqb;

    private final List<StructuredQueryDefinition> queries;

    private final Set<String> selectKeys;

    public MarkLogicQuerySimpleBuilder(QueryManager queryManager) {
        this.queryManager = queryManager;
        this.sqb = queryManager.newStructuredQueryBuilder();
        this.queries = new ArrayList<>();
        this.selectKeys = new HashSet<>();
    }

    public MarkLogicQuerySimpleBuilder eq(String key, Object value) {
        String serializedKey = MarkLogicHelper.serializeKey(key);
        String serializedValue = MarkLogicStateSerializer.serializeValue(value);
        Type type = DBSSession.getType(key);
        // TODO check if it's enought
        if (type instanceof ListType) {
            queries.add(sqb.containerQuery(sqb.element(serializedKey),
                    sqb.value(sqb.element(MarkLogicHelper.ARRAY_ITEM_KEY), serializedValue)));
        } else {
            queries.add(sqb.value(sqb.element(serializedKey), serializedValue));
        }
        return this;
    }

    public MarkLogicQuerySimpleBuilder notIn(String key, Collection<?> values) {
        if (!values.isEmpty()) {
            String serializedKey = MarkLogicHelper.serializeKey(key);
            StructuredQueryDefinition conditionQuery;
            if (values.size() == 1) {
                String serializedValue = MarkLogicStateSerializer.serializeValue(values.iterator().next());
                conditionQuery = sqb.value(sqb.element(serializedKey), serializedValue);
            } else {
                StructuredQueryDefinition[] orQueries = values.stream()
                                                              .map(MarkLogicStateSerializer::serializeValue)
                                                              .map(v -> sqb.value(sqb.element(serializedKey), v))
                                                              .toArray(StructuredQueryDefinition[]::new);
                conditionQuery = sqb.or(orQueries);
            }
            StructuredQueryDefinition notQuery = sqb.not(conditionQuery);
            queries.add(notQuery);
        }
        return this;
    }

    public MarkLogicQuerySimpleBuilder select(String key) {
        selectKeys.add(key);
        return this;
    }

    public RawQueryDefinition build() {
        RawStructuredQueryDefinition query = sqb.build(queries.toArray(new StructuredQueryDefinition[queries.size()]));
        if (selectKeys.isEmpty()) {
            return query;
        }
        String comboQuery = "<search xmlns=\"http://marklogic.com/appservices/search\">" //
                + query.toString() //
                + buildSelectPaths() //
                + "</search>";
        return queryManager.newRawCombinedQueryDefinition(new StringHandle(comboQuery));
    }

    private String buildSelectPaths() {
        StringBuilder extract = new StringBuilder("<options xmlns=\"http://marklogic.com/appservices/search\">");
        extract.append("<extract-document-data selected=\"include-with-ancestors\">");
        for (String selectKey : selectKeys) {
            extract.append("<extract-path>");
            extract.append(MarkLogicHelper.DOCUMENT_ROOT_PATH)
                   .append('/')
                   .append(MarkLogicHelper.serializeKey(selectKey));
            extract.append("</extract-path>");
        }
        extract.append("</extract-document-data>");
        extract.append("</options>");
        return extract.toString();
    }

}
