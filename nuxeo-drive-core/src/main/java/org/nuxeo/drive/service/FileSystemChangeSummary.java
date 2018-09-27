/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
     * @return the time code of current sync operation in milliseconds since 1970-01-01 UTC rounded to the second as
     *         measured on the server clock.
     */
    Long getSyncDate();

    /**
     * @return the upper bound of the range clause in the change query. Changes from the current summary instance all
     *         happen "strictly before" this bound. This value is expected to be passed to the next call to
     *         {@link NuxeoDriveManager#getChangeSummary(java.security.Principal, java.util.Map, long)} to get strictly
     *         monotonic change summaries (without overlap).
     */
    Long getUpperBound();

    String getActiveSynchronizationRootDefinitions();

    void setActiveSynchronizationRootDefinitions(String activeSynchronizationRootDefinitions);

    void setSyncDate(Long syncDate);

    void setUpperBound(Long upperBound);

    void setHasTooManyChanges(Boolean hasTooManyChanges);

    Boolean getHasTooManyChanges();

}
