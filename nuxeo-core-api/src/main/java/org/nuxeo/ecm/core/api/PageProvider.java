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
 *     arussel
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * Basic interface for a page provider, independent of type of items in the
 * list
 * <p>
 * Provides APIs to navigate between result pages
 *
 * @author arussel
 * @author Anahide Tchertchian
 * @param <T> any Serializable item
 */
public interface PageProvider<T extends Serializable> extends Serializable {

    /**
     * Constant to express that the total number of result elements is unknown.
     */
    long UNKNOWN_SIZE = -1;

    /**
     * Returns the provider identifier
     */
    String getName();

    /**
     * Sets the provider identifier
     */
    void setName(String name);

    /**
     * Returns the number of requested page size.
     */
    long getPageSize();

    /**
     * Returns the number of result elements if available or
     * <code>UNKNOWN_SIZE</code> if it is unknown
     */
    long getResultsCount();

    /**
     * Returns the total number of pages
     */
    long getNumberOfPages();

    /**
     * Returns the current page of results.
     * <p>
     * This method is designed to be called from JSF. It therefore ensures
     * cheapness of repeated calls, rather than data consistency. There is a
     * refresh() method for that.
     * <p>
     *
     * @return the current page
     */
    List<T> getCurrentPage();

    /**
     * Sets the current page of results to the required one and return it.
     *
     * @param page the page index, starting from 0
     */
    List<T> getPage(long page);

    /**
     * Forces refresh of the current page.
     */
    void refresh();

    /**
     * Returns a boolean expressing if there are further pages.
     */
    boolean isNextPageAvailable();

    /**
     * Returns a boolean expressing if there is a previous page.
     */
    boolean isPreviousPageAvailable();

    /**
     * Returns the number of elements in current page.
     */
    long getCurrentPageSize();

    /**
     * Returns the offset (starting from 0) of the first element in the current
     * page or <code>UNKNOWN_SIZE</code>
     */
    long getCurrentPageOffset();

    /**
     * Returns the current page index as a zero-based integer.
     */
    long getCurrentPageIndex();

    /**
     * Returns a simple formatted string for current pagination status.
     */
    String getCurrentPageStatus();

    /**
     * Go to the first page
     */
    void firstPage();

    /**
     * Go to the previous page
     */
    void previousPage();

    /**
     * Go to the next page
     */
    void nextPage();

    /**
     * Go to the last page
     */
    void lastPage();

    /**
     * Returns the current entry
     */
    T getCurrentEntry();

    /**
     * Sets the current entry
     *
     * @throws ClientException if entry is not found within current page.
     */
    void setCurrentEntry(T entry) throws ClientException;

    /**
     * Tells if there is a next entry.
     * <p>
     * The next entry might be in next page. If no current entry is set, this
     * returns false
     * </p>
     */
    boolean isNextEntryAvailable();

    /**
     * Tells if there is a previous entry.
     * <p>
     * The next entry might be in next page. If no current entry is set, this
     * returns false
     * </p>
     */
    boolean isPreviousEntryAvailable();

    /**
     * Move the current entry to the previous one, if applicable
     * <p>
     * No exception: this method is intended to be plugged directly at the UI
     * layer. In case there's no preivous entry, nothing will happen.
     * </p>
     *
     * @throws DistributionException
     */
    void previousEntry();

    /**
     * Move the current entry to the next one, if applicable
     * <p>
     * If needed and possible, the provider will forward to next page. No
     * special exceptions: this method is intended to be plugged directly at
     * the UI layer. In case there's no next entry, nothing happens.
     * </p>
     */
    void nextEntry();

    /**
     * Returns if this provider is sortable
     */
    boolean isSortable();

    /**
     * Returns the sorting info for this provider
     */
    List<SortInfo> getSortInfo();

    /**
     * Sets the sorting info for this provider
     */
    void setSortInfo(List<SortInfo> sortInfo);

}
