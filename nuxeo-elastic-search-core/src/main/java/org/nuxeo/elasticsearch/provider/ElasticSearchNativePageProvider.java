/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

public class ElasticSearchNativePageProvider extends AbstractPageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    protected static final Log log = LogFactory
            .getLog(ElasticSearchNativePageProvider.class);

    protected List<DocumentModel> currentPageDocuments;

    @Override
    public List<DocumentModel> getCurrentPage() {

        if (currentPageDocuments != null) {
            return currentPageDocuments;
        }

        currentPageDocuments = new ArrayList<DocumentModel>();

        long minMaxPageSize = getMinMaxPageSize();
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Perform query for provider '%s': with pageSize=%d, offset=%d",
                    getName(), minMaxPageSize, getCurrentPageOffset()));
        }

        String query = buildQuery();

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        SearchResponse searchResponse = ess.getClient()
                .prepareSearch(ElasticSearchComponent.MAIN_IDX).setTypes("doc")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.queryString(query)).setFrom(0).setSize(60)
                .execute().actionGet();

        setResultsCount(searchResponse.getHits().getTotalHits());
        List<String> ids = new ArrayList<String>();
        for (SearchHit hit : searchResponse.getHits()) {
            ids.add(hit.getId());
        }
        if (!ids.isEmpty()) {
            try {
                currentPageDocuments.addAll(fetchDocuments(ids, getCoreSession()));
            } catch (ClientException e) {
                log.error(e);
            }
        }

        return currentPageDocuments;
    }

    protected String buildQuery() {
        return getDefinition().getPattern();
    }

    protected List<DocumentModel> fetchDocuments(List<String> ids, CoreSession session)
            throws ClientException {
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

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new ClientRuntimeException("cannot find core session");
        }
        return coreSession;
    }

}
