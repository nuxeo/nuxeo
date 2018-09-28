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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractVirtualFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of the synchronization root parent {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceSyncRootParentFolderItem extends AbstractVirtualFolderItem {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(UserWorkspaceSyncRootParentFolderItem.class);

    public UserWorkspaceSyncRootParentFolderItem(String factoryName, Principal principal, String parentId,
            String parentPath, String folderName) {
        super(factoryName, principal, parentId, parentPath, folderName);
    }

    protected UserWorkspaceSyncRootParentFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public List<FileSystemItem> getChildren() {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        Map<String, SynchronizationRoots> syncRootsByRepo = Framework.getService(NuxeoDriveManager.class)
                                                                     .getSynchronizationRoots(principal);
        for (String repositoryName : syncRootsByRepo.keySet()) {
            try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                Set<IdRef> syncRootRefs = syncRootsByRepo.get(repositoryName).getRefs();
                Iterator<IdRef> syncRootRefsIt = syncRootRefs.iterator();
                while (syncRootRefsIt.hasNext()) {
                    IdRef idRef = syncRootRefsIt.next();
                    // TODO: ensure sync roots cache is up-to-date if ACL
                    // change, for now need to check permission
                    // See https://jira.nuxeo.com/browse/NXP-11146
                    if (!session.hasPermission(idRef, SecurityConstants.READ)) {
                        log.debug(
                                "User {} has no READ access on synchronization root {}, not including it in children.",
                                session::getPrincipal, () -> idRef);
                        continue;
                    }
                    DocumentModel doc = session.getDocument(idRef);
                    // Don't include user workspace (ie.top level folder) if
                    // registered as a synchronization root to avoid recursion
                    if (!UserWorkspaceHelper.isUserWorkspace(doc)) {
                        // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
                        FileSystemItem child = getFileSystemItemAdapterService().getFileSystemItem(doc, this, false,
                                false, false);
                        if (child == null) {
                            log.debug(
                                    "Synchronization root {} cannot be adapted as a FileSystemItem, not including it in children.",
                                    idRef);
                            continue;
                        }
                        log.debug("Including synchronization root {} in children.", idRef);
                        children.add(child);
                    }
                }
            }
        }
        Collections.sort(children);
        return children;
    }

}
