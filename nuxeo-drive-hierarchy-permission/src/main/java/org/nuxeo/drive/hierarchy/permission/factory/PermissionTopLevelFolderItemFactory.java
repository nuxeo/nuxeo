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
package org.nuxeo.drive.hierarchy.permission.factory;

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.hierarchy.permission.adapter.PermissionTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * User workspace and permission based implementation of the
 * {@link TopLevelFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
public class PermissionTopLevelFolderItemFactory extends
        DefaultTopLevelFolderItemFactory {

    @Override
    public FolderItem getTopLevelFolderItem(String userName)
            throws ClientException {
        return new PermissionTopLevelFolderItem(this, userName);
    }

    @Override
    public String getSyncRootParentFolderItemId(String userName)
            throws ClientException {
        return "permissionSyncRootParentFolderItemFactory"
                + AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR;
    }
}
