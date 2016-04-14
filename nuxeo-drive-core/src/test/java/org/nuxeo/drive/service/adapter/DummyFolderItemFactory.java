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
package org.nuxeo.drive.service.adapter;

import java.security.Principal;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Dummy folder implementation of a {@link FileSystemItemFactory} for test purpose.
 *
 * @author Antoine Taillefer
 */
public class DummyFolderItemFactory extends DefaultFileSystemItemFactory {

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc) throws ClientException {
        return new DummyFolderItem(name, doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) throws ClientException {
        return getFileSystemItem(doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {
        return getFileSystemItem(doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) throws ClientException {
        return getFileSystemItem(doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem) throws ClientException {
        return new DummyFolderItem(name, parentItem, doc);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted)
            throws ClientException {
        return getFileSystemItem(doc, parentItem);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException {
        return getFileSystemItem(doc, parentItem);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo) throws ClientException {
        return getFileSystemItem(doc, parentItem);
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        return id.startsWith(name + AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR);
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal) throws ClientException {
        String[] parts = StringUtils.split(id, AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR);
        CoreSession session = Framework.getLocalService(FileSystemItemManager.class).getSession(parts[1], principal);
        return new DummyFolderItem(name, getDocumentById(parts[2], session));
    }

}
