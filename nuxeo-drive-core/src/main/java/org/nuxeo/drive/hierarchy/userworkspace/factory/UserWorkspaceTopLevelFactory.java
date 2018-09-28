/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.hierarchy.userworkspace.factory;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceHelper;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.AbstractFileSystemItemFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of the {@link TopLevelFolderItemFactory}.
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceTopLevelFactory extends AbstractFileSystemItemFactory implements TopLevelFolderItemFactory {

    private static final Logger log = LogManager.getLogger(UserWorkspaceTopLevelFactory.class);

    protected static final String FOLDER_NAME_PARAM = "folderName";

    protected static final String SYNC_ROOT_PARENT_FACTORY_PARAM = "syncRootParentFactory";

    protected static final String DEFAULT_FOLDER_NAME = "Nuxeo Drive";

    protected String folderName = DEFAULT_FOLDER_NAME;

    protected String syncRootParentFactoryName;

    /*---------------------- AbstractFileSystemItemFactory ---------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) {
        // Look for the "folderName" parameter
        String folderNameParam = parameters.get(FOLDER_NAME_PARAM);
        if (!StringUtils.isEmpty(folderNameParam)) {
            folderName = folderNameParam;
        } else {
            log.info(
                    "Factory {} has no {} parameter, you can provide one in the factory contribution to avoid using the default value '{}'.",
                    this::getName, () -> FOLDER_NAME_PARAM, () -> DEFAULT_FOLDER_NAME);
        }
        // Look for the "syncRootParentFactory" parameter
        String syncRootParentFactoryParam = parameters.get(SYNC_ROOT_PARENT_FACTORY_PARAM);
        if (!StringUtils.isEmpty(syncRootParentFactoryParam)) {
            syncRootParentFactoryName = syncRootParentFactoryParam;
        } else {
            log.warn(
                    "Factory {} has no {} parameter, please provide one in the factory contribution to set the name of the synchronization root parent factory.",
                    this::getName, () -> SYNC_ROOT_PARENT_FACTORY_PARAM);
        }
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint) {
        // Check user workspace
        boolean isUserWorkspace = UserWorkspaceHelper.isUserWorkspace(doc);
        if (!isUserWorkspace) {
            log.trace("Document {} is not a user workspace, it cannot be adapted as a FileSystemItem.", doc::getId);
            return false;
        }
        return true;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return new UserWorkspaceTopLevelFolderItem(getName(), doc, folderName, syncRootParentFactoryName,
                relaxSyncRootConstraint, getLockInfo);
    }

    /*---------------------- VirtualFolderItemFactory ---------------*/
    @Override
    public FolderItem getVirtualFolderItem(Principal principal) {
        return getTopLevelFolderItem(principal);
    }

    @Override
    public String getFolderName() {
        return folderName;
    }

    @Override
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /*----------------------- TopLevelFolderItemFactory ---------------------*/
    @Override
    public FolderItem getTopLevelFolderItem(Principal principal) {
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

}
