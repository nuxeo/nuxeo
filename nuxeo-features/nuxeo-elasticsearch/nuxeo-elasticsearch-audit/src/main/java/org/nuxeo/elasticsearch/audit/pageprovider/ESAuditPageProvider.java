/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */
package org.nuxeo.elasticsearch.audit.pageprovider;

import static org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider.DEFAULT_ES_MAX_RESULT_WINDOW_VALUE;
import static org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider.ES_MAX_RESULT_WINDOW_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ESAuditPageProvider extends AbstractPageProvider<LogEntry> implements PageProvider<LogEntry> {

    private static final long serialVersionUID = 1L;

    protected SearchRequest searchRequest;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String UICOMMENTS_PROPERTY = "generateUIComments";

    protected static String emptyQuery = "{ \"match_all\" : { }\n }";

    protected Long maxResultWindow;

    @Override
    public String toString() {
        buildAuditQuery(true);
        return searchRequest.toString();
    }

    protected CoreSession getCoreSession() {
        Object session = getProperties().get(CORE_SESSION_PROPERTY);
        if (session != null && session instanceof CoreSession) {
            return (CoreSession) session;
        }
        return null;
    }

    protected void preprocessCommentsIfNeeded(List<LogEntry> entries) {
        Serializable preprocess = getProperties().get(UICOMMENTS_PROPERTY);

        if (preprocess != null && "true".equalsIgnoreCase(preprocess.toString())) {
            CoreSession session = getCoreSession();
            if (session != null) {
                CommentProcessorHelper cph = new CommentProcessorHelper(session);
                cph.processComments(entries);
            }
        }
    }

    @Override
    public List<LogEntry> getCurrentPage() {

        buildAuditQuery(true);
        searchRequest.source().from((int) (getCurrentPageIndex() * pageSize));
        searchRequest.source().size((int) getMinMaxPageSize());

        for (SortInfo sortInfo : getSortInfos()) {
            searchRequest.source().sort(sortInfo.getSortColumn(),
                    sortInfo.getSortAscending() ? SortOrder.ASC : SortOrder.DESC);
        }
        SearchResponse searchResponse = getESBackend().search(searchRequest);
        List<LogEntry> entries = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();

        // set total number of hits ?
        setResultsCount(hits.getTotalHits());
        ObjectMapper mapper = new ObjectMapper();
        for (SearchHit hit : hits) {
            try {
                entries.add(mapper.readValue(hit.getSourceAsString(), LogEntryImpl.class));
            } catch (IOException e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        preprocessCommentsIfNeeded(entries);

        long t0 = System.currentTimeMillis();

        CoreSession session = getCoreSession();
        if (session != null) {
            // send event for statistics !
            fireSearchEvent(session.getPrincipal(), searchRequest.toString(), entries, System.currentTimeMillis() - t0);
        }

        return entries;
    }

    protected boolean isNonNullParam(Object[] val) {
        if (val == null) {
            return false;
        }
        for (Object v : val) {
            if (v != null) {
                if (v instanceof String) {
                    if (!((String) v).isEmpty()) {
                        return true;
                    }
                } else if (v instanceof String[]) {
                    if (((String[]) v).length > 0) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
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

    protected ESAuditBackend getESBackend() {
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        if (backend instanceof ESAuditBackend) {
            return (ESAuditBackend) backend;
        }
        throw new NuxeoException(
                "Unable to use ESAuditPageProvider if audit service is not configured to run with ElasticSearch");
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

            String baseQuery = getESBackend().expandQueryVariables(pattern, params);
            searchRequest = getESBackend().buildQuery(baseQuery, null);
        } else {

            // Add the quick filters clauses to the fixed part
            String fixedPart = getFixedPart();
            if (!StringUtils.isBlank(quickFiltersClause)) {
                fixedPart = (!StringUtils.isBlank(fixedPart))
                        ? NXQLQueryBuilder.appendClause(fixedPart, quickFiltersClause) : quickFiltersClause;
            }

            // Where clause based on DocumentModel
            String baseQuery = getESBackend().expandQueryVariables(fixedPart, params);
            searchRequest = getESBackend().buildSearchQuery(baseQuery, whereClause.getPredicates(),
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
     * Returns the max result window where the PP can navigate without raising Elasticsearch
     * QueryPhaseExecutionException. {@code from + size} must be less than or equal to this value.
     *
     * @since 9.2
     */
    public long getMaxResultWindow() {
        if (maxResultWindow == null) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            String maxResultWindowStr = cs.getProperty(ES_MAX_RESULT_WINDOW_PROPERTY,
                    DEFAULT_ES_MAX_RESULT_WINDOW_VALUE);
            try {
                maxResultWindow = Long.valueOf(maxResultWindowStr);
            } catch (NumberFormatException e) {
                log.warn(String.format(
                        "Invalid maxResultWindow property value: %s for page provider: %s, fallback to default.",
                        maxResultWindowStr, getName()));
                maxResultWindow = Long.valueOf(DEFAULT_ES_MAX_RESULT_WINDOW_VALUE);
            }
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
