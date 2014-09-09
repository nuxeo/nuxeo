/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.fetcher;

import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 5.9.6
 */
public abstract class Fetcher {

    private final CoreSession session;
    private final SearchResponse response;
    private final Map<String, String> repoNames;

    public Fetcher(CoreSession session, SearchResponse response,
            Map<String, String> repoNames) {
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
