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
package org.nuxeo.drive.hierarchy.permission.adapter;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * User workspace implementation of a {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceFolderItem extends DocumentBackedFolderItem {

    private static final long serialVersionUID = 5179858544427598560L;

    public UserWorkspaceFolderItem(String factoryName, String parentId,
            DocumentModel userWorkspace, String folderName)
            throws ClientException {
        super(factoryName, parentId, userWorkspace);
        this.name = folderName;
    }

    @Override
    public boolean getCanRename() {
        return false;
    }

    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot rename the user workspace folder item.");
    }

    @Override
    public boolean getCanDelete() {
        return false;
    }

    @Override
    public void delete() throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot delete the top level folder item.");
    }

    @Override
    public FileSystemItem move(FolderItem dest) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot move the user workspace folder item.");
    }

}
