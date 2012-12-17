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
package org.nuxeo.drive.service;

import java.security.Principal;
import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.FileSystemItemManagerImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Provides an API to manage usual file system operations on a
 * {@link FileSystemItem} given its id. Allows the following actions:
 * <ul>
 * <li>Read</li>
 * <li>Read children</li>
 * <li>Create</li>
 * <li>Update</li>
 * <li>Delete</li>
 * <li>Rename</li>
 * <li>Move</li>
 * <li>Copy</li>
 * </ul>
 *
 * @author Antoine Taillefer
 * @see FileSystemItemManagerImpl
 */
public interface FileSystemItemManager {

    /**
     * Gets a session bound to the given repository for the given principal.
     */
    CoreSession getSession(String repositoryName, Principal principal)
            throws ClientException;

    /*------------- Read operations ----------------*/
    /**
     * Returns true if a {@link FileSystemItem} with the given id exists. Uses a
     * core session fetched with the given principal.
     *
     * @throws ClientException if no {@link FileSystemItemFactory} can handle
     *             the given {@link FileSystemItem} id or if an error occurs
     *             while checking the existence
     * @see FileSystemItemFactory#exists(String, Principal)
     */
    boolean exists(String id, Principal principal) throws ClientException;

    /**
     * Gets the {@link FileSystemItem} with the given id. Uses a core session
     * fetched with the given principal.
     *
     * @return the {@link FileSystemItem} or null if none matches the given id
     * @throws ClientException if no {@link FileSystemItemFactory} can handle
     *             the given {@link FileSystemItem} id or if an error occurs
     *             while retrieving the item
     * @see FileSystemItemFactory#getFileSystemItemById(String, Principal)
     */
    FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException;

    /**
     * Gets the children of the {@link FileSystemItem} with the given id. Uses a
     * core session fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved, or if it is not a {@link FolderItem} or
     *             if an error occurs while retrieving the children
     * @see FolderItem#getChildren()
     */
    List<FileSystemItem> getChildren(String id, Principal principal)
            throws ClientException;

    /**
     * Returns true if a child can be created in the {@link FileSystemItem} with
     * the given id. Uses a core session fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved
     */
    boolean canCreateChild(String id, Principal principal)
            throws ClientException;

    /*------------- Write operations ----------------*/
    /**
     * Creates a folder with the given name in the {@link FileSystemItem} with
     * the given id. Uses a core session fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved, or if it is not a {@link FolderItem} or
     *             if an error occurs while creating the folder
     * @see FolderItem#createFolder(String)
     */
    FolderItem createFolder(String parentId, String name, Principal principal)
            throws ClientException;

    /**
     * Creates a file with the given blob in the {@link FileSystemItem} with the
     * given id. Uses a core session fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved, or if it is not a {@link FolderItem} or
     *             if an error occurs while creating the file
     * @see FolderItem#createFile(Blob)
     */
    FileItem createFile(String parentId, Blob blob, Principal principal)
            throws ClientException;

    /**
     * Updates the {@link FileSystemItem} with the given id with the given blob.
     * Uses a core session fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved, or if it is not a {@link FileItem} or if
     *             an error occurs while updating the file
     * @see FileItem#setBlob(Blob)
     */
    FileItem updateFile(String id, Blob blob, Principal principal)
            throws ClientException;

    /**
     * Deletes the {@link FileSystemItem} with the given id. Uses a core session
     * fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved or if an error occurs while deleting the
     *             item
     * @see FileSystemItem#delete()
     */
    void delete(String id, Principal principal) throws ClientException;

    /**
     * Renames the {@link FileSystemItem} with the given id with the given name.
     * Uses a core session fetched with the given principal.
     *
     * @throws ClientException if the {@link FileSystemItem} with the given id
     *             cannot be retrieved or if an error occurs while renaming the
     *             item
     * @see FileSystemItem#rename(String)
     */
    FileSystemItem rename(String id, String name, Principal principal)
            throws ClientException;

    FileSystemItem move(String srcId, String destId, Principal principal)
            throws ClientException;

    FileSystemItem copy(String srcId, String destId, Principal principal)
            throws ClientException;

}
