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

import java.security.Principal;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Base implementation of a virtual {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public abstract class AbstractVirtualFolderItem extends AbstractFileSystemItem
        implements FolderItem {

    private static final long serialVersionUID = 1L;

    protected boolean canCreateChild;

    public AbstractVirtualFolderItem(String factoryName, Principal principal,
            String parentId, String parentPath, String folderName)
            throws ClientException {
        super(factoryName, principal);
        this.parentId = parentId;
        name = folderName;
        folder = true;
        creator = "system";
        // The Fixed Origin of (Unix) Time
        creationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        creationDate.set(1970, 0, 1, 0, 0, 0);
        lastModificationDate = this.creationDate;
        canRename = false;
        canDelete = false;
        canCreateChild = false;
        path = "/" + getId();
        if (parentPath != null) {
            path = parentPath + path;
        }
    }

    protected AbstractVirtualFolderItem() {
        // Needed for JSON deserialization
    }

    /*----------------------- FolderItem -----------------------*/
    public abstract List<FileSystemItem> getChildren() throws ClientException;

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot rename a virtual folder item.");
    }

    @Override
    public void delete() throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot delete a virtual folder item.");
    }

    @Override
    public boolean canMove(FolderItem dest) throws ClientException {
        return false;
    }

    @Override
    public FileSystemItem move(FolderItem dest) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot move a virtual folder item.");
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    public boolean getCanCreateChild() {
        return canCreateChild;
    }

    @Override
    public FolderItem createFolder(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a folder in a virtual folder item.");
    }

    @Override
    public FileItem createFile(Blob blob) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a file in a virtual folder item.");
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setCanCreateChild(boolean canCreateChild) {
        this.canCreateChild = canCreateChild;
    }

}
