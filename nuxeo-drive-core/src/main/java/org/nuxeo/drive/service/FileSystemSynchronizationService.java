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

import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.FileSystemSynchronizationServiceImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Provides an API for synchronizing documents with a file system. Allows the
 * following actions:
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
 * @see FileSystemSynchronizationServiceImpl
 */
public interface FileSystemSynchronizationService {

    /*------------- Read operations ----------------*/
    boolean exists(String docId, CoreSession session)
            throws ClientException;

    FileSystemItem getFileSystemItemById(String docId, CoreSession session)
            throws ClientException;

    List<FileSystemItem> getChildren(String parentId, CoreSession session)
            throws ClientException;

    /*------------- Write operations ----------------*/
    FolderItem createFolder(String parentId, String name, CoreSession session)
            throws ClientException;

    FileItem createFile(String parentId, Blob blob, CoreSession session)
            throws ClientException;

    FileItem updateFile(String docId, Blob blob, CoreSession session)
            throws ClientException;

    void delete(String docId, CoreSession session) throws ClientException;

    FileSystemItem rename(String docId, String name, CoreSession session)
            throws ClientException;

    FileSystemItem move(String docId, String destDocId, CoreSession session)
            throws ClientException;

    FileSystemItem copy(String docId, String destDocId, CoreSession session)
            throws ClientException;

}
