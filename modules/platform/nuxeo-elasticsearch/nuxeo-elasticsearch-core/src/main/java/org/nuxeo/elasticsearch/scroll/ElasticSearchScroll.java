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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.scroll;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.scroll.RepositoryScroll;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public class ElasticSearchScroll extends RepositoryScroll {

    // elastic keep alive must be less than 1d
    protected static final long MAX_ES_KEEP_ALIVE_SECONDS = 23 * 3600L;

    protected EsScrollResult esScroll;

    @Override
    public boolean hasNext() {
        if (hasNextResult == null) {
            hasNextResult = fetch();
        }
        return hasNextResult;
    }

    @Override
    protected boolean fetch() {
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        try {
            if (esScroll == null) {
                esScroll = ess.scroll(new NxQueryBuilder(session).nxql(request.getQuery())
                                                                 .limit(request.getSize())
                                                                 .onlyElasticsearchResponse(),
                        getKeepAlive());
            } else {
                esScroll = ess.scroll(esScroll);
            }
        } catch (ElasticsearchException e) {
            throw new NuxeoException("Elastic scroll failure on request: " + request, e);
        }
        SearchHit[] hits = esScroll.getElasticsearchResponse().getHits().getHits();
        return hits != null && hits.length > 0;
    }

    protected long getKeepAlive() {
        long keepAlive = request.getTimeout().toSeconds();
        if (keepAlive <= 0 || keepAlive > MAX_ES_KEEP_ALIVE_SECONDS) {
            return MAX_ES_KEEP_ALIVE_SECONDS;
        }
        return keepAlive;
    }

    @Override
    public List<String> next() {
        if (hasNextResult == null) {
            hasNextResult = fetch();
        }
        if (!hasNextResult) {
            throw new NoSuchElementException();
        }
        hasNextResult = null;
        SearchHit[] hits = esScroll.getElasticsearchResponse().getHits().getHits();
        return Arrays.stream(hits).map(SearchHit::getId).collect(Collectors.toList());
    }

    @Override
    public void close() {
        super.close();
        if (esScroll != null) {
            ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
            ess.clearScroll(esScroll);
        }
        esScroll = null;
    }
}
