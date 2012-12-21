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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import org.nuxeo.drive.service.impl.FileSystemChangeSummary;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.security.SecurityException;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo
 * user.
 */
public interface NuxeoDriveManager {

    /**
     * @param userName the id of the Nuxeo Drive user
     * @param newRootContainer the folderish document to be used as
     *            synchronization root: must be bound to an active session
     * @throws ClientException
     * @throws SecurityException if the user does not have write permissions to
     *             the container.
     */
    public void registerSynchronizationRoot(String userName,
            DocumentModel newRootContainer, CoreSession session)
            throws ClientException, SecurityException;

    /**
     * @param userName the id of the Nuxeo Drive user
     * @param rootContainer the folderish document that should no longer be used
     *            as a synchronization root
     */
    public void unregisterSynchronizationRoot(String userName,
            DocumentModel rootContainer, CoreSession session)
            throws ClientException;

    /**
     * Fetch the list of synchronization root refs for a given user and a given
     * session repository. This list is assumed to be short enough (in the order
     * of 100 folder max) so that no paging API is required.
     *
     * The user is taken from the session.getPrincipal() method.
     *
     * @param session active CoreSession instance to the repository hosting the
     *            roots.
     * @return the ordered set of non deleted synchronization root references
     *         for that user
     * @see #getSynchronizationRootPaths(String, CoreSession)
     */
    public Set<IdRef> getSynchronizationRootReferences(CoreSession session) throws ClientException;

    /**
     * Fetch all the synchronization root references and paths for a given user.
     * This list is assumed to be short enough (in the order of 100 folder max)
     * so that no paging API is required.
     *
     * @param principal the user to fetch the roots for
     * @return the map keyed by repository names all active roots definitions
     *         for the current user.
     */
    public Map<String, SynchronizationRoots> getSynchronizationRoots(
            Principal principal) throws ClientException;

    /**
     * Method to be called by a CoreEvent listener monitoring documents
     * deletions to cleanup references to recently deleted documents and
     * invalidate the caches.
     */
    public void handleFolderDeletion(IdRef ref) throws ClientException;

    /**
     * Gets a summary of document changes in all repositories or in the
     * repository against which the given session is bound depending on the
     * {@code allRepositories} parameter, for the given user's synchronization
     * roots, since the user's device last successful synchronization date.
     * <p>
     * The summary includes:
     * <ul>
     * <li>The list of sync root paths</li>
     * <li>A list of document changes</li>
     * <li>The document models that have changed</li>
     * <li>A status code</li>
     * </ul>
     *
     * @param allRepositories if true then the document changes are retrieved
     *            from all repositories, else only from the one against which
     *            the given session is bound
     * @param userName the id of the Nuxeo Drive user
     * @param session active CoreSession instance to the repository hosting the
     *            user's synchronization roots
     * @param lastSuccessfulSync the last successful synchronization date of the
     *            user's device. This time is expected to be in milliseconds
     *            since 1970-01-01 UTC as measured on the Nuxeo server clock,
     *            typically set to the value returned
     *            {@code DocumentChangeSummary#getSyncDate()} of the previous
     *            call to
     *            {@code NuxeoDriveManager#getDocumentChangeSummary(boolean, String, CoreSession, long)}
     *            or 0 for catching every event since the repository
     *            initialization.
     * @return the summary of document changes
     */
    public FileSystemChangeSummary getChangeSummary(
            Principal principal, long lastSuccessfulSync)
            throws ClientException;

    /**
     * Sets the {@link FileSystemChangeFinder} member.
     * <p>
     * TODO: make it overridable with an extension point and remove setter.
     */
    public void setChangeFinder(FileSystemChangeFinder changeFinder);

}
