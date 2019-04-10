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
package org.nuxeo.drive.adapter;

import java.io.Serializable;
import java.util.Calendar;

import org.nuxeo.drive.adapter.impl.AbstractDocumentBackedFileSystemItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.Lock;

/**
 * Representation of a file system item, typically a file or a folder.
 * 
 * @author Antoine Taillefer
 * @see AbstractFileSystemItem
 * @see AbstractDocumentBackedFileSystemItem
 * @see FileItem
 * @see FolderItem
 */
public interface FileSystemItem extends Comparable<FileSystemItem>, Serializable {

    /**
     * Gets a unique id generated server-side.
     */
    String getId();

    /**
     * Gets the parent {@link FileSystemItem} id.
     */
    String getParentId();

    /**
     * A concatenation of ancestor ids with '/' as prefix and separator.
     */
    String getPath();

    /**
     * Gets the name displayed in the file system.
     */
    String getName();

    boolean isFolder();

    String getCreator();

    String getLastContributor();

    Calendar getCreationDate();

    Calendar getLastModificationDate();

    boolean getCanRename();

    void rename(String name) throws ClientException;

    boolean getCanDelete();

    void delete() throws ClientException;

    Lock getLockInfo();

    boolean canMove(FolderItem dest) throws ClientException;

    FileSystemItem move(FolderItem dest) throws ClientException;

}
