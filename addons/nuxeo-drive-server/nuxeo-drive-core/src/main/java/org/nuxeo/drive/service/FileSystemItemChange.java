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

import org.nuxeo.drive.adapter.FileSystemItem;

/**
 * Representation of a file system item change.
 *
 * @author Antoine Taillefer
 */
public interface FileSystemItemChange {

    String getFileSystemItemId();

    void setFileSystemItemId(String fileSystemItemId);

    String getFileSystemItemName();

    void setFileSystemItemName(String fileSystemItemName);

    FileSystemItem getFileSystemItem();

    void setFileSystemItem(FileSystemItem fileSystemItem);

    String getRepositoryId();

    void setRepositoryId(String repositoryId);

    String getEventId();

    void setEventId(String eventId);

    Long getEventDate();

    void setEventDate(Long eventDate);

    String getDocUuid();

    void setDocUuid(String docUuid);

}
