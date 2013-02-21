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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace and permission based implementation of the top level
 * {@link FolderItem}.
 * <p>
 * Implements the following tree:
 *
 * <pre>
 * Nuxeo Drive
 *  |-- My Documents (= user workspace)
 *  |      |-- File 1.doc
 *  |      |-- File 2.doc
 *  |      |-- Folder 1
 *  |      |-- ...
 *  |-- Other Documents (= synchronized roots with ReadWrite permission)
 *  |      |-- Other folder 1
 *  |      |-- Other folder 2
 *  |      |-- ...
 * </pre>
 *
 * @author Antoine Taillefer
 */
public class PermissionTopLevelFolderItem extends DefaultTopLevelFolderItem {

    private static final long serialVersionUID = 5179858544427598560L;

    private static final Log log = LogFactory.getLog(PermissionTopLevelFolderItem.class);

    protected TopLevelFolderItemFactory factory;

    public PermissionTopLevelFolderItem(TopLevelFolderItemFactory factory,
            String userName) throws ClientException {
        super(factory.getName(), userName);
        this.factory = factory;
    }

    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();

        // Add user workspace
        UserWorkspaceService userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        DocumentModel userWorkspace = userWorkspaceService.getUserPersonalWorkspace(
                userName, null);
        if (userWorkspace == null) {
            log.warn(String.format(
                    "No personal workspace found for user %s, not adding it to the top level folder children.",
                    userName));
        } else {
            FileSystemItem userWorkspaceFSItem = getFileSystemItemAdapterService().getFileSystemItem(
                    userWorkspace, id);
            if (userWorkspaceFSItem == null) {
                log.warn(String.format(
                        "Personal workspace of user %s is not adaptable as a FileSystemItem, not adding it to the top level folder children.",
                        userName));
            } else {
                children.add(userWorkspaceFSItem);
            }
        }

        // Add synchronization root parent folder
        String syncRootParentFolderItemId = factory.getSyncRootParentFolderItemId(userName);
        FileSystemItem syncRootParentFolderItem = getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                syncRootParentFolderItemId).getFileSystemItemById(
                syncRootParentFolderItemId, principal);
        if (syncRootParentFolderItem == null) {
            log.warn(String.format(
                    "No FileSystemItem found for id %s, not adding the synchronization root parent folder to the top level folder children.",
                    syncRootParentFolderItemId));
        } else {
            children.add(syncRootParentFolderItem);
        }

        return children;
    }

}
