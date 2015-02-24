/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.elasticsearch.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

/**
 * Iterable query result of the results of an Elasticsearch query.
 * <p>
 * Loads all results in memory.
 *
 * @since 7.2
 */
public class EsResultSetImpl implements IterableQueryResult, Iterator<Map<String, Serializable>> {

    private final SearchResponse response;

    private final Map<String, Type> selectFieldsAndTypes;

    boolean closed;

    protected List<Map<String, Serializable>> maps;

    protected long size;

    private long pos;

    public EsResultSetImpl(SearchResponse response, Map<String, Type> selectFieldsAndTypes) {
        this.response = response;
        this.selectFieldsAndTypes = selectFieldsAndTypes;
        maps = buildMaps();
        size = maps.size();
    }

    protected List<Map<String, Serializable>> buildMaps() {
        List<Map<String, Serializable>> rows = new ArrayList<>(response.getHits().getHits().length);
        Map<String, Serializable> emptyRow = new HashMap<>(selectFieldsAndTypes.size());
        for (String fieldName : selectFieldsAndTypes.keySet()) {
            emptyRow.put(fieldName, null);
        }
        for (SearchHit hit : response.getHits().getHits()) {
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
            rows.add(row);
        }
        return rows;
    }

    @Override
    public void close() {
        closed = true;
        pos = -1;
    }

    @Override
    public boolean isLife() {
        return !closed;
    }

    @Override
    public long size() {
        return response.getHits().getTotalHits();
    }

    @Override
    public long pos() {
        return pos;
    }

    @Override
    public void skipTo(long pos) {
        if (pos < 0) {
            pos = 0;
        } else if (pos > size) {
            pos = size;
        }
        this.pos = pos;
    }

    @Override
    public Iterator<Map<String, Serializable>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return pos < size;
    }

    @Override
    public Map<String, Serializable> next() {
        if (closed || pos == size) {
            throw new NoSuchElementException();
        }
        Map<String, Serializable> map = maps.get((int) pos);
        pos++;
        return map;
    }

}
