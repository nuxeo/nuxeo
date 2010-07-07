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
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic implementation for a {@link PageProvider}
 * <p>
 * Fields to fill at construction: name, pageSize, resultsCount, sortable?
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractPageProvider<T extends Serializable> implements
        PageProvider<T> {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected long offset = 0;

    protected long pageSize = 0;

    protected long resultsCount = UNKNOWN_SIZE;

    protected int currentEntryIndex = 0;

    protected List<SortInfo> sortInfos;

    protected boolean sortable = false;

    public abstract List<T> getCurrentPage();

    /**
     * Page change hook
     */
    protected void pageChanged() {
        // nothing to do here, to override for custom behaviour
    }

    public void firstPage() {
        if (offset != 0) {
            offset = 0;
            pageChanged();
            refresh();
        }
    }

    public long getCurrentPageIndex() {
        if (pageSize == 0) {
            return 0;
        }
        return offset / pageSize;
    }

    public long getCurrentPageOffset() {
        return offset;
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
        if (pageSize == 0) {
            return 1;
        }
        return (int) (1 + (getResultsCount() - 1) / pageSize);
    }

    public List<T> getPage(long page) {
        offset = page * pageSize;
        pageChanged();
        refresh();
        return getCurrentPage();
    }

    public long getPageSize() {
        return pageSize;
    }

    public List<SortInfo> getSortInfos() {
        return sortInfos;
    }

    public SortInfo getSortInfo() {
        if (sortInfos != null && !sortInfos.isEmpty()) {
            return sortInfos.get(0);
        }
        return null;
    }

    public void setSortInfos(List<SortInfo> sortInfo) {
        this.sortInfos = sortInfo;
    }

    public void setSortInfo(SortInfo sortInfo) {
        this.sortInfos = new ArrayList<SortInfo>();
        if (sortInfo != null) {
            this.sortInfos.add(sortInfo);
        }
    }

    public boolean isNextPageAvailable() {
        if (pageSize == 0) {
            return false;
        }
        return getResultsCount() > pageSize + offset;
    }

    public boolean isPreviousPageAvailable() {
        return offset > 0;
    }

    public void lastPage() {
        offset = (int) (getResultsCount() - getResultsCount() % pageSize);
        pageChanged();
        refresh();
    }

    public void nextPage() {
        offset += pageSize;
        pageChanged();
        refresh();
    }

    public void previousPage() {
        if (offset >= pageSize) {
            offset -= pageSize;
            pageChanged();
            refresh();
        }
    }

    public void refresh() {
        currentEntryIndex = 0;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentPageStatus() {
        long total = getNumberOfPages();
        long current = getCurrentPageIndex() + 1;
        if (total == UNKNOWN_SIZE) {
            return String.format("%d", Long.valueOf(current));
        } else {
            return String.format("%d/%d", Long.valueOf(current),
                    Long.valueOf(total));
        }
    }

    public boolean isNextEntryAvailable() {
        if (pageSize == 0) {
            return currentEntryIndex < getResultsCount() - 1;
        } else {
            return (currentEntryIndex < (getResultsCount() % pageSize) - 1 || isNextPageAvailable());
        }
    }

    public boolean isPreviousEntryAvailable() {
        return (currentEntryIndex != 0 || isPreviousPageAvailable());
    }

    public void nextEntry() {
        if ((pageSize == 0 && currentEntryIndex < getResultsCount())
                || (currentEntryIndex < getCurrentPageSize() - 1)) {
            currentEntryIndex++;
            return;
        }
        if (!isNextPageAvailable()) {
            return;
        }

        nextPage();
        if (getCurrentPage().isEmpty()) {
            // things have changed since last query
            currentEntryIndex = 0;
        } else {
            currentEntryIndex = 0;
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
        if (getCurrentPage().isEmpty()) {
            // things may have changed
            currentEntryIndex = 0;
        } else {
            currentEntryIndex = (new Long(getPageSize() - 1)).intValue();
        }
    }

    public T getCurrentEntry() {
        if (getCurrentPage().isEmpty()) {
            return null;
        }
        return getCurrentPage().get(currentEntryIndex);
    }

    public void setCurrentEntry(T entry) throws ClientException {
        int i = getCurrentPage().indexOf(entry);
        if (i == -1) {
            throw new ClientException("Entry not found in current page");
        }
        currentEntryIndex = i;
    }

    public long getResultsCount() {
        return resultsCount;
    }

    public boolean isSortable() {
        return sortable;
    }

}
