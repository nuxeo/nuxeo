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
import java.util.List;
import java.util.Set;

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

    private static final long serialVersionUID = -5719579884697229867L;

    public static final String STATUS_FOUND_CHANGES = "found_changes";

    public static final String STATUS_TOO_MANY_CHANGES = "too_many_changes";

    public static final String STATUS_NO_CHANGES = "no_changes";

    protected Set<String> syncRootPaths;

    protected List<FileSystemItemChange> fileSystemChanges;

    protected String statusCode;

    protected Long syncDate;

    public FileSystemChangeSummary() {
        // Needed for JSON deserialization
    }

    public FileSystemChangeSummary(Set<String> syncRootPaths,
            List<FileSystemItemChange> fileSystemItemChanges,
            String statusCode, Long syncDate) {
        this.fileSystemChanges = fileSystemItemChanges;
        this.syncRootPaths = syncRootPaths;
        this.statusCode = statusCode;
        this.syncDate = syncDate;
    }

    public Set<String> getSyncRootPaths() {
        return syncRootPaths;
    }

    public void setSyncRootPaths(Set<String> syncRootPaths) {
        this.syncRootPaths = syncRootPaths;
    }

    public List<FileSystemItemChange> getDocumentChanges() {
        return fileSystemChanges;
    }

    public void setDocumentChanges(List<FileSystemItemChange> documentChanges) {
        this.fileSystemChanges = documentChanges;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
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

    public void setSyncDate(Long syncDate) {
        this.syncDate = syncDate;
    }

}
