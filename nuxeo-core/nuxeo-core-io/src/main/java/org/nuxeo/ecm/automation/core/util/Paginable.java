/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.util;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

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

}
