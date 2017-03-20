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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.elasticsearch.io.DocumentModelReaders;

/**
 * @since 6.0
 */
public class EsFetcher extends Fetcher {

    public EsFetcher(CoreSession session, SearchResponse response, Map<String, String> repoNames) {
        super(session, response, repoNames);
    }

    @Override
    public DocumentModelListImpl fetchDocuments() {
        DocumentModelListImpl ret = new DocumentModelListImpl(getResponse().getHits().getHits().length);
        DocumentModel doc;
        String sid = getSession().getSessionId();
        for (SearchHit hit : getResponse().getHits()) {
            // TODO: this does not work on multi repo
            doc = DocumentModelReaders.fromSource(hit.getSource()).sid(sid).getDocumentModel();
            // Add highlight if it exists
            Map<String, HighlightField> esHighlights = hit.highlightFields();
            if (!esHighlights.isEmpty()) {
                Map<String, List<String>> fields = new HashMap<>();
                for (String field : esHighlights.keySet()) {
                    fields.put(field, new ArrayList<>());
                    for (Text fragment : esHighlights.get(field).getFragments()) {
                        fields.get(field).add(fragment.toString());
                    }
                }
                doc.putContextData(HIGHLIGHT_CTX_DATA, (Serializable) fields);
            }
            ret.add(doc);
        }
        return ret;
    }
}
