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
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link FileSystemItem} implementations.
 *
 * @author Antoine Taillefer
 * @see AbstractDocumentBackedFileSystemItem
 * @see DefaultTopLevelFolderItem
 */
public abstract class AbstractFileSystemItem implements FileSystemItem {

    private static final long serialVersionUID = 1L;

    /** {@link FileSystemItem} attributes */
    protected String id;

    protected String parentId;

    protected String name;

    protected boolean folder;

    protected String creator;

    protected Calendar creationDate;

    protected Calendar lastModificationDate;

    protected boolean canRename;

    protected boolean canDelete;

    /** Internal attributes */
    protected String factoryName;

    // Must not be serialized => transient
    protected transient Principal principal;

    /**
     * Needed for JSON serialization/deserialization since we don't serialize
     * the principal
     */
    protected String userName;

    protected AbstractFileSystemItem(String factoryName, Principal principal) {
        this.factoryName = factoryName;
        this.principal = principal;
        this.userName = principal.getName();
        this.id = this.factoryName + "/";
    }

    protected AbstractFileSystemItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFolder() {
        return folder;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public Calendar getCreationDate() {
        return creationDate;
    }

    @Override
    public Calendar getLastModificationDate() {
        return lastModificationDate;
    }

    @Override
    public boolean getCanRename() {
        return canRename;
    }

    @Override
    public boolean getCanDelete() {
        return canDelete;
    }

    public abstract void rename(String name) throws ClientException;

    public abstract void delete() throws ClientException;

    /*---------- Needed for JSON serialization ----------*/
    public String getUserName() {
        return userName;
    }

    /*--------------------- Comparable -------------*/
    @Override
    public int compareTo(FileSystemItem other) {
        if (StringUtils.isEmpty(getName())
                && StringUtils.isEmpty(other.getName())) {
            return 0;
        }
        if (StringUtils.isEmpty(getName())
                && !StringUtils.isEmpty(other.getName())) {
            return -1;
        }
        if (!StringUtils.isEmpty(getName())
                && StringUtils.isEmpty(other.getName())) {
            return 1;
        }
        return getName().compareTo(other.getName());
    }

    /*--------------------- Object -----------------*/
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FileSystemItem)) {
            return false;
        }
        return getId().equals(((FileSystemItem) obj).getId());
    }

    @Override
    public String toString() {
        return String.format("%s(id=\"%s\", name=\"%s\")",
                getClass().getSimpleName(), getId(), getName());
    }

    /*--------------------- Protected ---------------------*/
    protected CoreSession getSession(String repositoryName)
            throws ClientException {
        return Framework.getLocalService(FileSystemItemManager.class).getSession(
                repositoryName, principal);
    }

    protected FileSystemItemAdapterService getFileSystemItemAdapterService() {
        return Framework.getLocalService(FileSystemItemAdapterService.class);
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setId(String id) {
        this.id = id;
    }

    protected void setParentId(String parentId) {
        this.parentId = parentId;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setFolder(boolean isFolder) {
        this.folder = isFolder;
    }

    protected void setCreator(String creator) {
        this.creator = creator;
    }

    protected void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    protected void setLastModificationDate(Calendar lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    protected void setCanRename(boolean canRename) {
        this.canRename = canRename;
    }

    protected void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    protected void setUserName(String userName) throws ClientException {
        this.userName = userName;
        this.principal = Framework.getLocalService(UserManager.class).getPrincipal(
                userName);
    }
}
