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
package org.nuxeo.drive.adapter.impl;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Base implementation of a virtual {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public abstract class AbstractVirtualFolderItem extends AbstractFileSystemItem implements FolderItem {

    private static final long serialVersionUID = 1L;

    protected boolean canCreateChild;

    protected boolean canScrollDescendants;

    public AbstractVirtualFolderItem(String factoryName, NuxeoPrincipal principal, String parentId, String parentPath,
            String folderName) {
        super(factoryName, principal, false);
        this.parentId = parentId;
        name = folderName;
        folder = true;
        creator = SecurityConstants.SYSTEM_USERNAME;
        lastContributor = SecurityConstants.SYSTEM_USERNAME;
        // The Fixed Origin of (Unix) Time
        creationDate = Calendar.getInstance(NuxeoDriveManagerImpl.UTC);
        creationDate.set(1970, 0, 1, 0, 0, 0);
        lastModificationDate = this.creationDate;
        canRename = false;
        canDelete = false;
        canCreateChild = false;
        canScrollDescendants = false;
        path = "/" + getId();
        if (parentPath != null) {
            path = parentPath + path;
        }
    }

    protected AbstractVirtualFolderItem() {
        // Needed for JSON deserialization
    }

    /*----------------------- FolderItem -----------------------*/
    @Override
    public abstract List<FileSystemItem> getChildren();

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) {
        throw new UnsupportedOperationException("Cannot rename a virtual folder item.");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Cannot delete a virtual folder item.");
    }

    @Override
    public boolean canMove(FolderItem dest) {
        return false;
    }

    @Override
    public FileSystemItem move(FolderItem dest) {
        throw new UnsupportedOperationException("Cannot move a virtual folder item.");
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    public boolean getCanScrollDescendants() {
        return canScrollDescendants;
    }

    @Override
    public ScrollFileSystemItemList scrollDescendants(String scrollId, int batchSize, long keepAlive) {
        throw new UnsupportedOperationException(
                "Cannot scroll through the descendants of a virtual folder item, please call getChildren() instead.");
    }

    @Override
    public boolean getCanCreateChild() {
        return canCreateChild;
    }

    @Override
    public FolderItem createFolder(String name, boolean overwrite) {
        throw new UnsupportedOperationException("Cannot create a folder in a virtual folder item.");
    }

    @Override
    public FileItem createFile(Blob blob, boolean overwrite) {
        throw new UnsupportedOperationException("Cannot create a file in a virtual folder item.");
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setCanCreateChild(boolean canCreateChild) {
        this.canCreateChild = canCreateChild;
    }

    protected void setCanScrollDescendants(boolean canScrollDescendants) {
        this.canScrollDescendants = canScrollDescendants;
    }

}
