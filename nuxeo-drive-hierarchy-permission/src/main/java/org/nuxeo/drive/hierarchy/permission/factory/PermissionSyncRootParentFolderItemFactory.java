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

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.PermissionSyncRootParentFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * Permission based implementation of {@link FileSystemItemFactory} for the
 * synchronization root parent {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class PermissionSyncRootParentFolderItemFactory extends
        DefaultTopLevelFolderItemFactory {

    protected static final String FOLDER_NAME_PARAM = "folderName";

    protected String folderName;

    /*--------------------------- TopLevelFolderItemFactory -----------------*/
    // Extends DefaultTopLevelFolderItemFactory for behavior
    // but not a topLevelFolderItemFactory
    @Override
    public FolderItem getTopLevelFolderItem(String userName)
            throws ClientException {
        throw new UnsupportedOperationException(String.format(
                "Factory %s is not a topLevelFolderItemFactory", getName()));
    }

    @Override
    public String getSyncRootParentFolderItemId(String userName)
            throws ClientException {
        throw new UnsupportedOperationException(String.format(
                "Factory %s is not a topLevelFolderItemFactory", getName()));
    }

    /*--------------------------- FileSystemItemFactory ---------------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) {
        String folderNameParam = parameters.get(FOLDER_NAME_PARAM);
        if (!StringUtils.isEmpty(folderNameParam)) {
            folderName = folderNameParam;
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException {
        if (!canHandleFileSystemItemId(id)) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot get the file system item for an id that cannot be handled from factory %s.",
                            getName()));
        }
        FileSystemItemManager fileSystemItemManager = Framework.getLocalService(FileSystemItemManager.class);
        String topLevelFolderItemId = fileSystemItemManager.getTopLevelFolder(
                principal).getId();
        return new PermissionSyncRootParentFolderItem(getName(),
                principal.getName(), topLevelFolderItemId, folderName);
    }
}
