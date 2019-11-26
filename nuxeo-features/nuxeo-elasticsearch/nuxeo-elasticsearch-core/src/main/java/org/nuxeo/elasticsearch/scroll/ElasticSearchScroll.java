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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.scroll.RepositoryScroll;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public class ElasticSearchScroll extends RepositoryScroll {

    protected EsScrollResult esScroll;

    @Override
    public boolean fetch() {
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        if (esScroll == null) {
            esScroll = ess.scroll(new NxQueryBuilder(session).nxql(request.getQuery())
                                                             .limit(request.getSize())
                                                             .onlyElasticsearchResponse(),
                    request.getTimeout().toSeconds());
        } else {
            esScroll = ess.scroll(esScroll);
        }
        SearchHit[] hits = esScroll.getElasticsearchResponse().getHits().getHits();
        if (hits == null || hits.length == 0) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> getIds() {
        SearchHit[] hits = esScroll.getElasticsearchResponse().getHits().getHits();
        if (hits == null || hits.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(hits).map(SearchHit::getId).collect(Collectors.toList());
    }

    @Override
    public void close() {
        super.close();
        esScroll = null;
    }
}
