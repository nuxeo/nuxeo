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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.hierarchy.permission.adapter.UserWorkspaceFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.impl.DefaultSyncRootFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace implementation of {@link FileSystemItemFactory}.
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceFolderItemFactory extends
        DefaultSyncRootFolderItemFactory {

    private static final Log log = LogFactory.getLog(UserWorkspaceFolderItemFactory.class);

    protected static final String FOLDER_NAME_PARAM = "folderName";

    protected String folderName;

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted) throws ClientException {
        // If the doc is not adaptable as a FileSystemItem return null
        if (!isFileSystemItem(doc, includeDeleted)) {
            log.debug(String.format(
                    "Document %s cannot be adapted as a FileSystemItem => returning null.",
                    doc.getId()));
            return null;
        }
        return adaptDocument(doc, false, null);
    }

    @Override
    public void handleParameters(Map<String, String> parameters) {
        String folderNameParam = parameters.get(FOLDER_NAME_PARAM);
        if (!StringUtils.isEmpty(folderNameParam)) {
            folderName = folderNameParam;
        }
    }

    /**
     * The permission synchronization root factory considers that a
     * {@link DocumentModel} is adaptable as a {@link FileSystemItem} if:
     * <ul>
     * <li>It is a user workspace</li>
     * <li>AND it is not in the "deleted" life cycle state, unless
     * {@code includeDeleted} is true</li>
     * <li>AND it is a synchronization root registered for the current user</li>
     * </ul>
     *
     * TODO: Check if it the current user's personal workspace, and not a only a
     * user workspace?
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted)
            throws ClientException {
        // Check user workspace
        boolean isUserWorkspace = "UserWorkspacesRoot".equals(doc.getCoreSession().getSuperParentType(
                doc));
        return isUserWorkspace && super.isFileSystemItem(doc, includeDeleted);
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc,
            boolean forceParentId, String parentId) throws ClientException {
        FileSystemItemManager fileSystemItemManager = Framework.getLocalService(FileSystemItemManager.class);
        String topLevelFolderItemId = fileSystemItemManager.getTopLevelFolder(
                doc.getCoreSession().getPrincipal()).getId();
        return new UserWorkspaceFolderItem(name, topLevelFolderItemId, doc,
                folderName);
    }

}
