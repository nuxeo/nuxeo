/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.util;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.QuickFilter;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7 (extracted from PaginableDocumentModelList)
 */
public interface Paginable<T> extends List<T> {

    /**
     * Returns the number of results per page. 0 means no pagination.
     */
    long getPageSize();

    /**
     * Returns the max number of results per page. 0 means no pagination.
     * <p>
     * If page size is greater than this maximum value, it will be taken into account instead.
     */
    long getMaxPageSize();

    /**
     * Returns the number of result elements if available or a negative value if it is unknown:
     * <code>UNKNOWN_SIZE</code> if it is unknown as query was not done, and since 5.5,
     * <code>UNKNOWN_SIZE_AFTER_QUERY</code> if it is still unknown after query was done.
     */
    long getResultsCount();

    /**
     * Returns the total number of pages or 0 if number of pages is unknown.
     */
    long getNumberOfPages();

    /**
     * Returns a boolean expressing if there are further pages.
     */
    boolean isNextPageAvailable();

    /**
     * Returns a boolean expressing if the last page can be displayed.
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
     *
     * @since 9.3
     */
    long getCurrentPageOffset();

    /**
     * Returns the current page index as a zero-based integer.
     */
    long getCurrentPageIndex();

    /**
     * Returns if this provider is sortable.
     */
    boolean isSortable();

    boolean hasError();

    String getErrorMessage();

    /**
     * @since 6.0
     */
    Map<String, Aggregate<? extends Bucket>> getAggregates();

    /**
     * @since 6.0
     */
    boolean hasAggregateSupport();

    /**
     * @since 8.4
     */
    List<QuickFilter> getActiveQuickFilters();

    /**
     * @since 8.4
     */
    List<QuickFilter> getAvailableQuickFilters();

    /**
     * Limit of number of results beyond which the page provider may not be able to compute {@link #getResultsCount()}
     * or navigate.
     * <p>
     * Requesting results beyond this limit may result in error. When {@link #getResultsCount()} is negative, it means
     * there may be more results than this limit.
     * <p>
     * 0 means there is no limit.
     *
     * @since 9.3
     */
    long getResultsCountLimit();

}
