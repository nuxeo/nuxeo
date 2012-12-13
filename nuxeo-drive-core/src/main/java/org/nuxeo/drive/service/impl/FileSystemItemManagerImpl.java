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

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Default implementation of the {@link FileSystemItemManager}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemManagerImpl implements FileSystemItemManager {

    /*------------- Opened sessions against each repository ----------------*/
    protected final ThreadLocal<Map<String, CoreSession>> openedSessions = new ThreadLocal<Map<String, CoreSession>>() {
        @Override
        protected Map<String, CoreSession> initialValue() {
            return new HashMap<String, CoreSession>();
        }
    };

    public CoreSession getSession(String repositoryName, Principal principal)
            throws ClientException {
        final String sessionKey = repositoryName + "/" + principal.getName();
        CoreSession session = openedSessions.get().get(sessionKey);
        if (session == null) {
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put("principal", (Serializable) principal);
            final CoreSession newSession = CoreInstance.getInstance().open(
                    repositoryName, context);
            openedSessions.get().put(sessionKey, newSession);
            try {
                Transaction t = TransactionHelper.lookupTransactionManager().getTransaction();
                t.registerSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                        CoreInstance.getInstance().close(newSession);
                        openedSessions.get().remove(sessionKey);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        // Nothing to do
                    }
                });
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
            }
            session = newSession;
        }
        return session;
    }

    /*------------- Read operations ----------------*/
    @Override
    public boolean exists(String id, Principal principal)
            throws ClientException {
        return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                id).exists(id, principal);
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException {
        return getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                id).getFileSystemItemById(id, principal);
    }

    @Override
    public List<FileSystemItem> getChildren(String id, Principal principal)
            throws ClientException {
        FileSystemItem fileSystemItem = getFileSystemItemById(id, principal);
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
            Principal principal) throws ClientException {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, principal);
        if (!(parentFsItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot create a folder in a non folderish file system item.");
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFolder(name);
    }

    @Override
    public FileItem createFile(String parentId, Blob blob, Principal principal)
            throws ClientException {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, principal);
        if (!(parentFsItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot create a file in a non folderish file system item.");
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFile(blob);
    }

    @Override
    public FileItem updateFile(String id, Blob blob, Principal principal)
            throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(id, principal);
        if (!(fsItem instanceof FileItem)) {
            throw new ClientException(
                    "Cannot update the content of a file system item that is not a file.");
        }
        FileItem file = (FileItem) fsItem;
        file.setBlob(blob);
        return file;
    }

    @Override
    public void delete(String id, Principal principal) throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(id, principal);
        fsItem.delete();
    }

    @Override
    public FileSystemItem rename(String id, String name, Principal principal)
            throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(id, principal);
        fsItem.rename(name);
        return fsItem;
    }

    @Override
    public FileSystemItem move(String srcId, String destId, Principal principal)
            throws ClientException {
        // TODO
        return null;
    }

    @Override
    public FileSystemItem copy(String srcId, String destId, Principal principal)
            throws ClientException {
        // TODO
        return null;
    }

    /*------------- Protected ---------------*/
    protected FileSystemItemAdapterService getFileSystemItemAdapterService() {
        return Framework.getLocalService(FileSystemItemAdapterService.class);
    }

}
