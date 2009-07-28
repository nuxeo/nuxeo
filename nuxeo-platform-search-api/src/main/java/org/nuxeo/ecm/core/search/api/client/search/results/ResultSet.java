/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ResultSet.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.search.results;

import java.util.List;

import org.nuxeo.ecm.core.search.api.client.SearchException;

/**
 * Result set.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ResultSet extends List<ResultItem> {

    String ALWAYS_DETACH_SEARCH_RESULTS_KEY = "org.nuxeo.ecm.core.search.alwaysDetachResults";

    /**
     * Returns the current offset for this result set.
     *
     * @return the offset as an integer.
     */
    int getOffset();

    /**
     * Returns the amount of results from offset requested.
     *
     * @return the amount of results from offset requested.
     */
    int getRange();

    /**
     * Returns the total number of hits this resultset comes from.
     *
     * @return an integer value
     */
    int getTotalHits();

    /**
     * Returns the amount of actual matching results.
     * <p>This is in contrast to getRange() that returns the maximum
     * number of results per page. </p>
     *
     * @return the amount of actual matching results.
     */
    int getPageHits();

    /**
     * Replays the exact same query.
     *
     * @return a new, updated ResultSet
     * @throws SearchException
     */
    ResultSet replay() throws SearchException;

    /**
     * Replays the same query with new offset and range.
     *
     * @param offset the new offset
     * @param range the new range
     *
     * @return a new, updated ResultSet
     * @throws SearchException
     */
    ResultSet replay(int offset, int range) throws SearchException;

    /**
     * Computes the next page by replaying the exact same request.
     *
     * @return the next computed page or null if there is none.
     * @throws SearchException
     */
    ResultSet nextPage() throws SearchException;

    /**
     * Goes to requested page.
     *
     * @param page the page to go to
     *
     * @return the next computed page or null if there is none.
     * @throws SearchException
     */
    ResultSet goToPage(int page) throws SearchException;

    /**
     * Is there another page available?
     *
     * @return true if next page available / false if not.
     */
    boolean hasNextPage();

    /**
     * Is this result set the first page of results?
     *
     * @return true if first page / false of not.
     */
    boolean isFirstPage();

    /**
     * Computes the page number among the total set of results.
     *
     * @return the page number
     */
    int getPageNumber();

}
