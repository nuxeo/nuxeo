/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.hierarchy.permission.factory;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.permission.adapter.UserSyncRootParentFolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceHelper;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.drive.service.impl.AbstractFileSystemItemFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of {@link FileSystemItemFactory} for the parent {@link FolderItem} of the user's
 * synchronization roots.
 *
 * @author Antoine Taillefer
 */
public class UserSyncRootParentFactory extends AbstractFileSystemItemFactory implements VirtualFolderItemFactory {

    private static final Logger log = LogManager.getLogger(UserSyncRootParentFactory.class);

    protected static final String FOLDER_NAME_PARAM = "folderName";

    protected String folderName;

    /*------------------- AbstractFileSystemItemFactory ------------------- */
    @Override
    public void handleParameters(Map<String, String> parameters) {
        // Look for the "folderName" parameter
        String folderNameParam = parameters.get(FOLDER_NAME_PARAM);
        if (StringUtils.isEmpty(folderNameParam)) {
            throw new NuxeoException(
                    String.format("Factory %s has no %s parameter, please provide one.", getName(), FOLDER_NAME_PARAM));
        }
        folderName = folderNameParam;
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint) {
        // Check user workspace
        boolean isUserWorkspace = UserWorkspaceHelper.isUserWorkspace(doc);
        if (!isUserWorkspace) {
            log.trace("Document {} is not a user workspace, it cannot be adapted as a FileSystemItem.", doc::getId);
            return false;
        }
        // Check trashed state
        if (!includeDeleted && doc.isTrashed()) {
            log.debug("Document {} is in the trash, it cannot be adapted as a FileSystemItem.", doc::getId);
            return false;
        }
        return true;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return new UserSyncRootParentFolderItem(getName(), doc, parentItem, folderName, relaxSyncRootConstraint,
                getLockInfo);
    }

    /*------------------- FileSystemItemFactory ------------------- */
    /**
     * Force parent item using {@link #getTopLevelFolderItem(Principal)}.
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        Principal principal = doc.getCoreSession().getPrincipal();
        return getFileSystemItem(doc, getTopLevelFolderItem(principal), includeDeleted);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        Principal principal = doc.getCoreSession().getPrincipal();
        return getFileSystemItem(doc, getTopLevelFolderItem(principal), includeDeleted, relaxSyncRootConstraint);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        Principal principal = doc.getCoreSession().getPrincipal();
        return getFileSystemItem(doc, getTopLevelFolderItem(principal), includeDeleted, relaxSyncRootConstraint,
                getLockInfo);
    }

    /*------------------- VirtualFolderItemFactory ------------------- */
    @Override
    public FolderItem getVirtualFolderItem(Principal principal) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        // TODO: handle multiple repositories
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryManager.getDefaultRepositoryName(),
                principal)) {
            UserWorkspaceService userWorkspaceService = Framework.getService(UserWorkspaceService.class);
            DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(session);
            if (userWorkspace == null) {
                throw new NuxeoException(
                        String.format("No personal workspace found for user %s.", principal.getName()));
            }
            return (FolderItem) getFileSystemItem(userWorkspace);
        }
    }

    @Override
    public String getFolderName() {
        return folderName;
    }

    @Override
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /*------------------- Protected ------------------- */
    protected FolderItem getTopLevelFolderItem(Principal principal) {
        FolderItem topLevelFolder = Framework.getService(FileSystemItemManager.class).getTopLevelFolder(principal);
        if (topLevelFolder == null) {
            throw new NuxeoException("Found no top level folder item. Please check your "
                    + "contribution to the following extension point:"
                    + " <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\""
                    + " point=\"topLevelFolderItemFactory\">.");
        }
        return topLevelFolder;
    }

}
