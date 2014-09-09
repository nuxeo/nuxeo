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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * @since 5.9.6
 */
public class VcsFetcher extends Fetcher {

    public VcsFetcher(CoreSession session, SearchResponse response,
            Map<String, String> repoNames) {
        super(session, response, repoNames);
    }

    @Override
    public DocumentModelListImpl fetchDocuments() {
        Map<String, List<String>> repoHits = getHitsPerRepository();
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
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
                    session.close();
                }
            }
        }
        sortResults(docs);
        DocumentModelListImpl ret = new DocumentModelListImpl(docs.size());
        if (!docs.isEmpty()) {
            ret.addAll(docs);
        }
        return ret;
    }

    private Map<String, List<String>> getHitsPerRepository() {
        Map<String, List<String>> ret = new HashMap<String, List<String>>();
        for (SearchHit hit : getResponse().getHits()) {
            String repoName = getRepoForIndex(hit.getIndex());
            List<String> docIds = ret.get(repoName);
            if (docIds == null) {
                docIds = new ArrayList<String>();
                ret.put(repoName, docIds);
            }
            docIds.add(hit.getId());
        }
        return ret;
    }

    private List<DocumentModel> fetchFromVcs(final List<String> ids,
            CoreSession session) throws ClientException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM Document WHERE ecm:uuid IN (");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(NXQL.escapeString(ids.get(i)));
            if (i < ids.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return session.query(sb.toString());
    }

    private void sortResults(List<DocumentModel> docs) {
        final List<String> ids = new ArrayList<String>();
        for (SearchHit hit : getResponse().getHits()) {
            ids.add(getRepoForIndex(hit.getIndex()) + hit.getId());
        }

        Collections.sort(docs, new Comparator<DocumentModel>() {
            @Override
            public int compare(DocumentModel a, DocumentModel b) {
                return ids.indexOf(a.getRepositoryName() + a.getId())
                        - ids.indexOf(b.getRepositoryName() + b.getId());
            }
        });

    }

}
