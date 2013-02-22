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

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Permission based implementation of the synchronization root parent
 * {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class PermissionSyncRootParentFolderItem extends
        DefaultTopLevelFolderItem {

    private static final long serialVersionUID = 1L;

    public PermissionSyncRootParentFolderItem(String factoryName,
            String userName, String parentId, String folderName)
            throws ClientException {
        super(factoryName, userName);
        this.parentId = parentId;
        this.name = folderName;
    }

    protected PermissionSyncRootParentFolderItem() {
        // Needed for JSON deserialization
    }

}
