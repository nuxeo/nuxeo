/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.hierarchy.userworkspace.adapter;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
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

    private static final Logger log = LogManager.getLogger(UserWorkspaceTopLevelFolderItem.class);

    protected DocumentModel userWorkspace;

    protected String syncRootParentFactoryName;

    public UserWorkspaceTopLevelFolderItem(String factoryName, DocumentModel userWorkspace, String folderName,
            String syncRootParentFactoryName) {
        this(factoryName, userWorkspace, folderName, syncRootParentFactoryName, false);
    }

    public UserWorkspaceTopLevelFolderItem(String factoryName, DocumentModel userWorkspace, String folderName,
            String syncRootParentFactoryName, boolean relaxSyncRootConstraint) {
        this(factoryName, userWorkspace, folderName, syncRootParentFactoryName, relaxSyncRootConstraint, true);
    }

    public UserWorkspaceTopLevelFolderItem(String factoryName, DocumentModel userWorkspace, String folderName,
            String syncRootParentFactoryName, boolean relaxSyncRootConstraint, boolean getLockInfo) {
        super(factoryName, null, userWorkspace, relaxSyncRootConstraint, getLockInfo);
        name = folderName;
        canRename = false;
        canDelete = false;
        canScrollDescendants = false;
        this.userWorkspace = userWorkspace;
        this.syncRootParentFactoryName = syncRootParentFactoryName;
        // detach user workspace as we will use another session to update it
        userWorkspace.detach(true);
    }

    protected UserWorkspaceTopLevelFolderItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- AbstractFileSystemItem ---------------------*/
    @Override
    public void rename(String name) {
        throw new UnsupportedOperationException("Cannot rename the top level folder item.");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Cannot delete the top level folder item.");
    }

    @Override
    public FileSystemItem move(FolderItem dest) {
        throw new UnsupportedOperationException("Cannot move the top level folder item.");
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    public List<FileSystemItem> getChildren() {

        // Register user workspace as a synchronization root if it is not
        // already the case
        if (!getNuxeoDriveManager().isSynchronizationRoot(principal, userWorkspace)) {
            try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                getNuxeoDriveManager().registerSynchronizationRoot(principal, userWorkspace, session);
            }
        }

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();

        // Add user workspace children
        children.addAll(super.getChildren());

        // Add synchronization root parent folder
        if (syncRootParentFactoryName == null) {
            log.debug(
                    "No synchronization root parent factory name parameter for factory {}, the synchronization roots won't be synchronized client side.",
                    factoryName);
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

    @Override
    public ScrollFileSystemItemList scrollDescendants(String scrollId, int batchSize, long keepAlive) {
        throw new UnsupportedOperationException(
                "Cannot scroll through the descendants of the user workspace top level folder item, please call getChildren() instead.");
    }

    protected NuxeoDriveManager getNuxeoDriveManager() {
        return Framework.getService(NuxeoDriveManager.class);
    }
}
