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
package org.nuxeo.drive.hierarchy.userworkspace.adapter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of the top level {@link FolderItem}.
 * <p>
 * Implements the following tree:
 *
 * <pre>
 * Nuxeo Drive
 *  |-- User workspace child 1
 *  |-- User workspace child 2
 *  |-- ...
 *  |-- My synchronized folders
 *         |-- Synchronized folder 1
 *         |-- Synchronized folder 2
 *         |-- ...
 * </pre>
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceTopLevelFolderItem extends DocumentBackedFolderItem {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserWorkspaceTopLevelFolderItem.class);

    protected DocumentModel userWorkspace;

    protected String syncRootParentFactoryName;

    public UserWorkspaceTopLevelFolderItem(String factoryName, DocumentModel userWorkspace, String folderName,
            String syncRootParentFactoryName) throws ClientException {
        this(factoryName, userWorkspace, folderName, syncRootParentFactoryName, false);
    }

    public UserWorkspaceTopLevelFolderItem(String factoryName, DocumentModel userWorkspace, String folderName,
            String syncRootParentFactoryName, boolean relaxSyncRootConstraint) throws ClientException {
        this(factoryName, userWorkspace, folderName, syncRootParentFactoryName, relaxSyncRootConstraint, true);
    }

    public UserWorkspaceTopLevelFolderItem(String factoryName, DocumentModel userWorkspace, String folderName,
            String syncRootParentFactoryName, boolean relaxSyncRootConstraint, boolean getLockInfo) throws ClientException {
        super(factoryName, null, userWorkspace, relaxSyncRootConstraint, getLockInfo);
        name = folderName;
        canRename = false;
        canDelete = false;
        this.userWorkspace = userWorkspace;
        this.syncRootParentFactoryName = syncRootParentFactoryName;
    }

    protected UserWorkspaceTopLevelFolderItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- AbstractFileSystemItem ---------------------*/
    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException("Cannot rename the top level folder item.");
    }

    @Override
    public void delete() throws ClientException {
        throw new UnsupportedOperationException("Cannot delete the top level folder item.");
    }

    @Override
    public FileSystemItem move(FolderItem dest) throws ClientException {
        throw new UnsupportedOperationException("Cannot move the top level folder item.");
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        // Register user workspace as a synchronization root if it is not
        // already the case
        if (!getNuxeoDriveManager().isSynchronizationRoot(principal, userWorkspace)) {
            getNuxeoDriveManager().registerSynchronizationRoot(principal, userWorkspace, getSession());
        }

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();

        // Add user workspace children
        children.addAll(super.getChildren());

        // Add synchronization root parent folder
        if (syncRootParentFactoryName == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "No synchronization root parent factory name parameter for factory %s, the synchronization roots won't be synchronized client side.",
                        factoryName));
            }

        } else {
            VirtualFolderItemFactory syncRootParentFactory = getFileSystemItemAdapterService().getVirtualFolderItemFactory(
                    syncRootParentFactoryName);
            FolderItem syncRootParent = syncRootParentFactory.getVirtualFolderItem(principal);
            if (syncRootParent != null) {
                children.add(syncRootParent);
            }
        }

        return children;
    }

    protected NuxeoDriveManager getNuxeoDriveManager() {
        return Framework.getLocalService(NuxeoDriveManager.class);
    }
}
