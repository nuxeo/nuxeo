/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.query.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Basic interface for a page provider, independent of type of items in the list
 * <p>
 * Provides APIs to navigate between result pages
 *
 * @param <T> any Serializable item
 * @since 5.4
 * @author arussel
 * @author Anahide Tchertchian
 */
public interface PageProvider<T> extends Serializable {

    public static final String DEFAULT_MAX_PAGE_SIZE_RUNTIME_PROP = "nuxeo.pageprovider.default-max-page-size";

    /**
     * Constant to express that the total number of result elements is unknown (usually because the query has not been
     * done yet).
     */
    public static final long UNKNOWN_SIZE = -1;

    /**
     * Constant to express that the total number of result elements is unknown even after performing a query.
     *
     * @since 5.5
     */
    public static final long UNKNOWN_SIZE_AFTER_QUERY = -2;

    /**
     * Default maximum page size value.
     *
     * @since 6.0, default value is 1000.
     */
    public static final long DEFAULT_MAX_PAGE_SIZE = 1000;

    /**
     * Page limit unknown.
     *
     * @since 5.8
     */
    public static final long PAGE_LIMIT_UNKNOWN = -1;

    /**
     * Returns the provider identifier
     */
    String getName();

    /**
     * Sets the provider identifier
     */
    void setName(String name);

    /**
     * Gets properties set on the provider.
     * <p>
     * Useful to retrieve a provider specific field attributes after instantiation. Other contextual parameters can be
     * passed through API constructing the result provider.
     */
    Map<String, Serializable> getProperties();

    /**
     * Sets properties set on the provider.
     * <p>
     * Useful to initialize a provider specific field attributes after instantiation. Other contextual parameters can be
     * passed through API constructing the result provider.
     */
    void setProperties(Map<String, Serializable> properties);

    Object[] getParameters();

    void setParameters(Object[] parameters);

    /**
     * Returns the number of results per page. 0 means no pagination.
     */
    long getPageSize();

    /**
     * Sets the number results per page. 0 means no pagination.
     */
    void setPageSize(long pageSize);

    /**
     * Returns the max number of results per page. 0 means no pagination.
     * <p>
     * If page size is greater than this maximum value, it will be taken into account instead.
     *
     * @since 5.4.2
     */
    long getMaxPageSize();

    /**
     * Sets the max number of results per page. 0 means no pagination.
     * <p>
     * If page size is greater than this maximum value, it will be taken into account instead.
     *
     * @since 5.4.2
     */
    void setMaxPageSize(long pageSize);

    /**
     * Returns the number of result elements if available or a negative value if it is unknown:
     * <code>UNKNOWN_SIZE</code> if it is unknown as query was not done, and since 5.5,
     * <code>UNKNOWN_SIZE_AFTER_QUERY</code> if it is still unknown after query was done.
     */
    long getResultsCount();

    /**
     * Sets the results count.
     *
     * @since 5.5
     */
    void setResultsCount(long resultsCount);

    /**
     * Returns the total number of pages or 0 if number of pages is unknown.
     */
    long getNumberOfPages();

    /**
     * Returns the page limit. The n first page we know they exist.
     *
     * @since 5.7.3
     */
    long getPageLimit();

    /**
     * Returns the current page of results.
     * <p>
     * This method is designed to be called from higher levels. It therefore ensures cheapness of repeated calls, rather
     * than data consistency. There is a refresh() method for that.
     * <p>
     *
     * @return the current page
     */
    List<T> getCurrentPage();

    /**
     * Returns the current page of results wrapped in a {@link PageSelection} item.
     * <p>
     * By default, no entry is selected, unless {@link #setSelectedEntries(List)} has been called before.
     */
    PageSelections<T> getCurrentSelectPage();

    /**
     * Sets the list of selected entries to take into account in {@link #getCurrentSelectPage()}.
     */
    void setSelectedEntries(List<T> entries);

    /**
     * Sets the current page offset.
     * <p>
     * If the provider keeps information linked to the current page, they should be reset after calling this method.
     *
     * @since 5.5
     */
    public void setCurrentPageOffset(long offset);

    /**
     * Sets the current page of results to the required one.
     *
     * @param currentPageIndex the page index, starting from 0
     * @since 5.7.3
     */
    void setCurrentPageIndex(long currentPageIndex);

