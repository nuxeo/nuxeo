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
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.elasticsearch.io.DocumentModelReaders;

/**
 * @since 6.0
 */
public class EsFetcher extends Fetcher {

    public EsFetcher(CoreSession session, SearchResponse response,
            Map<String, String> repoNames) {
        super(session, response, repoNames);
    }

    @Override
    public DocumentModelListImpl fetchDocuments() {
        DocumentModelListImpl ret = new DocumentModelListImpl(getResponse()
                .getHits().getHits().length);
        DocumentModel doc;
        String sid = getSession().getSessionId();
        for (SearchHit hit : getResponse().getHits()) {
            // TODO: this does not work on multi repo
            doc = DocumentModelReaders.fromSource(hit.getSource()).sid(sid)
                    .getDocumentModel();
            ret.add(doc);
        }
        return ret;
    }
}
