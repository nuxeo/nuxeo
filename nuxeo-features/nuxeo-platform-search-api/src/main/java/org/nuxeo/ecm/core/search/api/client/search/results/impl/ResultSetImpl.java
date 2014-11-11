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

import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;

/**
 * Result set implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ResultSetImpl extends ArrayList<ResultItem> implements ResultSet {

    private static final long serialVersionUID = -6376330426798015144L;

    protected final int offset;

    protected final int range;

    protected final int totalHits;

    protected final int pageHits;

    protected final SQLQuery query;

    protected final String backendName;

    protected final SearchPrincipal principal;

    public ResultSetImpl(SQLQuery query, String backendName,
            SearchPrincipal principal, int offset, int range,
            List<ResultItem> resultItems, int totalHits, int pageHits) {
        this.query = query;
        this.backendName = backendName;
        this.principal = principal;
        this.offset = offset;
        this.range = range;
        this.totalHits = totalHits;
        this.pageHits = pageHits;
        if (resultItems != null) {
            addAll(resultItems);
        }
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
        int current = getPageNumber();
        int newOffset = range * (page - current) + offset;
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
        if (pageHits < range) {
            return false;
        }
        return (offset + range) < totalHits;
    }

    public boolean isFirstPage() {
        return offset < range;
    }

    public ResultSet replay() throws SearchException {
        return replay(offset, range);
    }

    public ResultSet replay(int offset, int range) throws SearchException {
        SearchService service = SearchServiceDelegate.getRemoteSearchService();
        try {
            if (query != null) {
                return service.searchQuery(new ComposedNXQueryImpl(query, principal),
                        offset, range);
            } else {
                throw new SearchException(
                        "Replay is not supported..............");
            }
        } catch (QueryException e) {
            // should really not happen
            throw new SearchException("QueryException for " + query.toString());
        }
    }

    public int getPageNumber() {
        return (offset + range) / range;
    }

}
