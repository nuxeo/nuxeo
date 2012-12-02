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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemSynchronizationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of the {@link FileSystemSynchronizationService}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemSynchronizationServiceImpl implements
        FileSystemSynchronizationService {

    /*------------- Read operations ----------------*/
    @Override
    public FileSystemItem getFileSystemItemById(String docId,
            CoreSession session) throws ClientException {
        DocumentModel doc = getDocumentModel(docId, session);
        return doc.getAdapter(FileSystemItem.class);
    }

    @Override
    public List<FileSystemItem> getChildren(String parentId, CoreSession session)
            throws ClientException {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, session);
        if (!(parentFsItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot get the children of a non folderish document.");
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.getChildren();
    }

    /*------------- Write operations ----------------*/
    @Override
    public FolderItem createFolder(String parentId, String name,
            CoreSession session) throws ClientException {
        FileSystemItem parentFsItem = getFileSystemItemById(parentId, session);
        if (!(parentFsItem instanceof FolderItem)) {
            throw new ClientException(
                    "Cannot create a folder in a non folderish document.");
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
                    "Cannot create a file in a non folderish document.");
        }
        FolderItem parentFolder = (FolderItem) parentFsItem;
        return parentFolder.createFile(blob);
    }

    @Override
    public FileItem updateFile(String docId, Blob blob, CoreSession session)
            throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(docId, session);
        if (!(fsItem instanceof FileItem)) {
            throw new ClientException(
                    "Cannot update the content of a non file document.");
        }
        FileItem file = (FileItem) fsItem;
        file.setBlob(blob);
        return file;
    }

    @Override
    public void delete(String docId, CoreSession session)
            throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        docs.add(getDocumentModel(docId, session));
        getTrashService().trashDocuments(docs);
    }

    @Override
    public FileSystemItem rename(String docId, String name, CoreSession session)
            throws ClientException {
        FileSystemItem fsItem = getFileSystemItemById(docId, session);
        fsItem.rename(name);
        return fsItem;
    }

    @Override
    public FileSystemItem move(String docId, String destDocId,
            CoreSession session) throws ClientException {
        // TODO
        return null;
    }

    @Override
    public FileSystemItem copy(String docId, String destDocId,
            CoreSession session) throws ClientException {
        // TODO
        return null;
    }

    /*---------------------- Protected ------------------------------*/
    protected DocumentModel getDocumentModel(String docId, CoreSession session)
            throws ClientException {
        return session.getDocument(new IdRef(docId));
    }

    protected TrashService getTrashService() {
        return Framework.getLocalService(TrashService.class);
    }

}
