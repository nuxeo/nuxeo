/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Keeps track of current page and previous pages loaded from document iterator.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DocumentsPageProvider implements PagedDocumentsProvider {

    /**
     * Generated serial version uid.
     */
    private static final long serialVersionUID = 1016097877999734437L;

    private static final Log log = LogFactory.getLog(
            DocumentsPageProvider.class);

    /**
     * Reference to Documents iterator from which this class is feeding pages.
     */
    private final DocumentModelIterator docsIterator;

    private final int pageSize;

    private int currentPageIndex = -1;

    // local cache of loaded pages as a contiguous list from page 0
    // TODO limit the cache number...
    private final List<DocumentModelList> loadedPages = new ArrayList<DocumentModelList>();

    private long totalResultsCount;

    private String providerName;

    /**
     * Constructor taking as argument an iterator. The iterator is considered
     * unaltered
     *
     * @param docsIterator
     * @param pageSize
     */
    public DocumentsPageProvider(DocumentModelIterator docsIterator,
            int pageSize) {
        this.docsIterator = docsIterator;
        this.pageSize = pageSize;
        totalResultsCount = docsIterator.size();
    }

    public void setCurrentPage(int page) {
        // insure we have loaded the requested page if
        // currentPageIndex is updated by this
        // TODO is it correct to have the page loaded when set?
        getPage(page);
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public DocumentModelList getCurrentPage() {
        if (currentPageIndex == -1) {
            // avanse
            return getPage(0);
        }
        return getPage(currentPageIndex);
    }

    public DocumentModelList getPage(int page) {
        if (page < 0) {
            page = 0;
        }
        DocumentModelList docsPage = null;

        if (loadedPages.size() > page) {
            // we have it already retrieved
            docsPage = loadedPages.get(page);
            currentPageIndex = page;
        } else {

            // forward to the required page
            while (currentPageIndex < page) {
                if (docsIterator.hasNext()) {
                    docsPage = loadNextPage();

                    // cache the page
                    loadedPages.add(docsPage);
                } else {
                    // requested page out of limit
                    // empty list
                    // TODO : maybe reset to the first page
                    docsPage = new DocumentModelListImpl();
                    break;
                }
            }
        }

        return docsPage;
    }

    public boolean isNextPageAvailable() {
        return docsIterator.hasNext() || currentPageIndex < loadedPages.size() - 1;
    }

    /**
     * Creates the DocumentModelList for the next page.
     * Doesn't put in in the loadedPages cache
     */
    private DocumentModelList loadNextPage() {
        if (!docsIterator.hasNext()) {
            return new DocumentModelListImpl();
        }

        currentPageIndex++;
        DocumentModelList docsPage = new DocumentModelListImpl();
        for (int i = 0; i < pageSize; i++) {
            if (docsIterator.hasNext()) {
                try {
                    final DocumentModel docModel = docsIterator.next();
                    docsPage.add(docModel);
                } catch (NoSuchElementException e) {
                    log.error("Unpredicted end of iterator !!");
                    endReached(i);
                    break;
                }
            } else {
                endReached(i);
                break;
            }
        }
        return docsPage;
    }

    /**
     * Used to update some member fields from the new knowledge that
     * end was reached.
     *
     * @param posInPage: the position in current page when this happened
     */
    private void endReached(int posInPage) {
        if (DocumentModelIterator.UNKNOWN_SIZE == totalResultsCount) {
            totalResultsCount = pageSize * currentPageIndex + posInPage;
        }
    }

    public long getResultsCount() {
        long resultsCount = totalResultsCount;
        if (DocumentModelIterator.UNKNOWN_SIZE == resultsCount) {
            resultsCount = UNKNOWN_SIZE;
        }
        return resultsCount;
    }

    public boolean isPreviousPageAvailable() {
        return currentPageIndex > 0;
    }

    public void last() {
        int lastPage = getNumberOfPages();
        if (lastPage == UNKNOWN_SIZE) {
            while (isNextPageAvailable()) {
                getNextPage();
            }
        } else {
            setCurrentPage(lastPage - 1);
        }
    }

    public DocumentModelList getNextPage() {
        return getPage(currentPageIndex + 1);
    }

    public void next() {
        getPage(currentPageIndex + 1);
    }

    public void previous() {
        if (currentPageIndex > 0) {
            setCurrentPage(currentPageIndex - 1);
        }
    }

    public void rewind() {
        getPage(0);
    }

    public int getNumberOfPages() {
        long size = docsIterator.size();
        if (size == DocumentModelIterator.UNKNOWN_SIZE) {
            return UNKNOWN_SIZE;
        }
        return (int) (1 + (size - 1) / pageSize);
    }

    /**
     * Nothing can't be done to refresh this provider's pages
     * the whole provider should be instead replaced
     */
    public void refresh() {
    }

    // TODO stop duplication
    public String getCurrentPageStatus() {
        int total = getNumberOfPages();
        int current = currentPageIndex + 1;
        if (total == UNKNOWN_SIZE) {
            return String.format("%d", current);
        } else {
            return String.format("%d/%d", current, total);
        }
    }

    public int getCurrentPageOffset() {
        // Might become inappropriate if previous documents have changed
        return currentPageIndex * pageSize;
    }

    public int getCurrentPageSize() {
        // page getters are cached, hence cheap
        return getCurrentPage().size();
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getName() {
        return providerName;
    }

    public SortInfo getSortInfo() {
        return null;
    }

    public boolean isSortable() {
        return false;
    }

    public void setName(String name) {
        providerName = name;
    }

}
