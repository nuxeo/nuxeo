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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo user.
 */
public interface NuxeoDriveManager {

    public static final String LOCALLY_EDITED_COLLECTION_NAME = "Locally Edited";

    /**
     * @param principal the Nuxeo Drive user
     * @param newRootContainer the folderish document to be used as synchronization root: must be bound to an active
     *            session
     * @throws SecurityException if the user does not have write permissions to the container.
     */
    public void registerSynchronizationRoot(Principal principal, DocumentModel newRootContainer, CoreSession session);

    /**
     * @param principal the Nuxeo Drive user
     * @param rootContainer the folderish document that should no longer be used as a synchronization root
     */
    public void unregisterSynchronizationRoot(Principal principal, DocumentModel rootContainer, CoreSession session);

    /**
     * Fetch the list of synchronization root refs for a given user and a given session repository. This list is assumed
     * to be short enough (in the order of 100 folder max) so that no paging API is required. The user is taken from the
     * session.getPrincipal() method.
     *
     * @param session active CoreSession instance to the repository hosting the roots.
     * @return the ordered set of non deleted synchronization root references for that user
     * @see #getSynchronizationRootPaths(String, CoreSession)
     */
    public Set<IdRef> getSynchronizationRootReferences(CoreSession session);

    /**
     * Fetch all the synchronization root references and paths for a given user. This list is assumed to be short enough
     * (in the order of 100 folder max) so that no paging API is required.
     *
     * @param principal the user to fetch the roots for
     * @return the map keyed by repository names all active roots definitions for the current user.
     */
    public Map<String, SynchronizationRoots> getSynchronizationRoots(Principal principal);

    /**
     * Fetch all the collection sync root member ids for a given user.
     *
     * @param principal the user to fetch the ids for
     * @return the map keyed by repository names all collection sync root member ids for the current user.
     */
    public Map<String, Set<String>> getCollectionSyncRootMemberIds(Principal principal);

    /**
     * Checks if the given {@link DocumentModel} is a synchronization root for the given user.
     */
    public boolean isSynchronizationRoot(Principal principal, DocumentModel doc);

    /**
     * Method to be called by a CoreEvent listener monitoring documents deletions to cleanup references to recently
     * deleted documents and invalidate the caches.
     */
    public void handleFolderDeletion(IdRef ref);

    /**
     * Gets a summary of document changes in all repositories for the given user's synchronization roots, since the
     * user's device last successful synchronization date.
     * <p>
     * The summary includes:
     * <ul>
     * <li>The list of sync root paths</li>
     * <li>A list of document changes</li>
     * <li>The document models that have changed</li>
     * <li>A status code</li>
     * </ul>
     *
     * @param principal
     * @param lastSyncRootRefs the map keyed by repository names of document refs for the synchronization roots that
     *            were active during last synchronization
     * @param lastSuccessfulSync the last successful synchronization date of the user's device. This time is expected to
     *            be in milliseconds since 1970-01-01 UTC as measured on the Nuxeo server clock, typically set to the
     *            value returned by {@link FileSystemChangeSummary#getSyncDate()} of the previous call to
     *            {@link NuxeoDriveManager#getChangeSummary(Principal, Map, long)} or 0 for catching every event since
     *            the repository initialization.
     * @return the summary of document changes
     */
    public FileSystemChangeSummary getChangeSummary(Principal principal, Map<String, Set<IdRef>> lastSyncRootRefs,
            long lastSuccessfulSync);

    /**
     * Gets a summary of document changes in all repositories for the given user's synchronization roots, from the lower
     * bound sent by the user's device.
     * <p>
     * The summary includes:
     * <ul>
     * <li>The list of sync root paths</li>
     * <li>A list of document changes</li>
     * <li>The document models that have changed</li>
     * <li>A status code</li>
     * </ul>
     *
     * @param principal
     * @param lastSyncRootRefs the map keyed by repository names of document refs for the synchronization roots that
     *            were active during last synchronization
     * @param lowerBound the lower bound sent by the user's device. Typically set to the value returned by
     *            {@link FileSystemChangeSummary#getUpperBound()} of the previous call to
     *            {@link NuxeoDriveManager#getChangeSummaryIntegerBounds(Principal, Map, long)} or 0 for catching every
     *            event since the repository initialization.
     * @return the summary of document changes
     */
    public FileSystemChangeSummary getChangeSummaryIntegerBounds(Principal principal,
            Map<String, Set<IdRef>> lastSyncRootRefs, long lowerBound);

    /**
     * Gets the {@link FileSystemChangeFinder} member.
     */
    public FileSystemChangeFinder getChangeFinder();

    /**
     * Invalidate the synchronization roots cache for a given user so as to query the repository next time
     * {@link #getSynchronizationRoots(Principal)} is called.
     *
     * @param userName the principal name of the user to invalidate the cache for.
     */
    void invalidateSynchronizationRootsCache(String userName);

    /**
     * Invalidate the collection sync root member cache for a given user so as to query the repository next time
     * {@link #getCollectionSyncRootMemberIds(Principal)} is called.
     *
     * @param userName the principal name of the user to invalidate the cache for.
     */
    void invalidateCollectionSyncRootMemberCache(String userName);

    /**
     * Invalidate the collection sync root member cache for all users so as to query the repository next time
     * {@link #getCollectionSyncRootMemberIds(Principal)} is called.
     */
    void invalidateCollectionSyncRootMemberCache();

    /**
     * Adds the given {@link DocumentModel} to the {@link #LOCALLY_EDITED_COLLECTION_NAME} collection.
     *
     * @since 6.0
     */
    void addToLocallyEditedCollection(CoreSession session, DocumentModel doc);

}
