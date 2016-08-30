/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.elasticsearch.core;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.elasticsearch.action.search.SearchResponse;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.schema.types.Type;

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
        return new EsSearchHitConverter(selectFieldsAndTypes).convert(response.getHits().getHits());
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
    public boolean mustBeClosed() {
        return false; // holds no resources
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