    /**
     * Sets the current page of results to the required one and return it.
     *
     * @param page the page index, starting from 0
     */
    List<T> setCurrentPage(long page);

    /**
     * Forces refresh of the current page.
     */
    void refresh();

    /**
     * Returns a boolean expressing if there are further pages.
     */
    boolean isNextPageAvailable();

    /**
     * Returns a boolean expressing if the last page can be displayed.
     *
     * @since 5.5
     */
    boolean isLastPageAvailable();

    /**
     * Returns a boolean expressing if there is a previous page.
     */
    boolean isPreviousPageAvailable();

    /**
     * Returns the number of elements in current page.
     */
    long getCurrentPageSize();

    /**
     * Returns the offset (starting from 0) of the first element in the current page or <code>UNKNOWN_SIZE</code>.
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
     * Go to the last page. Does not do anything if there is only one page displayed, or if the number of results is
     * unknown.
     */
    void lastPage();

    /**
     * Returns the current entry.
     */
    T getCurrentEntry();

    /**
     * Sets the current entry.
     *
     * @throws ClientException if entry is not found within current page.
     */
    void setCurrentEntry(T entry) throws ClientException;

    /**
     * Sets the current entry index.
     *
     * @throws ClientException if index is not found within current page.
     */
    void setCurrentEntryIndex(long index) throws ClientException;

    /**
     * Returns true if there is a next entry.
     * <p>
     * The next entry might be in next page, except if results count is unknown.
     */
    boolean isNextEntryAvailable();

    /**
     * Returns true if there is a previous entry.
     * <p>
     * The previous entry might be in previous page.
     */
    boolean isPreviousEntryAvailable();

    /**
     * Move the current entry to the previous one, if applicable.
     * <p>
     * No exception: this method is intended to be plugged directly at the UI layer. In case there's no previous entry,
     * nothing will happen.
     */
    void previousEntry();

    /**
     * Move the current entry to the next one, if applicable.
     * <p>
     * If needed and possible, the provider will forward to next page. No special exceptions: this method is intended to
     * be plugged directly at the UI layer. In case there's no next entry, nothing happens.
     */
    void nextEntry();

    /**
     * Returns if this provider is sortable
     */
    boolean isSortable();

    void setSortable(boolean sortable);

    /**
     * Returns the complete list of sorting info for this provider
     */
    List<SortInfo> getSortInfos();

    /**
     * Returns the first sorting info for this provider
     * <p>
     * Also kept for compatibility with existing code.
     */
    SortInfo getSortInfo();

    /**
     * Sets the complete list of sorting info for this provider
     */
    void setSortInfos(List<SortInfo> sortInfo);

    /**
     * Sets the first and only sorting info for this provider.
     * <p>
     * Also kept for compatibility with existing code.
     */
    void setSortInfo(SortInfo sortInfo);

    /**
     * Sets the first and only sorting info for this provider if parameter removeOtherSortInfos is true. Otherwise, adds
     * or changes the sortAscending information according to given direction.
     */
    void setSortInfo(String sortColumn, boolean sortAscending, boolean removeOtherSortInfos);

    /**
     * Add the given sort info to the list of sorting infos.
     */
    void addSortInfo(String sortColumn, boolean sortAscending);

    /**
     * Returns a positive 0-based integer if given sort information is found on the set sort infos, indicating the sort
     * index, or -1 if this sort information is not found.
     */
    int getSortInfoIndex(String sortColumn, boolean sortAscending);

    DocumentModel getSearchDocumentModel();

    void setSearchDocumentModel(DocumentModel doc);

    boolean hasError();

    String getErrorMessage();

    Throwable getError();

    void setDefinition(PageProviderDefinition providerDefinition);

    PageProviderDefinition getDefinition();

    /**
     * Sets the {@link PageProviderChangedListener} for this {@code PageProvider}.
     *
     * @since 5.7
     */
    void setPageProviderChangedListener(PageProviderChangedListener listener);

    /**
     * Test if provider parameters have changed
     *
     * @since 5.7
     */
    boolean hasChangedParameters(Object[] parameters);

    /**
     * @since 6.0
     */
    List<AggregateDefinition> getAggregateDefinitions();

    /**
     * @since 6.0
     */
    Map<String, Aggregate<? extends Bucket>> getAggregates();

    /**
     * @since 6.0
     */
    boolean hasAggregateSupport();

}
