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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * @since 6.0
 */
public class VcsFetcher extends Fetcher {

    private static final int CHUNK_SIZE = 100;

    public VcsFetcher(CoreSession session, SearchResponse response, Map<String, String> repoNames) {
        super(session, response, repoNames);
    }

    @Override
    public DocumentModelListImpl fetchDocuments() {
        Map<String, List<String>> repoHits = getHitsPerRepository();
        List<DocumentModel> docs = new ArrayList<>();
        String openSessionRepository = getSession().getRepositoryName();
        boolean closeSession;
        CoreSession session;
        for (String repo : repoHits.keySet()) {
            if (openSessionRepository.equals(repo)) {
                session = getSession();
                closeSession = false;
            } else {
                session = CoreInstance.openCoreSession(repo);
                closeSession = true;
            }
            try {
                docs.addAll(fetchFromVcs(repoHits.get(repo), session));
            } finally {
                if (closeSession) {
                    ((CloseableCoreSession) session).close();
                }
            }
        }
        sortResults(docs);
        addHighlights(docs);
        DocumentModelListImpl ret = new DocumentModelListImpl(docs.size());
        if (!docs.isEmpty()) {
            ret.addAll(docs);
        }
        return ret;
    }

    private Map<String, List<String>> getHitsPerRepository() {
        Map<String, List<String>> ret = new HashMap<>();
        for (SearchHit hit : getResponse().getHits()) {
            String repoName = getRepoForIndex(hit.getIndex());
            List<String> docIds = ret.computeIfAbsent(repoName, k -> new ArrayList<>());
            docIds.add(hit.getId());
        }
        return ret;
    }

    private List<DocumentModel> fetchFromVcs(List<String> ids, CoreSession session) {
        List<DocumentModel> ret = null;
        int size = ids.size();
        int start = 0;
        int end = Math.min(CHUNK_SIZE, size);
        boolean done = false;
        while (!done) {
            List<DocumentModel> docs = fetchFromVcsChunk(ids.subList(start, end), session);
            if (ret == null) {
                ret = docs;
            } else {
                ret.addAll(docs);
            }
            if (end >= ids.size()) {
                done = true;
            } else {
                start = end;
                end = Math.min(start + CHUNK_SIZE, size);
            }
        }
        return ret;
    }

    private List<DocumentModel> fetchFromVcsChunk(final List<String> ids, CoreSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM Document, Relation WHERE ecm:uuid IN (");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(NXQL.escapeString(ids.get(i)));
            if (i < ids.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return session.query(sb.toString());
    }

    private void addHighlights(List<DocumentModel> docs) {
        for (SearchHit hit : getResponse().getHits()) {
            for (DocumentModel doc : docs) {
                String docId = doc.getRepositoryName() + doc.getId();
                String hitId = getRepoForIndex(hit.getIndex()) + hit.getId();
                if (docId.equals(hitId)) {
                    // Add highlight if it exists
                    Map<String, HighlightField> esHighlights = hit.getHighlightFields();
                    if (!esHighlights.isEmpty()) {
                        Map<String, List<String>> fields = new HashMap<>();
                        for (Map.Entry<String, HighlightField> entry : esHighlights.entrySet()) {
                            String field = entry.getKey();
                            List<String> list = new ArrayList<>();
                            for (Text fragment : entry.getValue().getFragments()) {
                                list.add(fragment.toString());
                            }
                            fields.put(field, list);
                        }
                        doc.putContextData(PageProvider.HIGHLIGHT_CTX_DATA, (Serializable) fields);
                    }
                    break;
                }
            }
        }
    }

    private void sortResults(List<DocumentModel> docs) {
        final List<String> ids = new ArrayList<>();
        for (SearchHit hit : getResponse().getHits()) {
            ids.add(getRepoForIndex(hit.getIndex()) + hit.getId());
        }

        docs.sort(new Comparator<DocumentModel>() {
            @Override
            public int compare(DocumentModel a, DocumentModel b) {
                return ids.indexOf(a.getRepositoryName() + a.getId()) - ids.indexOf(b.getRepositoryName() + b.getId());
            }
        });

    }

}
