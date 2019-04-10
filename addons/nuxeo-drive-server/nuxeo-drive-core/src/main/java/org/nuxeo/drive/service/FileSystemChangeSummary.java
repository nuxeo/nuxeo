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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;
import java.util.List;

/**
 * Summary of file system changes, including:
 * <ul>
 * <li>A list of file system item changes</li>
 * <li>A global status code</li>
 * </ul>
 * A document change is implemented by {@link FileSystemItemChange}.
 *
 * @author Antoine Taillefer
 */
public interface FileSystemChangeSummary extends Serializable {

    List<FileSystemItemChange> getFileSystemChanges();

    void setFileSystemChanges(List<FileSystemItemChange> changes);

    /**
     * @return the time code of current sync operation in milliseconds since
     *         1970-01-01 UTC rounded to the second as measured on the server
     *         clock. This value can be passed to the next call to
     *         {@link NuxeoDriveManager#getChangeSummary(java.security.Principal, java.util.Map, long)}
     *         to get strictly monotonic change summaries (without overlap). In
     *         this case, changes from the current summary instance all happen
     *         "strictly" before this time code.
     */
    Long getSyncDate();

    /**
     * @return the upper bound of the range clause in the change query. Changes
     *         from the current summary instance all happen "strictly before"
     *         this bound. This value is expected to be passed to the next call
     *         to
     *         {@link NuxeoDriveManager#getChangeSummaryIntegerBounds(java.security.Principal, java.util.Map, long)}
     *         to get strictly monotonic change summaries (without overlap).
     */
    Long getUpperBound();

    String getActiveSynchronizationRootDefinitions();

    void setActiveSynchronizationRootDefinitions(
            String activeSynchronizationRootDefinitions);

    void setSyncDate(Long syncDate);

    void setUpperBound(Long upperBound);

    void setHasTooManyChanges(Boolean hasTooManyChanges);

    Boolean getHasTooManyChanges();

}
