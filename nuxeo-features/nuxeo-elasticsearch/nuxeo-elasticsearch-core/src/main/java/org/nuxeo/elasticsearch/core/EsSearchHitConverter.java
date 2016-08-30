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
package org.nuxeo.elasticsearch.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

/**
 * Converter used to convert a {@link SearchHit} to a {@link Map}&lt;{@link String}, {@link Serializable}&gt;.
 *
 * @since 8.4
 */
public class EsSearchHitConverter {

    private final Map<String, Type> selectFieldsAndTypes;

    private final Map<String, Serializable> emptyRow;

    public EsSearchHitConverter(Map<String, Type> selectFieldsAndTypes) {
        this.selectFieldsAndTypes = selectFieldsAndTypes;
        this.emptyRow = buildEmptyRow(selectFieldsAndTypes);
    }

    public List<Map<String, Serializable>> convert(SearchHit... hits) {
        return Arrays.stream(hits).map(this::convert).collect(Collectors.toList());
    }

    public Map<String, Serializable> convert(SearchHit hit) {
        Map<String, Serializable> row = new HashMap<>(emptyRow);
        for (SearchHitField field : hit.getFields().values()) {
            String name = field.getName();
            Serializable value = field.<Serializable> getValue();
            // type conversion
            Type type;
            if (value instanceof String && (type = selectFieldsAndTypes.get(name)) instanceof DateType) {
                // convert back to calendar
                value = (Serializable) type.decode(((String) value));
            }
            row.put(name, value);
        }
        if (selectFieldsAndTypes.containsKey(NXQL.ECM_FULLTEXT_SCORE)) {
            row.put(NXQL.ECM_FULLTEXT_SCORE, Double.valueOf(hit.getScore()));
        }
        return row;
    }

    private static Map<String, Serializable> buildEmptyRow(Map<String, Type> selectFieldsAndTypes) {
        Map<String, Serializable> emptyRow = new HashMap<>(selectFieldsAndTypes.size());
        for (String fieldName : selectFieldsAndTypes.keySet()) {
            emptyRow.put(fieldName, null);
        }
        return emptyRow;
    }

}
