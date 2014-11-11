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
 */
package org.nuxeo.ecm.platform.query.nxql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageSelections;

/**
 * Page provider performing a queryAndFetch on a core session.
 * <p>
 * It builds the query at each call so that it can refresh itself when the
 * query changes.
 * <p>
 * TODO: describe needed properties
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class CoreQueryAndFetchPageProvider extends
        AbstractPageProvider<Map<String, Serializable>> {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String CHECK_QUERY_CACHE_PROPERTY = "checkQueryCache";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CoreQueryDocumentPageProvider.class);

    protected String query;

    protected List<Map<String, Serializable>> currentItems;

    @Override
    public List<Map<String, Serializable>> getCurrentPage() {
        checkQueryCache();
        if (currentItems == null) {
            errorMessage = null;
            error = null;

            if (query == null) {
                buildQuery();
            }
            if (query == null) {
                throw new ClientRuntimeException(String.format(
                        "Cannot perform null query: check provider '%s'",
                        getName()));
            }

            currentItems = new ArrayList<Map<String, Serializable>>();

            Map<String, Serializable> props = getProperties();
            CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
            if (coreSession == null) {
                throw new ClientRuntimeException("cannot find core session");
            }

            IterableQueryResult result = null;
            try {

                long minMaxPageSize = getMinMaxPageSize();

                long offset = getCurrentPageOffset();
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Perform query for provider '%s': '%s' with pageSize=%s, offset=%s",
                            getName(), query, Long.valueOf(minMaxPageSize),
                            Long.valueOf(offset)));
                }

                result = coreSession.queryAndFetch(query, NXQL.NXQL);
                long resultsCount = result.size();
                setResultsCount(resultsCount);
                if (offset < resultsCount) {
                    result.skipTo(offset);
                }

                Iterator<Map<String, Serializable>> it = result.iterator();
                int pos = 0;
                while (it.hasNext() && pos < minMaxPageSize) {
                    pos += 1;
                    Map<String, Serializable> item = it.next();
                    currentItems.add(item);
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Performed query for provider '%s': got %s hits",
                            getName(), Long.valueOf(resultsCount)));
                }

                // refresh may have triggered display of an empty page => go
                // back to first page or forward to last page depending on
                // results count and page size
                long pageSize = getPageSize();
                if (pageSize != 0) {
                    if (offset != 0 && currentItems.size() == 0) {
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

            } catch (ClientException e) {
                errorMessage = e.getMessage();
                error = e;
                log.warn(e.getMessage(), e);
            } finally {
                if (result != null) {
                    result.close();
                }
            }
        }

        return currentItems;
    }

    protected void buildQuery() {
        try {
            PageProviderDefinition def = getDefinition();
            String originalQuery = def.getPattern();

            SortInfo[] sortArray = null;
            if (sortInfos != null) {
                sortArray = sortInfos.toArray(new SortInfo[] {});
            }
            String newQuery = NXQLQueryBuilder.getQuery(originalQuery,
                    getParameters(), def.getQuotePatternParameters(),
                    def.getEscapePatternParameters(), sortArray);

            if (!newQuery.equals(query)) {
                // query has changed => refresh
                refresh();
                query = newQuery;
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public PageSelections<Map<String, Serializable>> getCurrentSelectPage() {
        checkQueryCache();
        return super.getCurrentSelectPage();
    }

    protected void checkQueryCache() {
        // maybe handle refresh of select page according to query
        Map<String, Serializable> props = getProperties();
        if (props.containsKey(CHECK_QUERY_CACHE_PROPERTY)
                && Boolean.TRUE.equals(Boolean.valueOf((String) props.get(CHECK_QUERY_CACHE_PROPERTY)))) {
            buildQuery();
        }

    }

    public String getCurrentQuery() {
        return query;
    }

    @Override
    protected void pageChanged() {
        super.pageChanged();
        currentItems = null;
    }

    @Override
    public void refresh() {
        super.refresh();
        query = null;
        currentItems = null;
    }

}
