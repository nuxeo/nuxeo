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

import org.nuxeo.drive.service.impl.FileSystemItemChange;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

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
     * @param rootPaths the synchronization root paths
     * @param lastSuccessfulSyncDate the last successful synchronization date of
     *            the user's device
     * @param syncDate the current synchronization date
     * @param limit the maximum number of changes to fetch
     * @return the list of document changes
     * @throws TooManyChangesException if the number of changes found has
     *             exceeded the limit
     * @throws ClientException if the access to the repository fails for another
     *             reason.
     */
    public List<FileSystemItemChange> getFileSystemChanges(CoreSession session,
            Set<String> rootPaths, long lastSuccessfulSyncDate, long syncDate,
            int limit) throws TooManyChangesException, ClientException;

}
