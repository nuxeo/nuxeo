/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.hierarchy.userworkspace.factory;

import java.security.Principal;

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceSyncRootParentFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.impl.AbstractVirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of {@link FileSystemItemFactory} for the parent {@link FolderItem} of the user's
 * synchronization roots.
 * 
 * @author Antoine Taillefer
 */
public class UserWorkspaceSyncRootParentFactory extends AbstractVirtualFolderItemFactory {

    @Override
    public FolderItem getVirtualFolderItem(Principal principal) throws ClientException {
        FileSystemItemManager fileSystemItemManager = Framework.getLocalService(FileSystemItemManager.class);
        FolderItem topLevelFolder = fileSystemItemManager.getTopLevelFolder(principal);
        if (topLevelFolder == null) {
            throw new ClientException(
                    "Found no top level folder item. Please check your contribution to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"topLevelFolderItemFactory\">.");
        }
        return new UserWorkspaceSyncRootParentFolderItem(getName(), principal, topLevelFolder.getId(),
                topLevelFolder.getPath(), folderName);
    }

}
