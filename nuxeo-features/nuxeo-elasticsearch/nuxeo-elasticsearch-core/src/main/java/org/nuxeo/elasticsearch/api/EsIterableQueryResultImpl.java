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
package org.nuxeo.elasticsearch.api;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.elasticsearch.core.EsSearchHitConverter;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;

/**
 * Iterable query result of results of an ElasticSearch scroll query and next ones.
 * <p>
 * Queries ElasticSearch when there's no more result in current response and there's more in cluster.
 * <p>
 * For better performance use {@link NxQueryBuilder#onlyElasticsearchResponse()} for the first scroll requests.
 *
 * @since 8.4
 */
public class EsIterableQueryResultImpl implements IterableQueryResult, Iterator<Map<String, Serializable>> {

    private final ElasticSearchService searchService;

    private EsScrollResult scrollResult;

    private final EsSearchHitConverter converter;

    private final long size;

    private boolean closed;

    private long pos;

    private int relativePos;

    public EsIterableQueryResultImpl(ElasticSearchService searchService, EsScrollResult scrollResult) {
        assert !scrollResult.getQueryBuilder().getSelectFieldsAndTypes().isEmpty();
        this.searchService = searchService;
        this.scrollResult = scrollResult;
        this.converter = new EsSearchHitConverter(scrollResult.getQueryBuilder().getSelectFieldsAndTypes());
        this.size = scrollResult.getElasticsearchResponse().getHits().getTotalHits();
    }

    @Override
    public void close() {
        if (!closed) {
            searchService.clearScroll(scrollResult);
            closed = true;
            pos = -1;
        }
    }

    @Override
    public boolean isLife() {
        return mustBeClosed();
    }

    @Override
    public boolean mustBeClosed() {
        return true;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public long pos() {
        return pos;
    }

    @Override
    public void skipTo(long pos) {
        checkNotClosed();
        if (pos < this.pos) {
            throw new IllegalArgumentException("Cannot go back in Iterable.");
        } else if (pos > size) {
            pos = size;
        } else {
            while (pos > this.pos) {
                nextHit();
            }
        }
        this.pos = pos;
    }

    @Override
    public Iterator<Map<String, Serializable>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        checkNotClosed();
        return pos < size;
    }

    @Override
    public Map<String, Serializable> next() {
        checkNotClosed();
        if (pos == size) {
            throw new NoSuchElementException();
        }
        SearchHit hit = nextHit();
        return converter.convert(hit);
    }

    private SearchHit nextHit() {
        if (relativePos == scrollResult.getElasticsearchResponse().getHits().getHits().length) {
            // Retrieve next scroll
            scrollResult = searchService.scroll(scrollResult);
            relativePos = 0;
        }
        SearchHit hit = scrollResult.getElasticsearchResponse().getHits().getAt(relativePos);
        relativePos++;
        pos++;
        return hit;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Query results iterator closed.");
        }
    }

}
