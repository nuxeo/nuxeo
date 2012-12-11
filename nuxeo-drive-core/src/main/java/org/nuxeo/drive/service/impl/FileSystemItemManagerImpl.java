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

import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of the {@link FileSystemItemManager}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemManagerImpl implements FileSystemItemManager {

    /*------------- Read operations ----------------*/
    @Override
    public boolean exists(String id, CoreSession session)
            throws ClientException {
        return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                id).exists(id, session);
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, CoreSession session)
            throws ClientException {
        return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                id).getFileSystemItemById(id, session);
    }

    @Override
    public List<FileSystemItem> getChildren(String id, CoreSession session)
            throws ClientException {
        FileSystemItem fileSystemItem = getFileSystemItemById(id, session);
        if (!(fileSystemItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot get the children of a non folderish file system item.");
        }
        FolderItem folderItem = (FolderItem) fileSystemItem;
        return folderItem.getChildren();
    }

    /*------------- Write operations ---------------*/
    @Override
    public FolderItem createFolder(String parentId, String name,
            CoreSession session) throws ClientException {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, session);
        if (!(parentFsItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot create a folder in a non folderish file system item.");
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFolder(name);
    }

    @Override
    public FileItem createFile(String parentId, Blob blob, CoreSession session)
            throws ClientException {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, session);
        if (!(parentFsItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot create a file in a non folderish file system item.");
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFile(blob);
    }

    @Override
    public FileItem updateFile(String id, Blob blob, CoreSession session)
            throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(id, session);
        if (!(fsItem instanceof FileItem)) {
            throw new ClientException(
                    "Cannot update the content of a file system item that is not a file.");
        }
        FileItem file = (FileItem) fsItem;
        file.setBlob(blob);
        return file;
    }

    @Override
    public void delete(String id, CoreSession session) throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(id, session);
        fsItem.delete();
    }

    @Override
    public FileSystemItem rename(String id, String name, CoreSession session)
            throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(id, session);
        fsItem.rename(name);
        return fsItem;
    }

    @Override
    public FileSystemItem move(String srcId, String destId, CoreSession session)
            throws ClientException {
        // TODO
        return null;
    }

    @Override
    public FileSystemItem copy(String srcId, String destId, CoreSession session)
            throws ClientException {
        // TODO
        return null;
    }

    /*------------- Protected ---------------*/
    protected FileSystemItemAdapterService getFileSystemItemAdapterService() {
        return Framework.getLocalService(FileSystemItemAdapterService.class);
    }

}
