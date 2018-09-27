/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Allows to find document changes.
 *
 * @author Antoine Taillefer
 */
public interface FileSystemChangeFinder extends Serializable {

    /**
     * Handles the parameters contributed through the {@code changeFinder} contribution.
     */
    void handleParameters(Map<String, String> parameters);

    /**
     * Gets the changes in the repository against which the given session is bound for the given synchronization root
     * paths, between the given lower and upper integer bounds and without exceeding the given limit. The change
     * summaries are mapped back to the file system view: the file system items might not always have the same tree
     * layout as the backing documents in the repositories but this is a back-end detail that the client does not have
     * to deal with.
     *
     * @param session the session bound to a specific repository
     * @param lastActiveRootRefs docrefs of the roots as reported by the last successful synchronization (can be empty
     *            or null)
     * @param activeRoots the currently active synchronization roots
     * @param collectionSyncRootMemberIds the collection sync root member ids
     * @param lowerBound the lower integer bound of the range clause in the change query
     * @param upperBound the upper integer bound of the range clause in the change query. This id is typically obtained
     *            by calling {@link #getUpperBound())}
     * @param limit the maximum number of changes to fetch
     * @return the list of document changes
     * @throws TooManyChangesException if the number of changes found has exceeded the limit
     */
    List<FileSystemItemChange> getFileSystemChanges(CoreSession session, Set<IdRef> lastActiveRootRefs,
            SynchronizationRoots activeRoots, Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound,
            int limit) throws TooManyChangesException;

    /**
     * Return the upper bound of the range clause in the change query.
     */
    long getUpperBound();

    /**
     * Returns the upper bound of the range clause in the change query taking into account the clustering delay if
     * clustering is enabled for at least one of the given repositories.
     *
     * @since 8.2
     */
    long getUpperBound(Set<String> repositoryNames);

}
