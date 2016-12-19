/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.api;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Basic implementation for a {@link PageProvider}.
 * <p>
 * Provides next/prev standard logics, and helper methods for retrieval of items and first/next/prev/last buttons
 * display as well as other display information (number of pages for instance).
 * <p>
 * Also handles selection by providing a default implementation of {@link #getCurrentSelectPage()} working in
 * conjunction with {@link #setSelectedEntries(List)}.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractPageProvider<T> implements PageProvider<T> {

    public static final Log log = LogFactory.getLog(AbstractPageProvider.class);

    private static final long serialVersionUID = 1L;

    /**
     * property used to enable globally tracking : property should contains the list of pageproviders to be tracked
     *
     * @since 7.4
     */
    public static final String PAGEPROVIDER_TRACK_PROPERTY_NAME = "nuxeo.pageprovider.track";

    /**
     * lists schemas prefixes that should be skipped when extracting "search fields" (tracking) from searchDocumentModel
     *
     * @since 7.4
     */
    protected static final List<String> SKIPPED_SCHEMAS_FOR_SEARCHFIELD = Collections.singletonList("cvd");

    protected String name;

    protected long offset = 0;

    protected long pageSize = 0;

    protected List<Long> pageSizeOptions;

    protected long maxPageSize = getDefaultMaxPageSize();

    protected long resultsCount = UNKNOWN_SIZE;

    protected int currentEntryIndex = 0;

    /**
     * Integer keeping track of the higher page index giving results. Useful for enabling or disabling the nextPage
     * action when number of results cannot be known.
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

    /**
     * @since 8.4
     */
    protected List<QuickFilter> quickFilters;

    protected String errorMessage;

    protected Throwable error;

    protected PageProviderDefinition definition;

    protected PageProviderChangedListener pageProviderChangedListener;

    /**
     * Returns the list of current page items.
     * <p>
     * Custom implementation can be added here, based on the page provider properties, parameters and
     * {@link WhereClauseDefinition} on the {@link PageProviderDefinition}, as well as search document, sort
     * information, etc...
     * <p>
     * Implementation of this method usually consists in setting a non-null value to a field caching current items, and
     * nullifying this field by overriding {@link #pageChanged()} and {@link #refresh()}.
     * <p>
     * Fields {@link #errorMessage} and {@link #error} can also be filled to provide accurate feedback in case an error
     * occurs during the search.
     * <p>
     * When items are retrieved, a call to {@link #setResultsCount(long)} should be made to ensure proper pagination as
     * implemented in this abstract class. The implementation in {@link CoreQueryAndFetchPageProvider} is a good example
     * when the total results count is known.
     * <p>
     * If for performance reasons, for instance, the number of results cannot be known, a fall-back strategy can be
     * applied to provide the "next" button but not the "last" one, by calling
     * {@link #getCurrentHigherNonEmptyPageIndex()} and {@link #setCurrentHigherNonEmptyPageIndex(int)}. In this case,
     * {@link CoreQueryDocumentPageProvider} is a good example.
     */
    @Override
    public abstract List<T> getCurrentPage();

    /**
     * Page change hook, to override for custom behavior
     * <p>
     * When overriding it, call {@code super.pageChanged()} as last statement to make sure that the
     * {@link PageProviderChangedListener} is called with the up-to-date @{code PageProvider} state.
     */
    protected void pageChanged() {
        currentEntryIndex = 0;
        currentSelectPage = null;
        notifyPageChanged();
    }

    @Override
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

    @Override
    public long getCurrentPageIndex() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            return 0;
        }
        long offset = getCurrentPageOffset();
        return offset / pageSize;
    }

    @Override
    public long getCurrentPageOffset() {
        return offset;
    }

    @Override
    public void setCurrentPageOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public long getCurrentPageSize() {
        List<T> currentItems = getCurrentPage();
        if (currentItems != null) {
            return currentItems.size();
        }
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
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

    @Override
    public List<T> setCurrentPage(long page) {
        setCurrentPageIndex(page);
        return getCurrentPage();
    }

    @Override
    public long getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(long pageSize) {
        long localPageSize = getPageSize();
        if (localPageSize != pageSize) {
            this.pageSize = pageSize;
            // reset offset too
            setCurrentPageOffset(0);
            refresh();
        }
    }

    @Override
    public List<Long> getPageSizeOptions() {
        List<Long> res = new ArrayList<>();
        if (pageSizeOptions != null) {
            res.addAll(pageSizeOptions);
        }
        // include the actual page size of page provider if not present
        long ppsize = getPageSize();
        if (ppsize > 0 && !res.contains(ppsize)) {
            res.add(Long.valueOf(ppsize));
        }
        Collections.sort(res);
        return res;
    }

    @Override
    public void setPageSizeOptions(List<Long> options) {
        pageSizeOptions = options;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        // break reference
        List<SortInfo> res = new ArrayList<>();
        if (sortInfos != null) {
            res.addAll(sortInfos);
        }
        return res;
    }

    @Override
    public SortInfo getSortInfo() {
        if (sortInfos != null && !sortInfos.isEmpty()) {
            return sortInfos.get(0);
        }
        return null;
    }

    protected boolean sortInfoChanged(List<SortInfo> oldSortInfos, List<SortInfo> newSortInfos) {
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

    @Override
    public void setQuickFilters(List<QuickFilter> quickFilters) {
        this.quickFilters = quickFilters;
    }

    @Override
    public List<QuickFilter> getQuickFilters() {
        return quickFilters;
    }

    @Override
    public List<QuickFilter> getAvailableQuickFilters() {
        return definition != null ? definition.getQuickFilters() : null;
    }

    @Override
    public void addQuickFilter(QuickFilter quickFilter) {
        if (quickFilters == null) {
            quickFilters = new ArrayList<>();
        }
        quickFilters.add(quickFilter);
    }

    @Override
    public void setSortInfos(List<SortInfo> sortInfo) {
        if (sortInfoChanged(this.sortInfos, sortInfo)) {
            this.sortInfos = sortInfo;
            refresh();
        }
    }

    @Override
    public void setSortInfo(SortInfo sortInfo) {
        List<SortInfo> newSortInfos = new ArrayList<>();
        if (sortInfo != null) {
            newSortInfos.add(sortInfo);
        }
        setSortInfos(newSortInfos);
    }

    @Override
    public void setSortInfo(String sortColumn, boolean sortAscending, boolean removeOtherSortInfos) {
        if (removeOtherSortInfos) {
            SortInfo sortInfo = new SortInfo(sortColumn, sortAscending);
            setSortInfo(sortInfo);
        } else {
            if (getSortInfoIndex(sortColumn, sortAscending) != -1) {
                // do nothing: sort on this column is not set
            } else if (getSortInfoIndex(sortColumn, !sortAscending) != -1) {
                // change direction
                List<SortInfo> newSortInfos = new ArrayList<>();
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

    @Override
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

    @Override
    public int getSortInfoIndex(String sortColumn, boolean sortAscending) {
        List<SortInfo> sortInfos = getSortInfos();
        if (sortInfos == null || sortInfos.isEmpty()) {
            return -1;
        } else {
            SortInfo sortInfo = new SortInfo(sortColumn, sortAscending);
            return sortInfos.indexOf(sortInfo);
        }
    }

    @Override
    public boolean isNextPageAvailable() {
        long pageSize = getPageSize();
        if (pageSize == 0) {
            return false;
        }
        long resultsCount = getResultsCount();
        if (resultsCount < 0) {
            long currentPageIndex = getCurrentPageIndex();
            return currentPageIndex < getCurrentHigherNonEmptyPageIndex() + getMaxNumberOfEmptyPages();
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

    @Override
    public boolean isPreviousPageAvailable() {
        long offset = getCurrentPageOffset();
        return offset > 0;
    }

    @Override
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

    @Override
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

    @Override
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
     * Refresh hook, to override for custom behavior
     * <p>
     * When overriding it, call {@code super.refresh()} as last statement to make sure that the
     * {@link PageProviderChangedListener} is called with the up-to-date @{code PageProvider} state.
     */
    @Override
    public void refresh() {
        setResultsCount(UNKNOWN_SIZE);
        setCurrentHigherNonEmptyPageIndex(-1);
        currentSelectPage = null;
        errorMessage = null;
        error = null;
        notifyRefresh();

    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCurrentPageStatus() {
        long total = getNumberOfPages();
        long current = getCurrentPageIndex() + 1;
        if (total <= 0) {
            // number of pages unknown or there is only one page
            return String.format("%d", Long.valueOf(current));
        } else {
            return String.format("%d/%d", Long.valueOf(current), Long.valueOf(total));
        }
    }

    @Override
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

    @Override
    public boolean isPreviousEntryAvailable() {
        return (currentEntryIndex != 0 || isPreviousPageAvailable());
    }

    @Override
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

    @Override
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

    @Override
    public T getCurrentEntry() {
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            return null;
        }
        return currentPage.get(currentEntryIndex);
    }

    @Override
    public void setCurrentEntry(T entry) {
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            throw new NuxeoException(String.format("Entry '%s' not found in current page", entry));
        }
        int i = currentPage.indexOf(entry);
        if (i == -1) {
            throw new NuxeoException(String.format("Entry '%s' not found in current page", entry));
        }
        currentEntryIndex = i;
    }

    @Override
    public void setCurrentEntryIndex(long index) {
        int intIndex = new Long(index).intValue();
        List<T> currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isEmpty()) {
            throw new NuxeoException(String.format("Index %s not found in current page", new Integer(intIndex)));
        }
        if (index >= currentPage.size()) {
            throw new NuxeoException(String.format("Index %s not found in current page", new Integer(intIndex)));
        }
        currentEntryIndex = intIndex;
    }

    @Override
    public long getResultsCount() {
        return resultsCount;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        // break reference
        return new HashMap<>(properties);
    }

    @Override
    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    /**
     * @since 6.0
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

    @Override
    public void setResultsCount(long resultsCount) {
        this.resultsCount = resultsCount;
        setCurrentHigherNonEmptyPageIndex(-1);
    }

    @Override
    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    @Override
    public boolean isSortable() {
        return sortable;
    }

    @Override
    public PageSelections<T> getCurrentSelectPage() {
        if (currentSelectPage == null) {
            List<PageSelection<T>> entries = new ArrayList<>();
            List<T> currentPage = getCurrentPage();
            currentSelectPage = new PageSelections<>();
            currentSelectPage.setName(name);
            if (currentPage != null && !currentPage.isEmpty()) {
                if (selectedEntries == null || selectedEntries.isEmpty()) {
                    // no selection at all
                    for (T entry : currentPage) {
                        entries.add(new PageSelection<>(entry, false));
                    }
                } else {
                    boolean allSelected = true;
                    for (T entry : currentPage) {
                        Boolean selected = Boolean.valueOf(selectedEntries.contains(entry));
                        if (!Boolean.TRUE.equals(selected)) {
                            allSelected = false;
                        }
                        entries.add(new PageSelection<>(entry, selected.booleanValue()));
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

    @Override
    public void setSelectedEntries(List<T> entries) {
        this.selectedEntries = entries;
        // reset current select page so that it's rebuilt
        currentSelectPage = null;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public DocumentModel getSearchDocumentModel() {
        return searchDocumentModel;
    }

    protected boolean searchDocumentModelChanged(DocumentModel oldDoc, DocumentModel newDoc) {
        if (oldDoc == null && newDoc == null) {
            return false;
        } else if (oldDoc == null || newDoc == null) {
            return true;
        }
        // do not compare properties and assume it's changed
        return true;
    }

    @Override
    public void setSearchDocumentModel(DocumentModel searchDocumentModel) {
        if (searchDocumentModelChanged(this.searchDocumentModel, searchDocumentModel)) {
            refresh();
        }
        this.searchDocumentModel = searchDocumentModel;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
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

    @Override
    public long getMaxPageSize() {
        return maxPageSize;
    }

    @Override
    public void setMaxPageSize(long maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    /**
     * Returns the minimal value for the max page size, taking the lower value between the requested page size and the
     * maximum accepted page size.
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
     * Returns an integer keeping track of the higher page index giving results. Useful for enabling or disabling the
     * nextPage action when number of results cannot be known.
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
    @Override
    public long getPageLimit() {
        return PAGE_LIMIT_UNKNOWN;
    }

    public void setCurrentHigherNonEmptyPageIndex(int higherFilledPageIndex) {
        this.currentHigherNonEmptyPageIndex = higherFilledPageIndex;
    }

    /**
     * Returns the maximum number of empty pages that can be fetched empty (defaults to 1). Can be useful for displaying
     * pages of a provider without results count.
     *
     * @since 5.5
     */
    public int getMaxNumberOfEmptyPages() {
        return 1;
    }

    protected long getDefaultMaxPageSize() {
        long res = DEFAULT_MAX_PAGE_SIZE;
        if (Framework.isInitialized()) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            String maxPageSize = cs.getProperty(DEFAULT_MAX_PAGE_SIZE_RUNTIME_PROP);
            if (!StringUtils.isBlank(maxPageSize)) {
                try {
                    res = Long.parseLong(maxPageSize.trim());
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Invalid max page size defined for property " + "\"%s\": %s (waiting for a long value)",
                            DEFAULT_MAX_PAGE_SIZE_RUNTIME_PROP, maxPageSize));
                }
            }
        }
        return res;
    }

    @Override
    public void setPageProviderChangedListener(PageProviderChangedListener listener) {
        pageProviderChangedListener = listener;
    }

    /**
     * Call the registered {@code PageProviderChangedListener}, if any, to notify that the page provider current page
     * has changed.
     *
     * @since 5.7
     */
    protected void notifyPageChanged() {
        if (pageProviderChangedListener != null) {
            pageProviderChangedListener.pageChanged(this);
        }
    }

    /**
     * Call the registered {@code PageProviderChangedListener}, if any, to notify that the page provider has refreshed.
     *
     * @since 5.7
     */
    protected void notifyRefresh() {
        if (pageProviderChangedListener != null) {
            pageProviderChangedListener.refreshed(this);
        }
    }

    @Override
    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(getParameters(), parameters);
    }

    protected boolean getParametersChanged(Object[] oldParams, Object[] newParams) {
        if (oldParams == null && newParams == null) {
            return true;
        } else if (oldParams != null && newParams != null) {
            if (oldParams.length != newParams.length) {
                return true;
            }
            for (int i = 0; i < oldParams.length; i++) {
                if (oldParams[i] == null && newParams[i] == null) {
                    continue;
                } else if (newParams[i] instanceof String[] && oldParams[i] instanceof String[]
                        && Arrays.equals((String[]) oldParams[i], (String[]) newParams[i])) {
                    continue;
                } else if (oldParams[i] != null && !oldParams[i].equals(newParams[i])) {
                    return true;
                } else if (newParams[i] != null && !newParams[i].equals(oldParams[i])) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public List<AggregateDefinition> getAggregateDefinitions() {
        return definition.getAggregates();
    }

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasAggregateSupport() {
        return false;
    }

    protected Boolean tracking = null;

    /**
     * @since 7.4
     */
    protected boolean isTrackingEnabled() {

        if (tracking != null) {
            return tracking;
        }

        if (getDefinition().isUsageTrackingEnabled()) {
            tracking = true;
        } else {
            String trackedPageProviders = Framework.getProperty(PAGEPROVIDER_TRACK_PROPERTY_NAME, "");
            if ("*".equals(trackedPageProviders)) {
                tracking = true;
            } else {
                List<String> pps = Arrays.asList(trackedPageProviders.split(","));
                if (pps.contains(getDefinition().getName())) {
                    tracking = true;
                } else {
                    tracking = false;
                }
            }
        }
        return tracking;
    }

    /**
     * Send a search event so that PageProvider calls can be tracked by Audit or other statistic gathering process
     *
     * @since 7.4
     */
    protected void fireSearchEvent(Principal principal, String query, List<T> entries, Long executionTimeMs) {

        if (!isTrackingEnabled()) {
            return;
        }

        Map<String, Serializable> props = new HashMap<>();

        props.put("pageProviderName", getDefinition().getName());

        props.put("effectiveQuery", query);
        props.put("searchPattern", getDefinition().getPattern());
        props.put("queryParams", getDefinition().getQueryParameters());
        props.put("params", getParameters());
        WhereClauseDefinition wc = getDefinition().getWhereClause();
        if (wc != null) {
            props.put("whereClause_fixedPart", wc.getFixedPart());
            props.put("whereClause_select", wc.getSelectStatement());
        }

        DocumentModel searchDocumentModel = getSearchDocumentModel();
        if (searchDocumentModel != null && !(searchDocumentModel instanceof SimpleDocumentModel)) {
            RenderingContext rCtx = RenderingContext.CtxBuilder.properties("*").get();
            try {
                // the SearchDocumentModel is not a Document bound to the repository
                // - it may not survive the Event Stacking (ShallowDocumentModel)
                // - it may take too much space in memory
                // => let's use JSON
                String searchDocumentModelAsJson = MarshallerHelper.objectToJson(DocumentModel.class,
                        searchDocumentModel, rCtx);
                props.put("searchDocumentModelAsJson", searchDocumentModelAsJson);
            } catch (IOException e) {
                log.error("Unable to Marshall SearchDocumentModel as JSON", e);
            }

            ArrayList<String> searchFields = new ArrayList<>();
            // searchFields collects the non- null fields inside the SearchDocumentModel
            // some schemas are skipped because they contains ContentView related info
            for (String schema : searchDocumentModel.getSchemas()) {
                for (Property prop : searchDocumentModel.getPropertyObjects(schema)) {
                    if (prop.getValue() != null
                            && !SKIPPED_SCHEMAS_FOR_SEARCHFIELD.contains(prop.getSchema().getNamespace().prefix)) {
                        if (prop.isList()) {
                            if (ArrayUtils.isNotEmpty(prop.getValue(Object[].class))) {
                                searchFields.add(prop.getPath());
                            }
                        } else {
                            searchFields.add(prop.getPath());
                        }
                    }
                }
            }
            props.put("searchFields", searchFields);
        }

        if (entries != null) {
            props.put("resultsCountInPage", entries.size());
        }
        props.put("resultsCount", getResultsCount());
        props.put("pageSize", getPageSize());
        props.put("pageIndex", getCurrentPageIndex());
        props.put("principal", principal.getName());

        if (executionTimeMs != null) {
            props.put("executionTimeMs", executionTimeMs);
        }

        incorporateAggregates(props);

        EventService es = Framework.getService(EventService.class);
        EventContext ctx = new UnboundEventContext(principal, props);
        es.fireEvent(ctx.newEvent("search"));
    }

    /**
     * Default (dummy) implementation that should be overridden by PageProvider actually dealing with Aggregates
     *
     * @since 7.4
     */
    protected void incorporateAggregates(Map<String, Serializable> eventProps) {

        List<AggregateDefinition> ags = getDefinition().getAggregates();
        if (ags != null) {
            ArrayList<HashMap<String, Serializable>> aggregates = new ArrayList<>();
            for (AggregateDefinition ag : ags) {
                HashMap<String, Serializable> agData = new HashMap<>();
                agData.put("type", ag.getType());
                agData.put("id", ag.getId());
                agData.put("field", ag.getDocumentField());
                agData.putAll(ag.getProperties());
                ArrayList<HashMap<String, Serializable>> rangesData = new ArrayList<>();
                if (ag.getDateRanges() != null) {
                    for (AggregateRangeDateDefinition range : ag.getDateRanges()) {
                        HashMap<String, Serializable> rangeData = new HashMap<>();
                        rangeData.put("from", range.getFromAsString());
                        rangeData.put("to", range.getToAsString());
                        rangesData.add(rangeData);
                    }
                    for (AggregateRangeDefinition range : ag.getRanges()) {
                        HashMap<String, Serializable> rangeData = new HashMap<>();
                        rangeData.put("from-dbl", range.getFrom());
                        rangeData.put("to-dbl", range.getTo());
                        rangesData.add(rangeData);
                    }
                }
                agData.put("ranges", rangesData);
                aggregates.add(agData);
            }
            eventProps.put("aggregates", aggregates);
        }

    }
}
