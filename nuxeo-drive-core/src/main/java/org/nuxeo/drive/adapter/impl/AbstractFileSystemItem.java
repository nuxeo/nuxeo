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
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for {@link FileSystemItem} implementations.
 *
 * @author Antoine Taillefer
 * @see AbstractDocumentBackedFileSystemItem
 * @see DefaultTopLevelFolderItem
 */
public abstract class AbstractFileSystemItem implements FileSystemItem {

    protected final String factoryName;

    protected Principal principal;

    protected AbstractFileSystemItem(String factoryName) {
        this.factoryName = factoryName;
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public String getId() {
        return factoryName + "/";
    }

    public abstract String getParentId();

    public abstract String getName();

    public abstract boolean isFolder();

    public abstract String getCreator();

    public abstract Calendar getCreationDate();

    public abstract Calendar getLastModificationDate();

    public abstract boolean getCanRename();

    public abstract void rename(String name) throws ClientException;

    public abstract boolean getCanDelete();

    public abstract void delete() throws ClientException;

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
}
