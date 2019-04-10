/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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

    public static final String FILE_SYSTEM_ITEM_ID_SEPARATOR = "#";

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
    protected transient NuxeoPrincipal principal;

    /**
     * Needed for JSON serialization/deserialization since we don't serialize the principal
     */
    protected String userName;

    protected AbstractFileSystemItem(String factoryName, NuxeoPrincipal principal, boolean relaxSyncRootConstraint) {
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
    public abstract void rename(String name);

    @Override
    public abstract void delete();

    @Override
    public abstract boolean canMove(FolderItem dest);

    @Override
    public abstract FileSystemItem move(FolderItem dest);

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
    protected FileSystemItemAdapterService getFileSystemItemAdapterService() {
        return Framework.getService(FileSystemItemAdapterService.class);
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

    protected void setUserName(String userName) {
        this.userName = userName;
        this.principal = Framework.getService(UserManager.class).getPrincipal(userName);
    }
}
