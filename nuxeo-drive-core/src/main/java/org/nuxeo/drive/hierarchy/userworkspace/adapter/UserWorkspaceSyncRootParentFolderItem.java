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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractVirtualFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
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

    private static final Log log = LogFactory.getLog(UserWorkspaceSyncRootParentFolderItem.class);

    public UserWorkspaceSyncRootParentFolderItem(String factoryName, Principal principal, String parentId,
            String parentPath, String folderName) throws ClientException {
        super(factoryName, principal, parentId, parentPath, folderName);
    }

    protected UserWorkspaceSyncRootParentFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        Map<String, SynchronizationRoots> syncRootsByRepo = Framework.getLocalService(NuxeoDriveManager.class).getSynchronizationRoots(
                principal);
        for (String repositoryName : syncRootsByRepo.keySet()) {
            CoreSession session = getSession(repositoryName);
            Set<IdRef> syncRootRefs = syncRootsByRepo.get(repositoryName).getRefs();
            Iterator<IdRef> syncRootRefsIt = syncRootRefs.iterator();
            while (syncRootRefsIt.hasNext()) {
                IdRef idRef = syncRootRefsIt.next();
                // TODO: ensure sync roots cache is up-to-date if ACL
                // change, for now need to check permission
                // See https://jira.nuxeo.com/browse/NXP-11146
                if (!session.hasPermission(idRef, SecurityConstants.READ)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "User %s has no READ access on synchronization root %s, not including it in children.",
                                session.getPrincipal().getName(), idRef));
                    }
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
                        if (log.isDebugEnabled()) {
                            log.debug(String.format(
                                    "Synchronization root %s cannot be adapted as a FileSystemItem, not including it in children.",
                                    idRef));
                        }
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Including synchronization root %s in children.", idRef));
                    }
                    children.add(child);
                }
            }
        }
        Collections.sort(children);
        return children;
    }

}
