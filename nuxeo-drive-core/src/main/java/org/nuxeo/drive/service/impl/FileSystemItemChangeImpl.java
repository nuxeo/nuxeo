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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemChange;

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

        // To be deprecated once the client uses the new filesystem API
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
        this.fileSystemItem = fsItem;
    }

    public String getFileSystemItemId() {
        return fileSystemItemId;
    }

    public void setFileSystemItemId(String fileSystemItemId) {
        this.fileSystemItemId = fileSystemItemId;
    }

    public String getFileSystemItemName() {
        return fileSystemItemName;
    }

    public void setFileSystemItemName(String fileSystemItemName) {
        this.fileSystemItemName = fileSystemItemName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getEventDate() {
        return eventDate;
    }

    public void setEventDate(Long eventDate) {
        this.eventDate = eventDate;
    }

    public String getDocUuid() {
        return docUuid;
    }

    public void setDocUuid(String docUuid) {
        this.docUuid = docUuid;
    }

    public FileSystemItem getFileSystemItem() {
        return fileSystemItem;
    }

    @JsonIgnore
    public void setFileSystemItem(FileSystemItem fileSystemItem) {
        this.fileSystemItem = fileSystemItem;
    }

    @Override
    public String toString() {
        if (fileSystemItem != null) {
            return String.format("%s(eventId=\"%s\", eventDate=%d, item=%s)", getClass().getSimpleName(), eventId,
                    eventDate, fileSystemItem);
        } else {
            return String.format("%s(eventId=\"%s\", eventDate=%d)", getClass().getSimpleName(), eventId, eventDate);
        }
    }
}
