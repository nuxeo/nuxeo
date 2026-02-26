/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.scroll.RepositoryScroll;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public class SearchServiceScroll extends RepositoryScroll {

    public static final String SEARCH_INDEX_PARAMETER_KEY = "nuxeo.search.client.default.scroller.index.name";

    private static final String INDEX_OPTION = "searchIndex";

    protected SearchResponse response;

    protected SearchIndex searchIndex;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        super.init(request, options);
        if (options == null || !options.containsKey(INDEX_OPTION)) {
            throw new IllegalArgumentException("Invalid SearchServiceScroll: 'index' option is required");
        }
        String index = options.get(INDEX_OPTION);
        SearchService service = Framework.getService(SearchService.class);
        searchIndex = service.getSearchIndex(index);
        if (!this.request.getRepository().equals(searchIndex.repository())) {
            throw new IllegalArgumentException("SearchServiceScroll invalid index " + searchIndex + " for repository: "
                    + this.request.getRepository());
        }
    }

    @Override
    public boolean hasNext() {
        if (hasNextResult == null) {
            hasNextResult = fetch();
        }
        return hasNextResult;
    }

    @Override
    protected boolean fetch() {
        SearchService service = Framework.getService(SearchService.class);
        if (response == null) {
            response = service.search(SearchQuery.builder(request.getQuery(), session.getPrincipal())
                                                 .searchIndex(searchIndex)
                                                 .scrollSize(request.getSize())
                                                 .scrollKeepAlive(request.getTimeout())
                                                 .build());
        } else {
            response = service.searchScroll(response.getScrollContext());
        }
        return response.getHitsCount() > 0;
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
        return response.getHits().stream().map(SearchHit::getDocId).toList();
    }

    @Override
    public void close() {
        super.close();
        if (response != null) {
            var scrollContext = response.getScrollContext();
            if (scrollContext != null) {
                SearchService service = Framework.getService(SearchService.class);
                service.clearSearchScroll(scrollContext);
            }
        }
        response = null;
    }
}
