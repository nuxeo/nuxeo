/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.automation.core.util;

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.PageProvider;

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
}
