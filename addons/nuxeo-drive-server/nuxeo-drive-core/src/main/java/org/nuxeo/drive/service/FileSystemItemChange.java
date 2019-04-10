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

import org.nuxeo.drive.adapter.FileSystemItem;

/**
 * Representation of a file system item change.
 * 
 * @author Antoine Taillefer
 */
public interface FileSystemItemChange extends Serializable {

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
