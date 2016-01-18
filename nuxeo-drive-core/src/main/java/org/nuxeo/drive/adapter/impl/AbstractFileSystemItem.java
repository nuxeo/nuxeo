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
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.Lock;
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

    public static final String FILE_SYSTEM_ITEM_ID_SEPARATOR = "#";

    private static final long serialVersionUID = 1L;

    /** {@link FileSystemItem} attributes */
    protected String id;

    protected String parentId;

    protected String name;

    protected boolean folder;

    protected String creator;

    protected String lastContributor;

    protected Calendar creationDate;

    protected Calendar lastModificationDate;

    protected boolean canRename;

    protected boolean canDelete;

    protected Lock lockInfo;

    /** Internal attributes */
    protected String factoryName;

    protected String path;

    // Must not be serialized => transient
    protected transient Principal principal;

    /**
     * Needed for JSON serialization/deserialization since we don't serialize the principal
     */
    protected String userName;

    protected AbstractFileSystemItem(String factoryName, Principal principal, boolean relaxSyncRootConstraint) {
        this.factoryName = factoryName;
        this.principal = principal;
        this.userName = principal.getName();
        if (relaxSyncRootConstraint) {
            // Don't include factory name in id as in this case the document can
            // be adapted by different factories depending on the principal.
            // Typically a document that is a sync root for a user but a
            // subfolder of a sync root for another one.
            // See https://jira.nuxeo.com/browse/NXP-16038
            this.id = StringUtils.EMPTY;
        } else {
            this.id = this.factoryName + FILE_SYSTEM_ITEM_ID_SEPARATOR;
        }
    }

    protected AbstractFileSystemItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public abstract void rename(String name) throws ClientException;

    @Override
    public abstract void delete() throws ClientException;

    @Override
    public abstract boolean canMove(FolderItem dest) throws ClientException;

    @Override
    public abstract FileSystemItem move(FolderItem dest) throws ClientException;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        return path;
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
    public String getLastContributor() {
        return lastContributor;
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

    @Override
    public Lock getLockInfo() {
        return lockInfo;
    }

    /*---------- Needed for JSON serialization ----------*/
    public String getUserName() {
        return userName;
    }

    /*--------------------- Comparable -------------*/
    @Override
    public int compareTo(FileSystemItem other) {
        if (StringUtils.isEmpty(getName()) && StringUtils.isEmpty(other.getName())) {
            return 0;
        }
        if (StringUtils.isEmpty(getName()) && !StringUtils.isEmpty(other.getName())) {
            return -1;
        }
        if (!StringUtils.isEmpty(getName()) && StringUtils.isEmpty(other.getName())) {
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
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s(id=\"%s\", name=\"%s\")", getClass().getSimpleName(), getId(), getName());
    }

    /*--------------------- Protected ---------------------*/
    protected CoreSession getSession(String repositoryName) throws ClientException {
        return getFileSystemItemManager().getSession(repositoryName, principal);
    }

    protected FileSystemItemManager getFileSystemItemManager() {
        return Framework.getLocalService(FileSystemItemManager.class);
    }

    protected FileSystemItemAdapterService getFileSystemItemAdapterService() {
        return Framework.getLocalService(FileSystemItemAdapterService.class);
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setId(String id) {
        this.id = id;
    }

    protected void setPath(String path) {
        this.path = path;
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

    protected void setLastContributor(String lastContributor) {
        this.lastContributor = lastContributor;
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

    protected void setLockInfo(Lock lockInfo) {
        this.lockInfo = lockInfo;
    }

    protected void setUserName(String userName) throws ClientException {
        this.userName = userName;
        this.principal = Framework.getLocalService(UserManager.class).getPrincipal(userName);
    }
}
