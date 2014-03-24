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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.SearchHit;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;

public class ElasticSearchNativePageProvider extends
        AbstractPageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    protected static final Log log = LogFactory
            .getLog(ElasticSearchNativePageProvider.class);

    protected List<DocumentModel> currentPageDocuments;

    @Override
    public List<DocumentModel> getCurrentPage() {
        // use a cache
        if (currentPageDocuments != null) {
            return currentPageDocuments;
        }
        if (log.isDebugEnabled()) {
            log.debug(String
                    .format("Perform query for provider '%s': with pageSize=%d, offset=%d",
                            getName(), getMinMaxPageSize(),
                            getCurrentPageOffset()));
        }
        // Build the ES query
        SearchRequestBuilder builder = makeQueryBuilder();
        // Execute the ES query
        if (log.isDebugEnabled()) {
            log.debug("Search query: " + builder.toString());
        }
        SearchResponse searchResponse = builder.execute().actionGet();
        if (log.isDebugEnabled()) {
            log.debug("Results: " + searchResponse.toString());
        }
        // Get the list of ids
        setResultsCount(searchResponse.getHits().getTotalHits());
        List<String> ids = new ArrayList<String>();
        for (SearchHit hit : searchResponse.getHits()) {
            ids.add(hit.getId());
        }
        // Fetch the document model
        currentPageDocuments = new ArrayList<DocumentModel>();
        if (!ids.isEmpty()) {
            try {
                currentPageDocuments.addAll(fetchDocuments(ids,
                        getCoreSession()));
            } catch (ClientException e) {
                log.error(e);
            }
        }
        return currentPageDocuments;
    }

    protected SearchRequestBuilder makeQueryBuilder() {
        Principal principal = getCoreSession().getPrincipal();
        ElasticSearchService ess = Framework
                .getLocalService(ElasticSearchService.class);
        SearchRequestBuilder ret = ess.getClient()
                .prepareSearch(ElasticSearchComponent.MAIN_IDX)
                .setTypes(ElasticSearchComponent.NX_DOCUMENT)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setFetchSource(ElasticSearchComponent.ID_FIELD, null)
                .setFrom((int) getCurrentPageOffset())
                .setSize((int) getMinMaxPageSize());
        try {
            SortInfo[] sortArray = null;
            if (sortInfos != null) {
                sortArray = sortInfos.toArray(new SortInfo[] {});
            }
            PageProviderDefinition def = getDefinition();
            if (def.getWhereClause() == null) {
                ElasticSearchQueryBuilder.makeQuery(ret, principal, def.getPattern(),
                        getParameters(), def.getQuotePatternParameters(),
                        def.getEscapePatternParameters(), sortArray);
            } else {
                DocumentModel searchDocumentModel = getSearchDocumentModel();
                if (searchDocumentModel == null) {
                    throw new ClientException(String.format(
                            "Cannot build query of provider '%s': "
                                    + "no search document model is set",
                            getName()));
                }
                ElasticSearchQueryBuilder.makeQuery(ret, principal, searchDocumentModel,
                        def.getWhereClause(), getParameters(), sortArray);
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        // TODO: Add primarytype filter
        // TODO: Add acl filtering
        return ret;
    }

    @Override
    protected void pageChanged() {
        currentPageDocuments = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        currentPageDocuments = null;
        super.refresh();
    }

    /**
     * Fetch document models from VCS, return results in the same order.
     *
     */
    protected List<DocumentModel> fetchDocuments(final List<String> ids,
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
        DocumentModelList ret = session.query(sb.toString());
        // Order the results
        Collections.sort(ret, new Comparator<DocumentModel>() {
            @Override
            public int compare(DocumentModel a, DocumentModel b) {
                return ids.indexOf(a.getId()) - ids.indexOf(b.getId());
            }
        });
        return ret;
    }

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props
                .get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new ClientRuntimeException("cannot find core session");
        }
        return coreSession;
    }

}
