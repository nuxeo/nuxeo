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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;

/**
 * Paginable object that uses a delegate to handle pagination.
 *
 * @since 5.8
 */
@SuppressWarnings("rawtypes")
public class PaginableWithDelegate<T> extends ArrayList<T> implements
        Paginable<T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Paginable delegate;

    /**
     * @param delegate
     */
    public PaginableWithDelegate(Paginable delegate) {
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
    public List<AggregateQuery> getAggregatesQuery() {
        return delegate.getAggregatesQuery();
    }

    @Override
    public Map<String, Aggregate> getAggregates() {
        return delegate.getAggregates();
    }

    @Override
    public boolean hasAggregateSupport() {
        return delegate.hasAggregateSupport();
    }

}
