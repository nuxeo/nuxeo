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
package org.nuxeo.drive.adapter.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Default implementation of the top level {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultTopLevelFolderItem extends AbstractFileSystemItem implements
        FolderItem {

    public DefaultTopLevelFolderItem(String factoryName) {
        super(factoryName);
    }

    /*--------------------- AbstractFileSystemItem ---------------------*/
    @Override
    public String getParentId() {
        return null;
    }

    @Override
    public String getName() {
        return "Nuxeo Drive";
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public String getCreator() {
        return "system";
    }

    @Override
    public Calendar getCreationDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2012, 11, 12, 12, 12, 12);
        return cal;
    }

    @Override
    public Calendar getLastModificationDate() {
        return getCreationDate();
    }

    @Override
    public boolean getCanRename() {
        return false;
    }

    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot rename a system folder item.");
    }

    @Override
    public boolean getCanDelete() {
        return false;
    }

    @Override
    public void delete() throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot delete a system folder item.");
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        // TODO
        // Set<IdRef> syncRootrefs = Framework.getLocalService(
        // NuxeoDriveManager.class).getSynchronizationRootReferences(
        // session.getPrincipal().getName(), session);
        // Iterator<IdRef> syncRootrefsIt = syncRootrefs.iterator();
        // while (syncRootrefsIt.hasNext()) {
        // IdRef idRef = syncRootrefsIt.next();
        //
        // DocumentModel doc = session.getDocument(idRef);
        // rootChildren.add(adapterService.getFileSystemItem(doc, getId()));
        //
        // }
        return children;
    }

    @Override
    public boolean getCanCreateChild() {
        return false;
    }

    @Override
    public FolderItem createFolder(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a folder in a system folder item.");
    }

    @Override
    public FileItem createFile(Blob blob) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a file in a system folder item.");
    }

}
