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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.QuickFilter;

/**
 * Paginable object that uses a delegate to handle pagination.
 *
 * @since 5.8
 */
public class PaginableWithDelegate<T> extends ArrayList<T> implements Paginable<T> {

    private static final long serialVersionUID = 1L;

    private Paginable<T> delegate;

    /**
     * @param delegate
     */
    public PaginableWithDelegate(Paginable<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public long getPageSize() {
        return delegate.getPageSize();
    }

    @Override
    public long getMaxPageSize() {
        return delegate.getMaxPageSize();
    }

    @Override
    public long getResultsCount() {
        return delegate.getResultsCount();
    }

    @Override
    public long getNumberOfPages() {
        return delegate.getNumberOfPages();
    }

    @Override
    public boolean isNextPageAvailable() {
        return delegate.isNextPageAvailable();
    }

    @Override
    public boolean isLastPageAvailable() {
        return delegate.isLastPageAvailable();
    }

    @Override
    public boolean isPreviousPageAvailable() {
        return delegate.isPreviousPageAvailable();
    }

    @Override
    public long getCurrentPageSize() {
        return delegate.getCurrentPageSize();
    }

    @Override
    public long getCurrentPageIndex() {
        return delegate.getCurrentPageIndex();
    }

    @Override
    public boolean isSortable() {
        return delegate.isSortable();
    }

    @Override
    public boolean hasError() {
        return delegate.hasError();
    }

    @Override
    public String getErrorMessage() {
        return delegate.getErrorMessage();
    }

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        return delegate.getAggregates();
    }

    @Override
    public boolean hasAggregateSupport() {
        return delegate.hasAggregateSupport();
    }

    @Override
    public List<QuickFilter> getActiveQuickFilters() {
        return delegate.getActiveQuickFilters();
    }

    @Override
    public List<QuickFilter> getAvailableQuickFilters() {
        return delegate.getAvailableQuickFilters();
    }

    @Override
    public long getCurrentPageOffset() {
        return delegate.getCurrentPageOffset();
    }

    @Override
    public long getResultsCountLimit() {
        return delegate.getResultsCountLimit();
    }

}
