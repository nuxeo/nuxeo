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

import java.security.Principal;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of the {@link FileSystemItemManager}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemManagerImpl implements FileSystemItemManager {

    private static final Logger log = LogManager.getLogger(FileSystemItemManagerImpl.class);

    /*------------- Read operations ----------------*/
    @Override
    public List<FileSystemItem> getTopLevelChildren(Principal principal) {
        return getTopLevelFolder(principal).getChildren();
    }

    @Override
    public FolderItem getTopLevelFolder(Principal principal) {
        return getFileSystemItemAdapterService().getTopLevelFolderItemFactory().getTopLevelFolderItem(principal);
    }

    @Override
    public boolean exists(String id, Principal principal) {
        return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(id).exists(id, principal);
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal) {
        try {
            return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(id).getFileSystemItemById(id,
                    principal);
        } catch (RootlessItemException e) {
            log.debug("RootlessItemException thrown while trying to get file system item with id {}, returning null.",
                    id);
            return null;
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, String parentId, Principal principal) {
        try {
            return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(id).getFileSystemItemById(id,
                    parentId, principal);
        } catch (RootlessItemException e) {
            log.debug(
                    "RootlessItemException thrown while trying to get file system item with id {} and parent id {}, returning null.",
                    id, parentId);
            return null;
        }
    }

    @Override
    public List<FileSystemItem> getChildren(String id, Principal principal) {
        FileSystemItem fileSystemItem = getFileSystemItemById(id, principal);
        if (fileSystemItem == null) {
            throw new NuxeoException(String.format(
                    "Cannot get the children of file system item with id %s because it doesn't exist.", id));
        }
        if (!(fileSystemItem instanceof FolderItem)) {
            throw new NuxeoException(String.format(
                    "Cannot get the children of file system item with id %s because it is not a folder.", id));
        }
        FolderItem folderItem = (FolderItem) fileSystemItem;
        return folderItem.getChildren();
    }

    @Override
    public ScrollFileSystemItemList scrollDescendants(String id, Principal principal, String scrollId, int batchSize,
            long keepAlive) {
        FileSystemItem fileSystemItem = getFileSystemItemById(id, principal);
        if (fileSystemItem == null) {
            throw new NuxeoException(String.format(
                    "Cannot get the descendants of file system item with id %s because it doesn't exist.", id));
        }
        if (!(fileSystemItem instanceof FolderItem)) {
            throw new NuxeoException(String.format(
                    "Cannot get the descendants of file system item with id %s because it is not a folder.", id));
        }
        FolderItem folderItem = (FolderItem) fileSystemItem;
        return folderItem.scrollDescendants(scrollId, batchSize, keepAlive);
    }

    @Override
    public boolean canMove(String srcId, String destId, Principal principal) {
        FileSystemItem srcFsItem = getFileSystemItemById(srcId, principal);
        if (srcFsItem == null) {
            return false;
        }
        FileSystemItem destFsItem = getFileSystemItemById(destId, principal);
        if (!(destFsItem instanceof FolderItem)) {
            return false;
        }
        return srcFsItem.canMove((FolderItem) destFsItem);
    }

    /*------------- Write operations ---------------*/
    @Override
    public FolderItem createFolder(String parentId, String name, Principal principal, boolean overwrite) {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, principal);
        if (parentFsItem == null) {
            throw new NuxeoException(String.format(
                    "Cannot create a folder in file system item with id %s because it doesn't exist.", parentId));
        }
        if (!(parentFsItem instanceof FolderItem)) {
            throw new NuxeoException(String.format(
                    "Cannot create a folder in file system item with id %s because it is not a folder but is: %s",
                    parentId, parentFsItem));
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFolder(name, overwrite);
    }

    @Override
    public FileItem createFile(String parentId, Blob blob, Principal principal, boolean overwrite) {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, principal);
        if (parentFsItem == null) {
            throw new NuxeoException(String.format(
                    "Cannot create a file in file system item with id %s because it doesn't exist.", parentId));
        }
        if (!(parentFsItem instanceof FolderItem)) {
            throw new NuxeoException(String.format(
                    "Cannot create a file in file system item with id %s because it is not a folder but is: %s",
                    parentId, parentFsItem));
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFile(blob, overwrite);
    }

    @Override
    public FileItem updateFile(String id, Blob blob, Principal principal) {
        FileSystemItem fsItem = getFileSystemItemById(id, principal);
        return updateFile(fsItem, blob);
    }

    @Override
    public FileItem updateFile(String id, String parentId, Blob blob, Principal principal) {
        FileSystemItem fsItem = getFileSystemItemById(id, parentId, principal);
        return updateFile(fsItem, blob);
    }

    @Override
    public void delete(String id, Principal principal) {
        FileSystemItem fsItem = getFileSystemItemById(id, principal);
        delete(fsItem);
    }

    @Override
    public void delete(String id, String parentId, Principal principal) {
        FileSystemItem fsItem = getFileSystemItemById(id, parentId, principal);
        delete(fsItem);
    }

    @Override
    public FileSystemItem rename(String id, String name, Principal principal) {
        FileSystemItem fsItem = getFileSystemItemById(id, principal);
        if (fsItem == null) {
            throw new NuxeoException(
                    String.format("Cannot rename file system item with id %s because it doesn't exist.", id));
        }
        fsItem.rename(name);
        return fsItem;
    }

    @Override
    public FileSystemItem move(String srcId, String destId, Principal principal) {
        FileSystemItem srcFsItem = getFileSystemItemById(srcId, principal);
        if (srcFsItem == null) {
            throw new NuxeoException(
                    String.format("Cannot move file system item with id %s because it doesn't exist.", srcId));
        }
        FileSystemItem destFsItem = getFileSystemItemById(destId, principal);
        if (destFsItem == null) {
            throw new NuxeoException(String.format(
                    "Cannot move a file system item to file system item with id %s because it doesn't exist.", destId));
        }
        if (!(destFsItem instanceof FolderItem)) {
            throw new NuxeoException(String.format(
                    "Cannot move a file system item to file system item with id %s because it is not a folder.",
                    destId));
        }
        return srcFsItem.move((FolderItem) destFsItem);
    }

    /*------------- Protected ---------------*/
    protected FileSystemItemAdapterService getFileSystemItemAdapterService() {
        return Framework.getService(FileSystemItemAdapterService.class);
    }

    protected FileItem updateFile(FileSystemItem fsItem, Blob blob) {
        if (fsItem == null) {
            throw new NuxeoException("Cannot update the content of file system item because it doesn't exist.");
        }
        if (!(fsItem instanceof FileItem)) {
            throw new NuxeoException(
                    String.format("Cannot update the content of file system item with id %s because it is not a file.",
                            fsItem.getId()));
        }
        FileItem file = (FileItem) fsItem;
        file.setBlob(blob);
        return file;
    }

    protected void delete(FileSystemItem fsItem) {
        if (fsItem == null) {
            throw new NuxeoException("Cannot delete file system item because it doesn't exist.");
        }
        fsItem.delete();
    }

}
