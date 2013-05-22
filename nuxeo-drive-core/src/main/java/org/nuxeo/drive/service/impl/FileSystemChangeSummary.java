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
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.ecm.core.api.IdRef;

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
public class FileSystemChangeSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    protected List<FileSystemItemChange> fileSystemChanges;

    protected Long syncDate;

    protected Boolean hasTooManyChanges = Boolean.FALSE;

    protected String activeSynchronizationRootDefinitions;

    public FileSystemChangeSummary() {
        // Needed for JSON deserialization
    }

    public FileSystemChangeSummary(
            List<FileSystemItemChange> fileSystemChanges,
            Map<String, Set<IdRef>> activeRootRefs, Long syncDate,
            Boolean tooManyChanges) {
        this.fileSystemChanges = fileSystemChanges;
        this.syncDate = syncDate;
        this.hasTooManyChanges = tooManyChanges;
        List<String> rootDefinitions = new ArrayList<String>();
        for (Map.Entry<String, Set<IdRef>> entry : activeRootRefs.entrySet()) {
            for (IdRef ref: entry.getValue()) {
                rootDefinitions.add(String.format("%s:%s", entry.getKey(), ref.toString()));
            }
        }
        this.activeSynchronizationRootDefinitions = StringUtils.join(rootDefinitions, ",");
    }

    public List<FileSystemItemChange> getFileSystemChanges() {
        return fileSystemChanges;
    }

    public void setFileSystemChanges(List<FileSystemItemChange> changes) {
        this.fileSystemChanges = changes;
    }

    /**
     * @return the time code of current sync operation in milliseconds since
     *         1970-01-01 UTC rounded to the second as measured on the server
     *         clock. Changes from the current summary instance all happen
     *         "strictly" before this time code. This value is expected to be
     *         passed to the next call to
     *         {@code NuxeoDriveManager#getDocumentChangeSummary} to get
     *         strictly monotonic change summaries (without overlap).
     */
    public Long getSyncDate() {
        return syncDate;
    }

    public String getActiveSynchronizationRootDefinitions() {
        return activeSynchronizationRootDefinitions;
    }

    public void setActiveSynchronizationRootDefinitions(
            String activeSynchronizationRootDefinitions) {
        this.activeSynchronizationRootDefinitions = activeSynchronizationRootDefinitions;
    }

    public void setSyncDate(Long syncDate) {
        this.syncDate = syncDate;
    }

    public void setHasTooManyChanges(Boolean hasTooManyChanges) {
        this.hasTooManyChanges = hasTooManyChanges;
    }

    public Boolean getHasTooManyChanges() {
        return this.hasTooManyChanges;
    }

    @Override
    public String toString() {
        if (hasTooManyChanges) {
            return String.format("%s(syncDate=%d, hasTooManyChanges=true)",
                    getClass().getSimpleName(), getSyncDate());
        } else {
            return String.format("%s(syncDate=%d, items=[%s])",
                    getClass().getSimpleName(), getSyncDate(),
                    StringUtils.join(fileSystemChanges, ", "));
        }
    }
}
