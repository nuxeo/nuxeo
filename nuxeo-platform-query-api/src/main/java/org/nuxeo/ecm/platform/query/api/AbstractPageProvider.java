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
package org.nuxeo.ecm.platform.query.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Basic implementation for a {@link PageProvider}.
 * <p>
 * Provides next/prev standard logics, and helper methods for retrieval of
 * items and first/next/prev/last buttons display as well as other display
 * information (number of pages for instance).
 * <p>
 * Also handles selection by providing a default implementation of
 * {@link #getCurrentSelectPage()} working in conjunction with
 * {@link #setSelectedEntries(List)}.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractPageProvider<T> implements PageProvider<T> {

    public static final Log log = LogFactory.getLog(AbstractPageProvider.class);

    private static final long serialVersionUID = 1L;

    protected String name;

    protected long offset = 0;

    protected long pageSize = 0;

    protected long maxPageSize = getDefaultMaxPageSize();

    protected long resultsCount = UNKNOWN_SIZE;

    protected int currentEntryIndex = 0;

    /**
     * Integer keeping track of the higher page index giving results. Useful
     * for enabling or disabling the nextPage action when number of results
     * cannot be known.
     *
     * @since 5.5
     */
    protected int currentHigherNonEmptyPageIndex = 0;

    protected List<SortInfo> sortInfos;

    protected boolean sortable = false;

    protected List<T> selectedEntries;

    protected PageSelections<T> currentSelectPage;

    protected Map<String, Serializable> properties;

    protected Object[] parameters;

    protected DocumentModel searchDocumentModel;

    protected String errorMessage;

    protected Throwable error;

    protected PageProviderDefinition definition;

    protected PageProviderChangedListener pageProviderChangedListener;

    protected static final AggregateQuery[] EMPTY_AGGREGATION_QUERY = new AggregateQuery[0];

    protected  AggregateQuery[] aggregateQuery;

    /**
     * Returns the list of current page items.
     * <p>
     * Custom implementation can be added here, based on the page provider
     * properties, parameters and {@link WhereClauseDefinition} on the
     * {@link PageProviderDefinition}, as well as search document, sort
     * information, etc...
     * <p>
     * Implementation of this method usually consists in setting a non-null
     * value to a field caching current items, and nullifying this field by
     * overriding {@link #pageChanged()} and {@link #refresh()}.
     * <p>
     * Fields {@link #errorMessage} and {@link #error} can also be filled to
     * provide accurate feedback in case an error occurs during the search.
     * <p>
     * When items are retrieved, a call to {@link #setResultsCount(long)}
     * should be made to ensure proper pagination as implemented in this
     * abstract class. The implementation in
     * {@link CoreQueryAndFetchPageProvider} is a good example when the total
     * results count is known.
     * <p>
     * If for performance reasons, for instance, the number of results cannot
     * be known, a fall-back strategy can be applied to provide the "next"
     * button but not the "last" one, by calling
     * {@link #getCurrentHigherNonEmptyPageIndex()} and
     * {@link #setCurrentHigherNonEmptyPageIndex(int)}. In this case,
     * {@link CoreQueryDocumentPageProvider} is a good example.
     */
    public abstract List<T> getCurrentPage();

    /**
     * Page change hook, to override for custom behavior
     * <p>
     * When overriding it, call {@code super.pageChanged()} as last statement
     * to make sure that the {@link PageProviderChangedListener} is called with
     * the up-to-date @{code PageProvider} state.
     */
    protected void pageChanged() {
        currentEntryIndex = 0;
        currentSelectPage = null;
        notifyPageChanged();
    }

    public void firstPage() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            // do nothing
            return;
        }
        long offset = getCurrentPageOffset();
        if (offset != 0) {
            setCurrentPageOffset(0);
            pageChanged();
        }
    }

    /**
     * @deprecated: use {@link #firstPage()} instead
     */
    @Deprecated
    public void rewind() {
        firstPage();
    }

    public long getCurrentPageIndex() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            return 0;
        }
        long offset = getCurrentPageOffset();
        return offset / pageSize;
    }

    public long getCurrentPageOffset() {
        return offset;
    }

    public void setCurrentPageOffset(long offset) {
        this.offset = offset;
    }

    public long getCurrentPageSize() {
        List<T> currentItems = getCurrentPage();
        if (currentItems != null) {
            return currentItems.size();
        }
        return 0;
    }

    public String getName() {
        return name;
    }

    public long getNumberOfPages() {
        long pageSize = getPageSize();
        // ensure 1 if no pagination
        if (pageSize == 0) {
            return 1;
        }
        // take max page size into into account
        pageSize = getMinMaxPageSize();
        if (pageSize == 0) {
            return 1;
        }
        long resultsCount = getResultsCount();
        if (resultsCount < 0) {
            return 0;
        } else {
            return (1 + (resultsCount - 1) / pageSize);
        }
    }

    @Override
    public void setCurrentPageIndex(long currentPageIndex) {
        long pageSize = getPageSize();
        long offset = currentPageIndex * pageSize;
        setCurrentPageOffset(offset);
        pageChanged();
    }

    public List<T> setCurrentPage(long page) {
        setCurrentPageIndex(page);
        return getCurrentPage();
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        long localPageSize = getPageSize();
        if (localPageSize != pageSize) {
            this.pageSize = pageSize;
            // reset offset too
            setCurrentPageOffset(0);
            refresh();
        }
    }

    public List<SortInfo> getSortInfos() {
        // break reference
        List<SortInfo> res = new ArrayList<SortInfo>();
        if (sortInfos != null) {
            res.addAll(sortInfos);
        }
        return res;
    }

    public SortInfo getSortInfo() {
        if (sortInfos != null && !sortInfos.isEmpty()) {
            return sortInfos.get(0);
        }
        return null;
    }

    protected boolean sortInfoChanged(List<SortInfo> oldSortInfos,
            List<SortInfo> newSortInfos) {
        if (oldSortInfos == null && newSortInfos == null) {
            return false;
        } else if (oldSortInfos == null) {
            oldSortInfos = Collections.emptyList();
        } else if (newSortInfos == null) {
            newSortInfos = Collections.emptyList();
        }
        if (oldSortInfos.size() != newSortInfos.size()) {
            return true;
        }
        for (int i = 0; i < oldSortInfos.size(); i++) {
            SortInfo oldSort = oldSortInfos.get(i);
            SortInfo newSort = newSortInfos.get(i);
            if (oldSort == null && newSort == null) {
                continue;
            } else if (oldSort == null || newSort == null) {
                return true;
            }
            if (!oldSort.equals(newSort)) {
                return true;
            }
        }
        return false;
    }

    public void setSortInfos(List<SortInfo> sortInfo) {
        if (sortInfoChanged(this.sortInfos, sortInfo)) {
            this.sortInfos = sortInfo;
            refresh();
        }
    }

    public void setSortInfo(SortInfo sortInfo) {
        List<SortInfo> newSortInfos = new ArrayList<SortInfo>();
        if (sortInfo != null) {
            newSortInfos.add(sortInfo);
        }
        setSortInfos(newSortInfos);
    }

    public void setSortInfo(String sortColumn, boolean sortAscending,
            boolean removeOtherSortInfos) {
        if (removeOtherSortInfos) {
            SortInfo sortInfo = new SortInfo(sortColumn, sortAscending);
            setSortInfo(sortInfo);
        } else {
            if (getSortInfoIndex(sortColumn, sortAscending) != -1) {
                // do nothing: sort on this column is not set
            } else if (getSortInfoIndex(sortColumn, !sortAscending) != -1) {
                // change direction
                List<SortInfo> newSortInfos = new ArrayList<SortInfo>();
                for (SortInfo sortInfo : getSortInfos()) {
                    if (sortColumn.equals(sortInfo.getSortColumn())) {
                        newSortInfos.add(new SortInfo(sortColumn, sortAscending));
                    } else {
                        newSortInfos.add(sortInfo);
                    }
                }
                setSortInfos(newSortInfos);
            } else {
                // just add it
                addSortInfo(sortColumn, sortAscending);
            }
        }
    }

    public void addSortInfo(String sortColumn, boolean sortAscending) {
        SortInfo sortInfo = new SortInfo(sortColumn, sortAscending);
        List<SortInfo> sortInfos = getSortInfos();
        if (sortInfos == null) {
            setSortInfo(sortInfo);
        } else {
            sortInfos.add(sortInfo);
            setSortInfos(sortInfos);
        }
    }

    public int getSortInfoIndex(String sortColumn, boolean sortAscending) {
        List<SortInfo> sortInfos = getSortInfos();
        if (sortInfos == null || sortInfos.isEmpty()) {
            return -1;
        } else {
            SortInfo sortInfo = new SortInfo(sortColumn, sortAscending);
            return sortInfos.indexOf(sortInfo);
        }
    }

    public boolean isNextPageAvailable() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            return false;
        }
        long resultsCount = getResultsCount();
        if (resultsCount < 0) {
            long currentPageIndex = getCurrentPageIndex();
            return currentPageIndex < getCurrentHigherNonEmptyPageIndex()
                    + getMaxNumberOfEmptyPages();
        } else {
            long offset = getCurrentPageOffset();
            return resultsCount > pageSize + offset;
        }
    }

    @Override
    public boolean isLastPageAvailable() {
        long resultsCount = getResultsCount();
        if (resultsCount < 0) {
            return false;
        }
        return isNextPageAvailable();
    }

    public boolean isPreviousPageAvailable() {
        long offset = getCurrentPageOffset();
        return offset > 0;
    }

    public void lastPage() {
        long pageSize = getPageSize();
        long resultsCount = getResultsCount();
        if (pageSize == 0 || resultsCount < 0) {
            // do nothing
            return;
        }
        if (resultsCount % pageSize == 0) {
            setCurrentPageOffset(resultsCount - pageSize);
        } else {
            setCurrentPageOffset(resultsCount - resultsCount % pageSize);
        }
        pageChanged();
    }

    /**
     * @deprecated: use {@link #lastPage()} instead
     */
    @Deprecated
    public void last() {
        lastPage();
    }

    public void nextPage() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            // do nothing
            return;
        }
        long offset = getCurrentPageOffset();
        offset += pageSize;
        setCurrentPageOffset(offset);
        pageChanged();
    }

    /**
     * @deprecated: use {@link #nextPage()} instead
     */
    @Deprecated
    public void next() {
        nextPage();
    }

    public void previousPage() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            // do nothing
            return;
        }
        long offset = getCurrentPageOffset();
        if (offset >= pageSize) {
            offset -= pageSize;
            setCurrentPageOffset(offset);
            pageChanged();
        }
    }

    /**
     * @deprecated: use {@link #previousPage()} instead
     */
    @Deprecated
    public void previous() {
        previousPage();
    }

    /**
     * Refresh hook, to override for custom behavior
     * <p>
     * When overriding it, call {@code super.refresh()} as last statement to
     * make sure that the {@link PageProviderChangedListener} is called with
     * the up-to-date @{code PageProvider} state.
     */
    public void refresh() {
        setResultsCount(UNKNOWN_SIZE);
        setCurrentHigherNonEmptyPageIndex(-1);
        currentSelectPage = null;
        errorMessage = null;
        error = null;
        notifyRefresh();

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentPageStatus() {
        long total = getNumberOfPages();
        long current = getCurrentPageIndex() + 1;
        if (total <= 0) {
            // number of pages unknown or there is only one page
            return String.format("%d", Long.valueOf(current));
        } else {
            return String.format("%d/%d", Long.valueOf(current),
                    Long.valueOf(total));
        }
    }

    public boolean isNextEntryAvailable() {
        long pageSize = getPageSize();
        long resultsCount = getResultsCount();
        if (pageSize == 0) {
            if (resultsCount < 0) {
                // results count unknown
                long currentPageSize = getCurrentPageSize();
                return currentEntryIndex < currentPageSize - 1;
            } else {
                return currentEntryIndex < resultsCount - 1;
            }
        } else {
            long currentPageSize = getCurrentPageSize();
            if (currentEntryIndex < currentPageSize - 1) {
                return true;
            }
            if (resultsCount < 0) {
                // results count unknown => do not look for entry in next page
                return false;
            } else {
                return isNextPageAvailable();
            }
        }
    }

    public boolean isPreviousEntryAvailable() {
        return (currentEntryIndex != 0 || isPreviousPageAvailable());
    }

    public void nextEntry() {
        long pageSize = getPageSize();
        long resultsCount = getResultsCount();
        if (pageSize == 0) {
            if (resultsCount < 0) {
                // results count unknown
                long currentPageSize = getCurrentPageSize();
                if (currentEntryIndex < currentPageSize - 1) {
                    currentEntryIndex++;
                    return;
                }
            } else {
                if (currentEntryIndex < resultsCount - 1) {
                    currentEntryIndex++;
                    return;
                }
            }
        } else {
            long currentPageSize = getCurrentPageSize();
            if (currentEntryIndex < currentPageSize - 1) {
                currentEntryIndex++;
                return;
            }
            if (resultsCount >= 0) {
                // if results count is unknown, do not look for entry in next
                // page
                if (isNextPageAvailable()) {
                    nextPage();
                    currentEntryIndex = 0;
                    return;
                }
            }
        }

    }

    public void previousEntry() {
        if (currentEntryIndex > 0) {
            currentEntryIndex--;
            return;
        }
        if (!isPreviousPageAvailable()) {
            return;
        }

        previousPage();
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            // things may have changed since last query
            currentEntryIndex = 0;
        } else {
            currentEntryIndex = (new Long(getPageSize() - 1)).intValue();
        }
    }

    public T getCurrentEntry() {
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            return null;
        }
        return currentPage.get(currentEntryIndex);
    }

    public void setCurrentEntry(T entry) throws ClientException {
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            throw new ClientException(String.format(
                    "Entry '%s' not found in current page", entry));
        }
        int i = currentPage.indexOf(entry);
        if (i == -1) {
            throw new ClientException(String.format(
                    "Entry '%s' not found in current page", entry));
        }
        currentEntryIndex = i;
    }

    public void setCurrentEntryIndex(long index) throws ClientException {
        int intIndex = new Long(index).intValue();
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            throw new ClientException(
                    String.format("Index %s not found in current page",
                            new Integer(intIndex)));
        }
        if (index >= currentPage.size()) {
            throw new ClientException(
                    String.format("Index %s not found in current page",
                            new Integer(intIndex)));
        }
        currentEntryIndex = intIndex;
    }

    public long getResultsCount() {
        return resultsCount;
    }

    public Map<String, Serializable> getProperties() {
        // break reference
        return new HashMap<String, Serializable>(properties);
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    /**
     * @since 5.9.6
     */
    protected boolean getBooleanProperty(String propName, boolean defaultValue) {
        Map<String, Serializable> props = getProperties();
        if (props.containsKey(propName)) {
            Serializable prop = props.get(propName);
            if (prop instanceof String) {
                return Boolean.parseBoolean((String) prop);
            } else {
                return Boolean.TRUE.equals(prop);
            }
        }
        return defaultValue;
    }

    public void setResultsCount(long resultsCount) {
        this.resultsCount = resultsCount;
        setCurrentHigherNonEmptyPageIndex(-1);
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public PageSelections<T> getCurrentSelectPage() {
        if (currentSelectPage == null) {
            List<PageSelection<T>> entries = new ArrayList<PageSelection<T>>();
            List<T> currentPage = getCurrentPage();
            currentSelectPage = new PageSelections<T>();
            currentSelectPage.setName(name);
            if (currentPage != null && !currentPage.isEmpty()) {
                if (selectedEntries == null || selectedEntries.isEmpty()) {
                    // no selection at all
                    for (int i = 0; i < currentPage.size(); i++) {
                        entries.add(new PageSelection<T>(currentPage.get(i),
                                false));
                    }
                } else {
                    boolean allSelected = true;
                    for (int i = 0; i < currentPage.size(); i++) {
                        T entry = currentPage.get(i);
                        Boolean selected = Boolean.valueOf(selectedEntries.contains(entry));
                        if (!Boolean.TRUE.equals(selected)) {
                            allSelected = false;
                        }
                        entries.add(new PageSelection<T>(entry,
                                selected.booleanValue()));
                    }
                    if (allSelected) {
                        currentSelectPage.setSelected(true);
                    }
                }
            }
            currentSelectPage.setEntries(entries);
        }
        return currentSelectPage;
    }

    public void setSelectedEntries(List<T> entries) {
        this.selectedEntries = entries;
        // reset current select page so that it's rebuilt
        currentSelectPage = null;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public DocumentModel getSearchDocumentModel() {
        return searchDocumentModel;
    }

    protected boolean searchDocumentModelChanged(DocumentModel oldDoc,
            DocumentModel newDoc) {
        if (oldDoc == null && newDoc == null) {
            return false;
        } else if (oldDoc == null || newDoc == null) {
            return true;
        }
        // do not compare properties and assume it's changed
        return true;
    }

    public void setSearchDocumentModel(DocumentModel searchDocumentModel) {
        if (searchDocumentModelChanged(this.searchDocumentModel,
                searchDocumentModel)) {
            refresh();
        }
        this.searchDocumentModel = searchDocumentModel;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    @Override
    public PageProviderDefinition getDefinition() {
        return definition;
    }

    @Override
    public void setDefinition(PageProviderDefinition providerDefinition) {
        this.definition = providerDefinition;
    }

    public long getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(long maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    /**
     * Returns the minimal value for the max page size, taking the lower value
     * between the requested page size and the maximum accepted page size.
     *
     * @since 5.4.2
     */
    public long getMinMaxPageSize() {
        long pageSize = getPageSize();
        long maxPageSize = getMaxPageSize();
        if (maxPageSize < 0) {
            maxPageSize = getDefaultMaxPageSize();
        }
        if (pageSize <= 0) {
            return maxPageSize;
        }
        if (maxPageSize > 0 && maxPageSize < pageSize) {
            return maxPageSize;
        }
        return pageSize;
    }

    /**
     * Returns an integer keeping track of the higher page index giving
     * results. Useful for enabling or disabling the nextPage action when
     * number of results cannot be known.
     *
     * @since 5.5
     */
    public int getCurrentHigherNonEmptyPageIndex() {
        return currentHigherNonEmptyPageIndex;
    }

    /**
     * Returns the page limit. The n first page we know they exist.
     *
     * @since 5.8
     */
    public long getPageLimit() {
        return PAGE_LIMIT_UNKNOWN;
    }

    public void setCurrentHigherNonEmptyPageIndex(int higherFilledPageIndex) {
        this.currentHigherNonEmptyPageIndex = higherFilledPageIndex;
    }

    /**
     * Returns the maximum number of empty pages that can be fetched empty
     * (defaults to 1). Can be useful for displaying pages of a provider
     * without results count.
     *
     * @since 5.5
     */
    public int getMaxNumberOfEmptyPages() {
        return 1;
    }

    protected long getDefaultMaxPageSize() {
        long res = DEFAULT_MAX_PAGE_SIZE;
        if (Framework.isInitialized()) {
            String maxPageSize = Framework.getProperty(DEFAULT_MAX_PAGE_SIZE_RUNTIME_PROP);
            if (!StringUtils.isBlank(maxPageSize)) {
                try {
                    res = Long.parseLong(maxPageSize.trim());
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Invalid max page size defined for property "
                                    + "\"%s\": %s (waiting for a long value)",
                            DEFAULT_MAX_PAGE_SIZE_RUNTIME_PROP, maxPageSize));
                }
            }
        }
        return res;
    }

    @Override
    public void setPageProviderChangedListener(
            PageProviderChangedListener listener) {
        pageProviderChangedListener = listener;
    }

    /**
     * Call the registered {@code PageProviderChangedListener}, if any, to
     * notify that the page provider current page has changed.
     *
     * @since 5.7
     */
    protected void notifyPageChanged() {
        if (pageProviderChangedListener != null) {
            pageProviderChangedListener.pageChanged(this);
        }
    }

    /**
     * Call the registered {@code PageProviderChangedListener}, if any, to
     * notify that the page provider has refreshed.
     *
     * @since 5.7
     */
    protected void notifyRefresh() {
        if (pageProviderChangedListener != null) {
            pageProviderChangedListener.refreshed(this);
        }
    }

    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(getParameters(), parameters);
    }

    protected boolean getParametersChanged(Object[] oldParams,
            Object[] newParams) {
        if (oldParams == null && newParams == null) {
            return true;
        } else if (oldParams != null && newParams != null) {
            if (oldParams.length != newParams.length) {
                return true;
            }
            for (int i = 0; i < oldParams.length; i++) {
                if (oldParams[i] == null && newParams[i] == null) {
                    continue;
                } else if (newParams[i] instanceof String[]
                        && oldParams[i] instanceof String[]
                        && Arrays.equals((String[]) oldParams[i],
                                (String[]) newParams[i])) {
                    continue;
                } else if (oldParams[i] != null
                        && !oldParams[i].equals(newParams[i])) {
                    return true;
                } else if (newParams[i] != null
                        && !newParams[i].equals(oldParams[i])) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public AggregateQuery[] getAggregatesQuery() {
        if (aggregateQuery == null) {
            AggregateDefinition[] aggDefinitions = definition.getAggregates();
            if (aggDefinitions.length == 0) {
                aggregateQuery = EMPTY_AGGREGATION_QUERY;
            } else {
                aggregateQuery = new AggregateQuery[aggDefinitions.length];
                int i = 0;
                for(AggregateDefinition def: aggDefinitions){
                    aggregateQuery[i++] = new AggregateQuery(def, searchDocumentModel);
                }
            }
        }
        return aggregateQuery;
    }

    @Override
    public List<Aggregate> getAggregates() {
        throw new NotImplementedException();
    }

}
