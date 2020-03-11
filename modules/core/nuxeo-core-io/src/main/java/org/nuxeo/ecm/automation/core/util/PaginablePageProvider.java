/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.QuickFilter;

/**
 * Wraps a {@link org.nuxeo.ecm.platform.query.api.PageProvider}.
 *
 * @since 5.7.3
 */
public class PaginablePageProvider<T> extends ArrayList<T> implements Paginable<T> {

    private static final long serialVersionUID = 1L;

    protected PageProvider<T> pageProvider;

    public PaginablePageProvider(PageProvider<T> pageProvider) {
        super(pageProvider.getCurrentPage());
        this.pageProvider = pageProvider;
    }

    @Override
    public long getPageSize() {
        return pageProvider.getPageSize();
    }

    @Override
    public long getMaxPageSize() {
        return pageProvider.getMaxPageSize();
    }

    @Override
    public long getResultsCount() {
        return pageProvider.getResultsCount();
    }

    @Override
    public long getNumberOfPages() {
        return pageProvider.getNumberOfPages();
    }

    @Override
    public boolean isNextPageAvailable() {
        return pageProvider.isNextPageAvailable();
    }

    @Override
    public boolean isLastPageAvailable() {
        return pageProvider.isLastPageAvailable();
    }

    @Override
    public boolean isPreviousPageAvailable() {
        return pageProvider.isPreviousPageAvailable();
    }

    @Override
    public long getCurrentPageSize() {
        return pageProvider.getCurrentPageSize();
    }

    @Override
    public long getCurrentPageIndex() {
        return pageProvider.getCurrentPageIndex();
    }

    @Override
    public long getCurrentPageOffset() {
        return pageProvider.getCurrentPageOffset();
    }

    @Override
    public boolean isSortable() {
        return pageProvider.isSortable();
    }

    @Override
    public boolean hasError() {
        return pageProvider.hasError();
    }

    @Override
    public String getErrorMessage() {
        return pageProvider.getErrorMessage();
    }

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        return pageProvider.getAggregates();
    }

    @Override
    public boolean hasAggregateSupport() {
        return pageProvider.hasAggregateSupport();
    }

    @Override
    public List<QuickFilter> getActiveQuickFilters() {
        return pageProvider.getQuickFilters();
    }

    @Override
    public List<QuickFilter> getAvailableQuickFilters() {
        return pageProvider.getAvailableQuickFilters();
    }

    @Override
    public long getResultsCountLimit() {
        return pageProvider.getResultsCountLimit();
    }
}
