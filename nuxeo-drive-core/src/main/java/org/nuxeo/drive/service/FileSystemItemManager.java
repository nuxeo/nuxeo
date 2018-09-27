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
package org.nuxeo.drive.service;

import java.security.Principal;
import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.impl.FileSystemItemManagerImpl;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Provides an API to manage usual file system operations on a {@link FileSystemItem} given its id. Allows the following
 * actions:
 * <ul>
 * <li>Read</li>
 * <li>Read children</li>
 * <li>Read descendants</li>
 * <li>Create</li>
 * <li>Update</li>
 * <li>Delete</li>
 * <li>Rename</li>
 * <li>Move</li>
 * </ul>
 *
 * @author Antoine Taillefer
 * @see FileSystemItemManagerImpl
 */
public interface FileSystemItemManager {

    /*------------- Read operations ----------------*/
    /**
     * Gets the children of the top level {@link FolderItem} for the given principal.
     *
     * @deprecated use getTopLevelFolder#getChildren instead
     */
    @Deprecated
    List<FileSystemItem> getTopLevelChildren(Principal principal);

    /**
     * Gets the top level {@link FolderItem} for the given principal.
     */
    FolderItem getTopLevelFolder(Principal principal);

    /**
     * Returns true if a {@link FileSystemItem} with the given id exists for the given principal.
     *
     * @see FileSystemItemFactory#exists(String, Principal)
     */
    boolean exists(String id, Principal principal);

    /**
     * Gets the {@link FileSystemItem} with the given id for the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id
     * @see FileSystemItemFactory#getFileSystemItemById(String, Principal)
     */
    FileSystemItem getFileSystemItemById(String id, Principal principal);

    /**
     * Gets the {@link FileSystemItem} with the given id and parent id for the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id and parent id
     * @see #getFileSystemItemById(String, Principal)
     * @since 6.0
     */
    FileSystemItem getFileSystemItemById(String id, String parentId, Principal principal);

    /**
     * Gets the children of the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FolderItem#getChildren()
     */
    List<FileSystemItem> getChildren(String id, Principal principal);

    /**
     * Retrieves at most {@code batchSize} descendants of the {@link FolderItem} with the given {@code id} for the given
     * {@code principal} and the given {@code scrollId}.
     * <p>
     * When passing a null {@code scrollId} the initial search request is executed and the first batch of results is
     * returned along with a {@code scrollId} which should be passed to the next call in order to retrieve the next
     * batch of results.
     * <p>
     * Ideally, the search context made available by the initial search request is kept alive during {@code keepAlive}
     * milliseconds if {@code keepAlive} is positive.
     * <p>
     * Results are not necessarily sorted.
     *
     * @see FolderItem#scrollDescendants(String, int, long)
     * @since 8.3
     */
    ScrollFileSystemItemList scrollDescendants(String id, Principal principal, String scrollId, int batchSize,
            long keepAlive);

    /**
     * Return true if the {@link FileSystemItem} with the given source id can be moved to the {@link FileSystemItem}
     * with the given destination id for the given principal.
     *
     * @see FileSystemItem#getCanMove(String)
     */
    boolean canMove(String srcId, String destId, Principal principal);

    /*------------- Write operations ----------------*/
    /**
     * Creates a folder with the given name in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FolderItem#createFolder(String)
     * @deprecated since 9.1, use {@link #createFolder(String, String, Principal, boolean)} instead
     */
    @Deprecated
    default FolderItem createFolder(String parentId, String name, Principal principal) {
        return createFolder(parentId, name, principal, false);
    }

    /**
     * Creates a folder with the given name in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @param overwrite allows to overwrite an existing folder with the same title
     * @see FolderItem#createFolder(String, boolean)
     * @since 9.1
     */
    FolderItem createFolder(String parentId, String name, Principal principal, boolean overwrite);

    /**
     * Creates a file with the given blob in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FolderItem#createFile(Blob)
     * @deprecated since 9.1, use {@link #createFile(String, Blob, Principal, boolean)} instead
     */
    @Deprecated
    default FileItem createFile(String parentId, Blob blob, Principal principal) {
        return createFile(parentId, blob, principal, false);
    }

    /**
     * Creates a file with the given blob in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @param overwrite allows to overwrite an existing folder with the same title
     * @see FolderItem#createFile(Blob, boolean)
     * @since 9.1
     */
    FileItem createFile(String parentId, Blob blob, Principal principal, boolean overwrite);

    /**
     * Updates the {@link FileSystemItem} with the given id with the given blob for the given principal.
     *
     * @see FileItem#setBlob(Blob)
     */
    FileItem updateFile(String id, Blob blob, Principal principal);

    /**
     * Updates the {@link FileSystemItem} with the given id and parent id with the given blob for the given principal.
     *
     * @see #updateFile(String, Blob, Principal)
     * @since 6.0
     */
    FileItem updateFile(String id, String parentId, Blob blob, Principal principal);

    /**
     * Deletes the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FileSystemItem#delete()
     */
    void delete(String id, Principal principal);

    /**
     * Deletes the {@link FileSystemItem} with the given id and parent id for the given principal.
     *
     * @see #delete(String, Principal)
     * @since 6.0
     */
    void delete(String id, String parentId, Principal principal);

    /**
     * Renames the {@link FileSystemItem} with the given id with the given name for the given principal.
     *
     * @see FileSystemItem#rename(String)
     */
    FileSystemItem rename(String id, String name, Principal principal);

    /**
     * Moves the {@link FileSystemItem} with the given source id to the {@link FileSystemItem} with the given
     * destination id for the given principal.
     *
     * @see FileSystemItem#move(String)
     */
    FileSystemItem move(String srcId, String destId, Principal principal);

}
