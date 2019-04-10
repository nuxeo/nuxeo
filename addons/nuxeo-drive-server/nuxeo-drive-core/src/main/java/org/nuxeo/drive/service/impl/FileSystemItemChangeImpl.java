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
package org.nuxeo.drive.service.impl;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemChange;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Default implementation of a {@link FileSystemItemChange}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemChangeImpl implements FileSystemItemChange {

    private static final long serialVersionUID = -5697869523880291618L;

    protected String repositoryId;

    protected String eventId;

    protected Long eventDate;

    protected String docUuid;

    protected FileSystemItem fileSystemItem;

    protected String fileSystemItemId;

    protected String fileSystemItemName;

    public FileSystemItemChangeImpl() {
        // Needed for JSON deserialization
    }

    public FileSystemItemChangeImpl(String eventId, long eventDate, String repositoryId, String docUuid,
            String fileSystemItemId, String fileSystemItemName) {
        this.eventId = eventId;
        this.eventDate = eventDate;

        this.repositoryId = repositoryId;
        this.docUuid = docUuid;

        // We store the fileSystemItemId for events that no longer have access
        // to the full filesystem item description as is the case when a
        // document is deleted
        this.fileSystemItemId = fileSystemItemId;

        // Just there to make debugging easier and tests more readable: the
        // client should only need the fileSystemItemId.
        this.fileSystemItemName = fileSystemItemName;
    }

    public FileSystemItemChangeImpl(String eventId, long eventDate, String repositoryId, String docUuid,
            FileSystemItem fsItem) {
        this(eventId, eventDate, repositoryId, docUuid, fsItem.getId(), fsItem.getName());
        fileSystemItem = fsItem;
    }

    @Override
    public String getFileSystemItemId() {
        return fileSystemItemId;
    }

    @Override
    public void setFileSystemItemId(String fileSystemItemId) {
        this.fileSystemItemId = fileSystemItemId;
    }

    @Override
    public String getFileSystemItemName() {
        return fileSystemItemName;
    }

    @Override
    public void setFileSystemItemName(String fileSystemItemName) {
        this.fileSystemItemName = fileSystemItemName;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public Long getEventDate() {
        return eventDate;
    }

    @Override
    public void setEventDate(Long eventDate) {
        this.eventDate = eventDate;
    }

    @Override
    public String getDocUuid() {
        return docUuid;
    }

    @Override
    public void setDocUuid(String docUuid) {
        this.docUuid = docUuid;
    }

    @Override
    public FileSystemItem getFileSystemItem() {
        return fileSystemItem;
    }

    @Override
    @JsonDeserialize(using = FileSystemItemDeserializer.class)
    public void setFileSystemItem(FileSystemItem fileSystemItem) {
        this.fileSystemItem = fileSystemItem;
    }

    @Override
    public String toString() {
        return String.format(
                "%s(eventId=\"%s\", eventDate=%d, repositoryId=%s, docUuid=%s, fileSystemItemId=%s, fileSystemItemName=%s, fileSystemItem=%s)",
                getClass().getSimpleName(), eventId, eventDate, repositoryId, docUuid, fileSystemItemId,
                fileSystemItemName, fileSystemItem);

    }

}
