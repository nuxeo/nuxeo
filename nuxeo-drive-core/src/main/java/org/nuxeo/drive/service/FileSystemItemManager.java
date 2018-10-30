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

import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.impl.FileSystemItemManagerImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

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
    List<FileSystemItem> getTopLevelChildren(NuxeoPrincipal principal);

    /**
     * Gets the top level {@link FolderItem} for the given principal.
     */
    FolderItem getTopLevelFolder(NuxeoPrincipal principal);

    /**
     * Returns true if a {@link FileSystemItem} with the given id exists for the given principal.
     *
     * @see FileSystemItemFactory#exists(String, NuxeoPrincipal)
     */
    boolean exists(String id, NuxeoPrincipal principal);

    /**
     * Gets the {@link FileSystemItem} with the given id for the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id
     * @see FileSystemItemFactory#getFileSystemItemById(String, NuxeoPrincipal)
     */
    FileSystemItem getFileSystemItemById(String id, NuxeoPrincipal principal);

    /**
     * Gets the {@link FileSystemItem} with the given id and parent id for the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id and parent id
     * @see #getFileSystemItemById(String, NuxeoPrincipal)
     * @since 6.0
     */
    FileSystemItem getFileSystemItemById(String id, String parentId, NuxeoPrincipal principal);

    /**
     * Gets the children of the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FolderItem#getChildren()
     */
    List<FileSystemItem> getChildren(String id, NuxeoPrincipal principal);

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
    ScrollFileSystemItemList scrollDescendants(String id, NuxeoPrincipal principal, String scrollId, int batchSize,
            long keepAlive);

    /**
     * Return true if the {@link FileSystemItem} with the given source id can be moved to the {@link FileSystemItem}
     * with the given destination id for the given principal.
     *
     * @see FileSystemItem#getCanMove(String)
     */
    boolean canMove(String srcId, String destId, NuxeoPrincipal principal);

    /*------------- Write operations ----------------*/
    /**
     * Creates a folder with the given name in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FolderItem#createFolder(String)
     * @deprecated since 9.1, use {@link #createFolder(String, String, NuxeoPrincipal, boolean)} instead
     */
    @Deprecated
    default FolderItem createFolder(String parentId, String name, NuxeoPrincipal principal) {
        return createFolder(parentId, name, principal, false);
    }

    /**
     * Creates a folder with the given name in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @param overwrite allows to overwrite an existing folder with the same title
     * @see FolderItem#createFolder(String, boolean)
     * @since 9.1
     */
    FolderItem createFolder(String parentId, String name, NuxeoPrincipal principal, boolean overwrite);

    /**
     * Creates a file with the given blob in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FolderItem#createFile(Blob)
     * @deprecated since 9.1, use {@link #createFile(String, Blob, NuxeoPrincipal, boolean)} instead
     */
    @Deprecated
    default FileItem createFile(String parentId, Blob blob, NuxeoPrincipal principal) {
        return createFile(parentId, blob, principal, false);
    }

    /**
     * Creates a file with the given blob in the {@link FileSystemItem} with the given id for the given principal.
     *
     * @param overwrite allows to overwrite an existing file with the same title
     * @see FolderItem#createFile(Blob, boolean)
     * @since 9.1
     */
    FileItem createFile(String parentId, Blob blob, NuxeoPrincipal principal, boolean overwrite);

    /**
     * Updates the {@link FileSystemItem} with the given id with the given blob for the given principal.
     *
     * @see FileItem#setBlob(Blob)
     */
    FileItem updateFile(String id, Blob blob, NuxeoPrincipal principal);

    /**
     * Updates the {@link FileSystemItem} with the given id and parent id with the given blob for the given principal.
     *
     * @see #updateFile(String, Blob, NuxeoPrincipal)
     * @since 6.0
     */
    FileItem updateFile(String id, String parentId, Blob blob, NuxeoPrincipal principal);

    /**
     * Deletes the {@link FileSystemItem} with the given id for the given principal.
     *
     * @see FileSystemItem#delete()
     */
    void delete(String id, NuxeoPrincipal principal);

    /**
     * Deletes the {@link FileSystemItem} with the given id and parent id for the given principal.
     *
     * @see #delete(String, NuxeoPrincipal)
     * @since 6.0
     */
    void delete(String id, String parentId, NuxeoPrincipal principal);

    /**
     * Renames the {@link FileSystemItem} with the given id with the given name for the given principal.
     *
     * @see FileSystemItem#rename(String)
     */
    FileSystemItem rename(String id, String name, NuxeoPrincipal principal);

    /**
     * Moves the {@link FileSystemItem} with the given source id to the {@link FileSystemItem} with the given
     * destination id for the given principal.
     *
     * @see FileSystemItem#move(String)
     */
    FileSystemItem move(String srcId, String destId, NuxeoPrincipal principal);

}
