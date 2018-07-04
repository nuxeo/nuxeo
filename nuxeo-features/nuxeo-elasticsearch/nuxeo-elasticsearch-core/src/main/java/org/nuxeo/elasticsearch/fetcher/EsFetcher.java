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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.elasticsearch.io.DocumentModelReaders;

/**
 * @since 6.0
 */
public class EsFetcher extends Fetcher {

    protected final HitDocConsumer consumer;

    public EsFetcher(CoreSession session, SearchResponse response, Map<String, String> repoNames) {
        super(session, response, repoNames);
        this.consumer = null;
    }

    /**
     * @since 10.2
     */
    public EsFetcher(CoreSession session, SearchResponse response, Map<String, String> repoNames, HitDocConsumer consumer) {
        super(session, response, repoNames);
        this.consumer = consumer;
    }

    @Override
    public DocumentModelListImpl fetchDocuments() {
        DocumentModelListImpl ret = new DocumentModelListImpl(getResponse().getHits().getHits().length);
        DocumentModel doc;
        String sid = getSession().getSessionId();
        for (SearchHit hit : getResponse().getHits()) {
            // TODO: this does not work on multi repo
            doc = DocumentModelReaders.fromSource(hit.getSourceAsMap()).sid(sid).getDocumentModel();

            if (doc != null && consumer != null) {
                consumer.accept(hit, doc);
            }

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
            ret.add(doc);
        }
        return ret;
    }

    /**
     * Consumes both a SearchHit and DocumentModel.
     * @since 10.2
     */
    @FunctionalInterface
    public interface HitDocConsumer extends BiConsumer<SearchHit, DocumentModel> {

    }
}
