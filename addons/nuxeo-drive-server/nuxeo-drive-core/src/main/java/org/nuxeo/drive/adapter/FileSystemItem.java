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
package org.nuxeo.drive.adapter;

import java.util.Calendar;

import org.nuxeo.drive.adapter.impl.AbstractDocumentBackedFileSystemItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
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
public interface FileSystemItem extends Comparable<FileSystemItem> {

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

    void rename(String name);

    boolean getCanDelete();

    void delete();

    Lock getLockInfo();

    boolean canMove(FolderItem dest);

    FileSystemItem move(FolderItem dest);

}
