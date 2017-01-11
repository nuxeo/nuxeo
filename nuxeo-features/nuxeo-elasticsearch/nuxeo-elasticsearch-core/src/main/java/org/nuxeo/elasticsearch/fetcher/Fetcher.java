/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.fetcher;

import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 6.0
 */
public abstract class Fetcher {

    protected static final String HIGHLIGHT_CTX_DATA = "highlight";

    private final CoreSession session;

    private final SearchResponse response;

    private final Map<String, String> repoNames;

    public Fetcher(CoreSession session, SearchResponse response, Map<String, String> repoNames) {
        this.session = session;
        this.response = response;
        this.repoNames = repoNames;
    }

    protected CoreSession getSession() {
        return session;
    }

    protected SearchResponse getResponse() {
        return response;
    }

    protected String getRepoForIndex(String indexName) {
        if (repoNames == null) {
            return null;
        }
        return repoNames.get(indexName);
    }

    abstract public DocumentModelListImpl fetchDocuments();

}
