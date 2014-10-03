/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.elasticsearch.audit.pageprovider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONReader;
import org.nuxeo.runtime.api.Framework;

public class ESAuditPageProvider extends AbstractPageProvider<LogEntry>
        implements PageProvider<LogEntry> {

    private static final long serialVersionUID = 1L;

    protected SearchRequestBuilder searchBuilder;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String UICOMMENTS_PROPERTY = "generateUIComments";

    protected static String emptyQuery = "{ \"match_all\" : { }\n }";

    public String toString() {
        buildAuditQuery(true);
        StringBuffer sb = new StringBuffer();
        sb.append(searchBuilder.toString());
        return sb.toString();
    }

    protected void preprocessCommentsIfNeeded(List<LogEntry> entries) {
        Serializable preprocess = getProperties().get(UICOMMENTS_PROPERTY);
        
        if (preprocess != null
                && "true".equalsIgnoreCase(preprocess.toString())) {
            Object session = (CoreSession) getProperties().get(
                    CORE_SESSION_PROPERTY);
            if (session != null  && CoreSession.class.isAssignableFrom(session.getClass())  ) {
                CommentProcessorHelper cph = new CommentProcessorHelper((CoreSession)session);
                cph.processComments(entries);
            }    
        }
    }

    @Override
    public List<LogEntry> getCurrentPage() {

        buildAuditQuery(true);
        searchBuilder.setFrom((int) (getCurrentPageIndex() * pageSize));
        searchBuilder.setSize((int) getMinMaxPageSize());

        for (SortInfo sortInfo : getSortInfos()) {
            searchBuilder.addSort(sortInfo.getSortColumn(),
                    sortInfo.getSortAscending() ? SortOrder.ASC
                            : SortOrder.DESC);
        }

        SearchResponse searchResponse = searchBuilder.execute().actionGet();
        List<LogEntry> entries = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();

        // set total number of hits ?
        setResultsCount(hits.getTotalHits());

        for (SearchHit hit : hits) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        preprocessCommentsIfNeeded(entries);
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
            if (fixedPart==null || fixedPart.isEmpty()) {
                fixedPart = emptyQuery;
            }
            return fixedPart;
        }
    }

    protected boolean allowSimplePattern() {
        return true;
    }

    protected ESAuditBackend getESBackend() {
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        if (backend instanceof ESAuditBackend) {
            return (ESAuditBackend) backend;
        }
        throw new ClientRuntimeException(
                "Unable to use ESAuditPageProvider if audit service is not configured to run with ElasticSearch");
    }

    protected void buildAuditQuery(boolean includeSort) {
        PageProviderDefinition def = getDefinition();
        Object[] params = getParameters();

        if (def.getWhereClause() == null) {
            // Simple Pattern

            if (!allowSimplePattern()) {
                throw new UnsupportedOperationException(
                        "This page provider requires a explicit Where Clause");
            }
            String baseQuery = getESBackend().expandQueryVariables(
                    def.getPattern(), params);
            searchBuilder = getESBackend().buildQuery(baseQuery, null);
        } else {
            // Where clause based on DocumentModel
            String baseQuery = getESBackend().expandQueryVariables(
                    getFixedPart(), params);
            searchBuilder = getESBackend().buildSearchQuery(baseQuery,
                    def.getWhereClause().getPredicates(),
                    getSearchDocumentModel());
        }
    }

    public void refresh() {
        setCurrentPageOffset(0);
        super.refresh();
    }

    @Override
    public long getResultsCount() {
        return resultsCount;
    }

}
