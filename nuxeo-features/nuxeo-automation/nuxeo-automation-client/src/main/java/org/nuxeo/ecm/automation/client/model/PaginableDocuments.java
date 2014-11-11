/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.model;

import java.util.List;

/**
 * @author matic
 */
public class PaginableDocuments extends Documents {

    private static final long serialVersionUID = 1L;

    /**
     * @deprecated since 5.7.3. Use {@link #resultsCount}.
     */
    @Deprecated
    protected int totalSize;

    /**
     * @deprecated since 5.7.3. Use {@link #numberOfPages}.
     */
    @Deprecated
    protected int pageCount;

    /**
     * @deprecated since 5.7.3. Use {@link #currentPageIndex}.
     */
    @Deprecated
    protected int pageIndex;

    protected int pageSize;

    protected int currentPageIndex;

    protected int numberOfPages;

    protected int resultsCount;

    public PaginableDocuments() {
    }

    /**
     * @param size
     */
    public PaginableDocuments(List<Document> docs, int resultsCount, int pageSize,
            int numberOfPages, int currentPageIndex) {
        super(docs);
        this.resultsCount = resultsCount;
        this.pageSize = pageSize;
        this.numberOfPages = numberOfPages;
        this.currentPageIndex = currentPageIndex;
    }

    /**
     * @deprecated since 5.7.3. Use {@link #getResultsCount()}.
     */
    @Deprecated
    public int getTotalSize() {
        return getResultsCount();
    }

    /**
     * @deprecated since 5.7.3. Use {@link #getNumberOfPages()}.
     */
    @Deprecated
    public int getPageCount() {
        return getNumberOfPages();
    }

    /**
     * @deprecated since 5.7.3. Use {@link #getCurrentPageIndex()}.
     */
    @Deprecated
    public int getPageIndex() {
        return getCurrentPageIndex();
    }

    /**
     * @deprecated since 5.7.3. Use {@link #setResultsCount(int)}.
     */
    @Deprecated
    public void setTotalSize(int totalSize) {
        setResultsCount(totalSize);
    }

    /**
     * @deprecated since 5.7.3. Use {@link #setNumberOfPages(int)}.
     */
    @Deprecated
    public void setPageCount(int pageCount) {
        setNumberOfPages(pageCount);
    }

    /**
     * @deprecated since 5.7.3. Use {@link #setCurrentPageIndex(int)}.
     */
    @Deprecated
    public void setPageIndex(int pageIndex) {
        setCurrentPageIndex(pageIndex);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public void setCurrentPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public int getResultsCount() {
        return resultsCount;
    }

    public void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
    }
}
