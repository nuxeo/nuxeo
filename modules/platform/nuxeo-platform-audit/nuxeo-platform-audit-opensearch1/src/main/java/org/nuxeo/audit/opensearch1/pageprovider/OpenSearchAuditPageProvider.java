/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.audit.opensearch1.pageprovider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.opensearch1.OpenSearchAuditBackend;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.sort.SortOrder;

public class OpenSearchAuditPageProvider extends AbstractPageProvider<LogEntry> implements PageProvider<LogEntry> {

    private static final Logger log = LogManager.getLogger(OpenSearchAuditPageProvider.class);

    private static final long serialVersionUID = 1L;

    // @since 9.2
    public static final String OS_MAX_RESULT_WINDOW_PROPERTY = "org.nuxeo.elasticsearch.provider.maxResultWindow";

    // This is the default OpenSearch index.max_result_window
    public static final long DEFAULT_OS_MAX_RESULT_WINDOW_VALUE = 10000;

    /**
     * @deprecated since 2025.20, use {@link PageProviderSpec#CORE_SESSION_PROPERTY} instead
     */
    @Deprecated(since = "2025.20", forRemoval = true)
    public static final String CORE_SESSION_PROPERTY = PageProviderSpec.CORE_SESSION_PROPERTY;

    /**
     * @deprecated since 2025.0, unused
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String UICOMMENTS_PROPERTY = "generateUIComments";

    protected static String emptyQuery = "{ \"match_all\" : { }\n }";

    protected SearchRequest searchRequest;

    protected Long maxResultWindow;

    @Override
    public String toString() {
        buildAuditQuery(true);
        return searchRequest.toString();
    }

    protected CoreSession getCoreSession() {
        if (getProperties().get(CORE_SESSION_PROPERTY) instanceof CoreSession session) {
            return session;
        }
        return null;
    }

    @Override
    public List<LogEntry> getCurrentPage() {

        buildAuditQuery(true);
        searchRequest.source().from((int) (getCurrentPageIndex() * pageSize));
        searchRequest.source().size((int) getMinMaxPageSize());

        for (SortInfo sortInfo : getSortInfos()) {
            searchRequest.source()
                         .sort(sortInfo.getSortColumn(), sortInfo.getSortAscending() ? SortOrder.ASC : SortOrder.DESC);
        }
        SearchResponse searchResponse = getOSBackend().search(searchRequest);
        List<LogEntry> entries = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();

        // set total number of hits ?
        setResultsCount(hits.getTotalHits().value);
        for (SearchHit hit : hits) {
            try {
                entries.add(MarshallerHelper.jsonToObject(LogEntry.class, hit.getSourceAsString(),
                        RenderingContext.CtxBuilder.get()));
            } catch (IOException e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }

        long t0 = System.currentTimeMillis();

        CoreSession session = getCoreSession();
        if (session != null) {
            // send event for statistics !
            fireSearchEvent(session.getPrincipal(), searchRequest.toString(), entries, System.currentTimeMillis() - t0);
        }

        return entries;
    }

    protected String getFixedPart() {
        if (getDefinition().getWhereClause() == null) {
            return null;
        } else {
            String fixedPart = getDefinition().getWhereClause().getFixedPart();
            if (fixedPart == null || fixedPart.isEmpty()) {
                fixedPart = emptyQuery;
            }
            return fixedPart;
        }
    }

    protected boolean allowSimplePattern() {
        return true;
    }

    protected OpenSearchAuditBackend getOSBackend() {
        if (Framework.getService(AuditBackend.class) instanceof OpenSearchAuditBackend backend) {
            return backend;
        }
        throw new NuxeoException(
                "Unable to use OpenSearchAuditPageProvider if AuditService is not configured to run with OpenSearch");
    }

    protected void buildAuditQuery(boolean includeSort) {
        PageProviderDefinition def = getDefinition();
        Object[] params = getParameters();
        List<QuickFilter> quickFilters = getQuickFilters();
        String quickFiltersClause = "";

        if (quickFilters != null && !quickFilters.isEmpty()) {
            for (QuickFilter quickFilter : quickFilters) {
                String clause = quickFilter.getClause();
                if (!quickFiltersClause.isEmpty() && clause != null) {
                    quickFiltersClause = NXQLQueryBuilder.appendClause(quickFiltersClause, clause);
                } else {
                    quickFiltersClause = clause != null ? clause : "";
                }
            }
        }

        WhereClauseDefinition whereClause = def.getWhereClause();
        if (whereClause == null) {
            // Simple Pattern

            if (!allowSimplePattern()) {
                throw new UnsupportedOperationException("This page provider requires a explicit Where Clause");
            }
            String originalPattern = def.getPattern();
            String pattern = quickFiltersClause.isEmpty() ? originalPattern
                    : StringUtils.containsIgnoreCase(originalPattern, " WHERE ")
                            ? NXQLQueryBuilder.appendClause(originalPattern, quickFiltersClause)
                            : originalPattern + " WHERE " + quickFiltersClause;

            String baseQuery = getOSBackend().expandQueryVariables(pattern, params);
            searchRequest = getOSBackend().buildQuery(baseQuery, null);
        } else {

            // Add the quick filters clauses to the fixed part
            String fixedPart = getFixedPart();
            if (!StringUtils.isBlank(quickFiltersClause)) {
                fixedPart = (!StringUtils.isBlank(fixedPart))
                        ? NXQLQueryBuilder.appendClause(fixedPart, quickFiltersClause)
                        : quickFiltersClause;
            }

            // Where clause based on DocumentModel
            String baseQuery = getOSBackend().expandQueryVariables(fixedPart, params);
            searchRequest = getOSBackend().buildSearchQuery(baseQuery, whereClause.getPredicates(),
                    getSearchDocumentModel());
        }
    }

    @Override
    public void refresh() {
        setCurrentPageOffset(0);
        super.refresh();
    }

    @Override
    public long getResultsCount() {
        return resultsCount;
    }

    @Override
    public List<SortInfo> getSortInfos() {

        // because ContentView can reuse PageProVider without redefining columns
        // ensure compat for ContentView configured with JPA log.* sort syntax
        List<SortInfo> sortInfos = super.getSortInfos();
        for (SortInfo si : sortInfos) {
            if (si.getSortColumn().startsWith("log.")) {
                si.setSortColumn(si.getSortColumn().substring(4));
            }
        }
        return sortInfos;
    }

    @Override
    public boolean isLastPageAvailable() {
        if ((getResultsCount() + getPageSize()) <= getMaxResultWindow()) {
            return super.isNextPageAvailable();
        }
        return false;
    }

    @Override
    public boolean isNextPageAvailable() {
        if ((getCurrentPageOffset() + 2 * getPageSize()) <= getMaxResultWindow()) {
            return super.isNextPageAvailable();
        }
        return false;
    }

    @Override
    public long getPageLimit() {
        return getMaxResultWindow() / getPageSize();
    }

    /**
     * Returns the max result window where the PP can navigate without raising OpenSearch QueryPhaseExecutionException.
     * {@code from + size} must be less than or equal to this value.
     *
     * @since 9.2
     */
    public long getMaxResultWindow() {
        if (maxResultWindow == null) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            maxResultWindow = cs.getLong(OS_MAX_RESULT_WINDOW_PROPERTY, DEFAULT_OS_MAX_RESULT_WINDOW_VALUE);
        }
        return maxResultWindow;
    }

    /**
     * Set the max result window where the PP can navigate, for testing purpose.
     *
     * @since 9.2
     */
    public void setMaxResultWindow(long maxResultWindow) {
        this.maxResultWindow = maxResultWindow;
    }

}
