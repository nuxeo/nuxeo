/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Allows to find document changes.
 *
 * @author Antoine Taillefer
 */
public interface FileSystemChangeFinder extends Serializable {

    /**
     * Gets the changes in the repository against which the given session is
     * bound for the given synchronization root paths, since the given last
     * successful synchronization date and without exceeding the given limit.
     *
     * The change summaries are mapped back to the file system view: the file
     * system items might not always have the same tree layout as the backing
     * documents in the repositories but this is a back-end detail that the
     * client does not have to deal with.
     *
     * @param session the session bound to a specific repository
     * @param lastActiveRootRefs docrefs of the roots as reported by the last
     *            successful synchronization (can be empty or null)
     * @param activeRoots the currently active synchronization roots
     * @param lastSuccessfulSyncDate the last successful synchronization date as
     *            measured on the server for this user device.
     * @param syncDate the current synchronization date (upper bound on the date
     *            of the changes to return). This date is typically obtained by
     *            calling {@link #getCurrentDate()}
     * @param limit the maximum number of changes to fetch
     * @return the list of document changes
     * @throws TooManyChangesException if the number of changes found has
     *             exceeded the limit
     * @throws ClientException if the access to the repository fails for another
     *             reason.
     */
    List<FileSystemItemChange> getFileSystemChanges(CoreSession session,
            Set<IdRef> lastActiveRootRefs, SynchronizationRoots activeRoots,
            long lastSuccessfulSyncDate, long syncDate, int limit)
            throws ClientException, TooManyChangesException;

    /**
     * Gets the changes in the repository against which the given session is
     * bound for the given synchronization root paths, between the given lower
     * and upper integer bounds and without exceeding the given limit.
     *
     * The change summaries are mapped back to the file system view: the file
     * system items might not always have the same tree layout as the backing
     * documents in the repositories but this is a back-end detail that the
     * client does not have to deal with.
     *
     * @param session the session bound to a specific repository
     * @param lastActiveRootRefs docrefs of the roots as reported by the last
     *            successful synchronization (can be empty or null)
     * @param activeRoots the currently active synchronization roots
     * @param collectionSyncRootMemberIds the collection sync root member ids
     * @param lowerBound the lower integer bound of the range clause in the
     *            change query
     * @param upperBound the upper integer bound of the range clause in the
     *            change query. This id is typically obtained by calling
     *            {@link #getUpperBound())}
     * @param limit the maximum number of changes to fetch
     * @return the list of document changes
     * @throws TooManyChangesException if the number of changes found has
     *             exceeded the limit
     * @throws ClientException if the access to the repository fails for another
     *             reason.
     */
    List<FileSystemItemChange> getFileSystemChangesIntegerBounds(
            CoreSession session, Set<IdRef> lastActiveRootRefs,
            SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound,
            long upperBound, int limit) throws ClientException,
            TooManyChangesException;

    /**
     * Read the current time code to query for changes. The time is truncated to
     * 0 milliseconds to have a consistent behavior across databases.
     *
     * Call to this method should be monotonic (or very nearly monotonic).
     */
    long getCurrentDate();

    /**
     * Return the upper bound of the range clause in the change query.
     */
    long getUpperBound();

}
