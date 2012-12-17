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

import java.util.Calendar;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Base class for {@link FileSystemItem} implementations.
 *
 * @author Antoine Taillefer
 * @see AbstractDocumentBackedFileSystemItem
 * @see DefaultTopLevelFolderItem
 */
public abstract class AbstractFileSystemItem implements FileSystemItem {

    protected final String factoryName;

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

}
