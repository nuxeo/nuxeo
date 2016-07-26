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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;

/**
 * Simple query builder for a MarkLogic query.
 *
 * @since 8.3
 */
class MarkLogicQuerySimpleBuilder {

    private final List<String> queries;

    public MarkLogicQuerySimpleBuilder() {
        this.queries = new ArrayList<>();
    }

    public MarkLogicQuerySimpleBuilder eq(String key, Object value) {
        Type type = DBSSession.getType(key);
        // TODO check if it's enought
        String k = key;
        if (type instanceof ListType) {
            k += MarkLogicHelper.ARRAY_ITEM_KEY_SUFFIX;
        }
        queries.add(elementValueQuery(k, value));
        return this;
    }

    public MarkLogicQuerySimpleBuilder notIn(String key, Collection<?> values) {
        if (!values.isEmpty()) {
            String query = elementValueQuery(key, values.toArray());
            String notQuery = String.format("cts:not-query(%s)", query);
            queries.add(notQuery);
        }
        return this;
    }

    private String elementValueQuery(String key, Object... values) {
        String serializedKey = MarkLogicHelper.serializeKey(key);
        String serializedValue = serializeValues(values);
        return String.format("cts:element-value-query(fn:QName(\"\",\"%s\"),%s)", serializedKey, serializedValue);
    }

    private String serializeValues(Object... values) {
        if (values.length == 1) {
            return "\"" + MarkLogicStateSerializer.serializeValue(values[0]) + "\"";
        }
        return Arrays.stream(values)
                     .map(MarkLogicStateSerializer::serializeValue)
                     .map(value -> "\"" + value + "\"")
                     .collect(Collectors.joining(",", "(", ")"));
    }

    public String build() {
        return String.format("cts:search(fn:doc(),cts:and-query((%s)))", String.join(",", queries));
    }

}
