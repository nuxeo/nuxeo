/*
 * (C) Copyright 2007-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Georges Racinet
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.search.api.client.search.results.document;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.DocumentModelResultItem;

/**
 * @deprecated use {@link CoreQueryDocumentPageProvider} instead
 */
@Deprecated
public class SearchPageProvider implements PagedDocumentsProvider {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SearchPageProvider.class);

    private static final DocumentModelList EMPTY = new DocumentModelListImpl();

    private ResultSet searchResults;

    private final String query;

    // will cache current page
    private DocumentModelList currentPageDocList;

    private String providerName;

    private SortInfo sortInfo;

    private boolean sortable;

    private boolean pendingRefresh = false;

    // has the current page changed since last time it has been built
    private boolean pageChanged = false;

    /**
     * Constructor to create a sortable provider. Note that a provider can be
     * sortable and have a null sortInfo, which means a subsequent method call
     * with sortInfo not null will succeed.
     *
     * @param set the resultset
     * @param sortable if sortable, a subsequent call that provides sorting
     *            info
     * @param sortInfo the sorting info or null if the resultset is not sorted
     * @param query the query that produced this result. will succeed.
     */
    public SearchPageProvider(ResultSet set, boolean sortable,
            SortInfo sortInfo, String query) {
        searchResults = set;
        this.sortInfo = sortInfo;
        this.sortable = sortable;
        this.query = query;
    }

    /**
     * Constructor to create a non-sortable resultset.
     *
     * @param set
     */
    public SearchPageProvider(ResultSet set) {
        this(set, false, null, null);
    }

    @Override
    public DocumentModelList getCurrentPage() {
        if (currentPageDocList != null) {
            return currentPageDocList;
        }

        try {
            // if page has changed, no need to refresh.
            if (pendingRefresh && !pageChanged) {
                performRefresh();
            }
            currentPageDocList = constructDocumentModels();
            return currentPageDocList;
        } catch (SearchException e) {
            log.error("Catched a SearchException", e);
            return EMPTY;
        }
    }

    @Override
    public int getCurrentPageIndex() {
        int pag = searchResults.getPageNumber();
        // pag is 1 based
        // we need 0 based
        return pag - 1;
    }

    @SuppressWarnings("boxing")
    @Override
    public String getCurrentPageStatus() {
        int total = getNumberOfPages();
        int current = getCurrentPageIndex() + 1;
        if (total == UNKNOWN_SIZE) {
            return String.format("%d", current);
        } else {
            return String.format("%d/%d", current, total);
        }
    }

    @Override
    public DocumentModelList getNextPage() {
        next();
        return getCurrentPage();
    }

    public void goToPage(int page) {
        // 1 based
        page += 1;
        // TODO if the page is over the limit maybe go to the last page/ or
        // first
        try {
            ResultSet res = searchResults.goToPage(page);
            if (res == null) {
                return; // keep the same one to avoid NPEs
            }
            searchResults = res;
            // invalidate cache
            currentPageDocList = null;
            pageChanged = true;
        } catch (SearchException e) {
            log.error("getPage failed", e);
        }
    }

    @Override
    public DocumentModelList getPage(int page) {
        goToPage(page);
        return getCurrentPage();
    }

    @Override
    public long getResultsCount() {
        return searchResults.getTotalHits();
    }

    @Override
    public boolean isNextPageAvailable() {
        return searchResults.hasNextPage();
    }

    public String getQuery() {
        return query;
    }

    @Override
    public void last() {
        goToPage(getNumberOfPages() - 1);
    }

    @Override
    public void next() {
        if (isNextPageAvailable()) {
            goToPage(getCurrentPageIndex() + 1);
        }
    }

    @Override
    public void previous() {
        int i = getCurrentPageIndex();
        if (i > 0) {
            goToPage(i - 1);
        }
    }

    @Override
    public void rewind() {
        goToPage(0);
    }

    @Override
    public boolean isPreviousPageAvailable() {
        return getCurrentPageIndex() > 0;
    }

    @Override
    public int getNumberOfPages() {
        int range = searchResults.getRange();
        if (range == 0) {
            return 1;
        }
        return (int) (1 + (getResultsCount() - 1) / range);
    }

    protected void performRefresh() throws SearchException {
        searchResults = searchResults.replay();
        pendingRefresh = false;
    }

    /**
     * Actual refresh will be next time the page is really needed. Better
     * suited for Seam/JSF (avoid useless multiple requests)
     */
    @Override
    public void refresh() {
        pendingRefresh = true;
        currentPageDocList = null;
    }

    @Override
    public int getCurrentPageOffset() {
        return searchResults.getOffset();
    }

    @Override
    public int getCurrentPageSize() {
        return searchResults.getPageHits();
    }

    @Override
    public int getPageSize() {
        return searchResults.getRange();
    }

    protected DocumentModelList constructDocumentModels() {
        if (searchResults == null) {
            return EMPTY;
        }
        int pageHits = searchResults.getPageHits();
        List<DocumentModel> res = new ArrayList<DocumentModel>(pageHits);
        for (int i = 0; i < pageHits; i++) {
            ResultItem rItem = searchResults.get(i);
            DocumentModel doc = ((DocumentModelResultItem) rItem).getDocumentModel();
            res.add(doc);
        }
        pageChanged = false;
        return new DocumentModelListImpl(res);
    }

    @Override
    public SortInfo getSortInfo() {
        return sortInfo;
    }

    @Override
    public boolean isSortable() {
        return sortable;
    }

    public void setSortInfo(SortInfo sortInfo) {
        this.sortInfo = sortInfo;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public void setName(String name) {
        providerName = name;
    }

}
