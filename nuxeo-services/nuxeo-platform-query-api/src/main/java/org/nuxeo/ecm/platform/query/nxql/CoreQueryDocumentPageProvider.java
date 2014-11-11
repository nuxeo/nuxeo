/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.nxql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider performing a query on a core session.
 * <p>
 * It builds the query at each call so that it can refresh itself when the
 * query changes.
 * <p>
 * The page provider property named {@link #CORE_SESSION_PROPERTY} is used to
 * pass the {@link CoreSession} instance that will perform the query. The
 * optional property {@link #CHECK_QUERY_CACHE_PROPERTY} can be set to "true"
 * to avoid performing the query again if it did not change.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class CoreQueryDocumentPageProvider extends
        AbstractPageProvider<DocumentModel> {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String MAX_RESULTS_PROPERTY = "maxResults";

    // Special maxResults value used for navigation, can be tuned
    public static final String DEFAULT_NAVIGATION_RESULTS_KEY = "DEFAULT_NAVIGATION_RESULTS";

    // Special maxResults value that means same as the page size
    public static final String PAGE_SIZE_RESULTS_KEY = "PAGE_SIZE";

    public static final String DEFAULT_NAVIGATION_RESULTS_PROPERTY = "org.nuxeo.ecm.platform.query.nxql.defaultNavigationResults";

    public static final String DEFAULT_NAVIGATION_RESULTS_VALUE = "200";

    public static final String CHECK_QUERY_CACHE_PROPERTY = "checkQueryCache";

    private static final Log log = LogFactory.getLog(CoreQueryDocumentPageProvider.class);

    private static final long serialVersionUID = 1L;

    protected String query;

    protected List<DocumentModel> currentPageDocuments;

    protected Long maxResults;

    @Override
    public List<DocumentModel> getCurrentPage() {
        checkQueryCache();
        if (currentPageDocuments == null) {
            error = null;
            errorMessage = null;

            CoreSession coreSession = getCoreSession();
            if (query == null) {
                buildQuery(coreSession);
            }
            if (query == null) {
                throw new ClientRuntimeException(String.format(
                        "Cannot perform null query: check provider '%s'",
                        getName()));
            }

            currentPageDocuments = new ArrayList<DocumentModel>();

            try {

                long minMaxPageSize = getMinMaxPageSize();

                long offset = getCurrentPageOffset();
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Perform query for provider '%s': '%s' with pageSize=%s, offset=%s",
                            getName(), query, Long.valueOf(minMaxPageSize),
                            Long.valueOf(offset)));
                }

                DocumentModelList docs;
                if (getMaxResults() > 0) {
                    docs = coreSession.query(query, getFilter(),
                            minMaxPageSize, offset, getMaxResults());
                } else {
                    // use a totalCount=true instead of countUpTo=-1 to enable
                    // global limitation described in NXP-9381
                    docs = coreSession.query(query, getFilter(),
                            minMaxPageSize, offset, true);
                }
                long resultsCount = docs.totalSize();
                if (resultsCount < 0) {
                    // results count is truncated
                    setResultsCount(UNKNOWN_SIZE_AFTER_QUERY);
                } else {
                    setResultsCount(resultsCount);
                }
                currentPageDocuments = docs;

                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Performed query for provider '%s': got %s hits (limit %s)",
                            getName(), Long.valueOf(resultsCount),
                            Long.valueOf(getMaxResults())));
                }

                // refresh may have triggered display of an empty page => go
                // back to first page or forward to last page depending on
                // results count and page size
                long pageSize = getPageSize();
                if (pageSize != 0) {
                    if (offset != 0 && currentPageDocuments.size() == 0) {
                        if (resultsCount == 0) {
                            // fetch first page directly
                            if (log.isDebugEnabled()) {
                                log.debug(String.format(
                                        "Current page %s is not the first one but "
                                                + "shows no result and there are "
                                                + "no results => rewind to first page",
                                        Long.valueOf(getCurrentPageIndex())));
                            }
                            firstPage();
                        } else {
                            // fetch last page
                            if (log.isDebugEnabled()) {
                                log.debug(String.format(
                                        "Current page %s is not the first one but "
                                                + "shows no result and there are "
                                                + "%s results => fetch last page",
                                        Long.valueOf(getCurrentPageIndex()),
                                        Long.valueOf(resultsCount)));
                            }
                            lastPage();
                        }
                        // fetch current page again
                        getCurrentPage();
                    }
                }

                if (getResultsCount() < 0) {
                    // additional info to handle next page when results count
                    // is unknown
                    if (currentPageDocuments != null
                            && currentPageDocuments.size() > 0) {
                        int higherNonEmptyPage = getCurrentHigherNonEmptyPageIndex();
                        int currentFilledPage = Long.valueOf(
                                getCurrentPageIndex()).intValue();
                        if ((docs.size() >= getPageSize())
                                && (currentFilledPage > higherNonEmptyPage)) {
                            setCurrentHigherNonEmptyPageIndex(currentFilledPage);
                        }
                    }
                }
            } catch (Exception e) {
                error = e;
                errorMessage = e.getMessage();
                log.warn(e.getMessage(), e);
            }
        }
        return currentPageDocuments;
    }

    protected void buildQuery(CoreSession coreSession) {
        try {
            SortInfo[] sortArray = null;
            if (sortInfos != null) {
                sortArray = sortInfos.toArray(new SortInfo[] {});
            }
            String newQuery;
            PageProviderDefinition def = getDefinition();
            if (def.getWhereClause() == null) {
                newQuery = NXQLQueryBuilder.getQuery(def.getPattern(),
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
                newQuery = NXQLQueryBuilder.getQuery(searchDocumentModel,
                        def.getWhereClause(), getParameters(), sortArray);
            }

            if (query != null && newQuery != null && !newQuery.equals(query)) {
                // query has changed => refresh
                refresh();
            }
            query = newQuery;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void checkQueryCache() {
        // maybe handle refresh of select page according to query
        Map<String, Serializable> props = getProperties();
        if (props.containsKey(CHECK_QUERY_CACHE_PROPERTY)
                && Boolean.TRUE.equals(Boolean.valueOf((String) props.get(CHECK_QUERY_CACHE_PROPERTY)))) {
            CoreSession coreSession = getCoreSession();
            buildQuery(coreSession);
        }
    }

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new ClientRuntimeException("cannot find core session");
        }
        return coreSession;
    }

    /**
     * Returns the maximum number of results or
     * <code>0<code> if there is no limit.
     *
     * @since 5.6
     */
    public long getMaxResults() {
        if (maxResults == null) {
            maxResults = Long.valueOf(0);
            String maxResultsStr = (String) getProperties().get(
                    MAX_RESULTS_PROPERTY);
            if (maxResultsStr != null) {
                if (DEFAULT_NAVIGATION_RESULTS_KEY.equals(maxResultsStr)) {
                    maxResultsStr = Framework.getProperty(
                            DEFAULT_NAVIGATION_RESULTS_PROPERTY,
                            DEFAULT_NAVIGATION_RESULTS_VALUE);
                } else if (PAGE_SIZE_RESULTS_KEY.equals(maxResultsStr)) {
                    maxResultsStr = Long.valueOf(getPageSize()).toString();
                }
                try {
                    maxResults = Long.valueOf(maxResultsStr);
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Invalid maxResults property value: %s for page provider: %s, fallback to unlimited.",
                            maxResultsStr, getName()));
                }
            }
        }
        return maxResults.longValue();
    }

    /**
     * Returns the page limit. The n first page we know they exist. We don't
     * compute the number of page beyond this limit.
     *
     * @since 5.8
     */
    @Override
    public long getPageLimit() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            return 0;
        }
        return getMaxResults() / pageSize;
    }

    /**
     * Sets the maximum number of result elements.
     *
     * @since 5.6
     */
    public void setMaxResults(long maxResults) {
        this.maxResults = Long.valueOf(maxResults);
    }

    @Override
    public PageSelections<DocumentModel> getCurrentSelectPage() {
        checkQueryCache();
        return super.getCurrentSelectPage();
    }

    public String getCurrentQuery() {
        return query;
    }

    protected Filter getFilter() {
        return null;
    }

    @Override
    protected void pageChanged() {
        currentPageDocuments = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        query = null;
        currentPageDocuments = null;
        super.refresh();
    }

}
