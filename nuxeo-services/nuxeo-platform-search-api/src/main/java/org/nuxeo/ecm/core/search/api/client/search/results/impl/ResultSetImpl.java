/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ResultSetImpl.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.search.results.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.runtime.api.Framework;

/**
 * Result set implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ResultSetImpl extends ArrayList<ResultItem> implements ResultSet {

    private static final long serialVersionUID = -6376330426798015144L;

    protected final int offset;

    /** 0 means all results */
    protected final int range;

    protected final int totalHits;

    protected final int pageHits;

    protected final String query;

    protected final SQLQuery sqlQuery;

    protected final SearchPrincipal principal;

    protected final CoreSession session;

    protected Boolean detachResultsFlag;

    /**
     * Constructor used when a CoreSession is available.
     */
    public ResultSetImpl(String query, CoreSession session, int offset,
            int range, List<ResultItem> resultItems, int totalHits, int pageHits) {
        this.query = query;
        sqlQuery = null;
        this.session = session;
        principal = null;
        this.offset = offset;
        this.range = range;
        this.totalHits = totalHits;
        this.pageHits = pageHits;
        if (resultItems != null) {
            addAll(resultItems);
        }
    }

    /**
     * Constructor used for compatibility, using the SearchService.
     *
     * @deprecated use the constructor taking a {@link CoreSession} instead
     */
    @Deprecated
    public ResultSetImpl(SQLQuery sqlQuery, SearchPrincipal principal,
            int offset, int range, List<ResultItem> resultItems, int totalHits,
            int pageHits) {
        this.sqlQuery = sqlQuery;
        query = null;
        this.principal = principal;
        session = null;
        this.offset = offset;
        this.range = range;
        this.totalHits = totalHits;
        this.pageHits = pageHits;
        if (resultItems != null) {
            addAll(resultItems);
        }
    }

    public boolean detachResults() {
        if (detachResultsFlag == null) {
            detachResultsFlag = Boolean.valueOf(Framework.getProperty(
                    ALWAYS_DETACH_SEARCH_RESULTS_KEY, "false"));
        }
        return detachResultsFlag.booleanValue();
    }

    public int getOffset() {
        return offset;
    }

    public int getRange() {
        return range;
    }

    public ResultSet nextPage() throws SearchException {
        if (!hasNextPage()) {
            return null;
        }
        return replay(offset + range, range);
    }

    public ResultSet goToPage(int page) throws SearchException {
        int newOffset = range * (page - 1);
        if (newOffset >= 0 && newOffset < totalHits) {
            return replay(newOffset, range);
        }
        return null;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public int getPageHits() {
        return pageHits;
    }

    public boolean hasNextPage() {
        if (range == 0) {
            return false;
        }
        if (pageHits < range) {
            return false;
        }
        return (offset + range) < totalHits;
    }

    public boolean isFirstPage() {
        return range == 0 ? true : offset < range;
    }

    public ResultSet replay() throws SearchException {
        return replay(offset, range);
    }

    public ResultSet replay(int offset, int range) throws SearchException {
        if (session != null) {
            try {
                DocumentModelList list = session.query(query, null, range,
                        offset, true);
                List<ResultItem> resultItems = new ArrayList<ResultItem>(
                        list.size());
                for (DocumentModel doc : list) {
                    if (doc == null) {
                        continue;
                    }
                    if (detachResults()) {
                        // detach the document so that we can use it beyond the
                        // session
                        try {
                            ((DocumentModelImpl) doc).detach(true);
                        } catch (DocumentSecurityException e) {
                            // no access to the document (why?)
                            continue;
                        }
                    }
                    resultItems.add(new DocumentModelResultItem(doc));
                }
                return new ResultSetImpl(query, session, offset, range,
                        resultItems, (int) list.totalSize(), list.size());
            } catch (ClientException e) {
                throw new SearchException("QueryException for: " + query, e);
            }
        }

        // compat code, loopback through search service
        if (sqlQuery == null) {
            throw new SearchException("Replay is not supported");
        }
        SearchService service = SearchServiceDelegate.getRemoteSearchService();
        try {
            return service.searchQuery(new ComposedNXQueryImpl(sqlQuery,
                    principal), offset, range);
        } catch (QueryException e) {
            // should really not happen
            throw new SearchException("QueryException for "
                    + sqlQuery.toString());
        }
    }

    public int getPageNumber() {
        if (range == 0) {
            return 1;
        }
        return (offset + range) / range;
    }

}
