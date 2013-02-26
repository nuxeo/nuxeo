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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of the parent {@link FolderItem} of the
 * user's synchronization roots.
 *
 * @author Antoine Taillefer
 */
public class UserSyncRootParentFolderItem extends DocumentBackedFolderItem {

    private static final long serialVersionUID = 1L;

    protected boolean isUserWorkspaceSyncRoot = false;

    public UserSyncRootParentFolderItem(String factoryName, DocumentModel doc,
            String parentId, String folderName) throws ClientException {
        super(factoryName, parentId, doc);
        name = folderName;
        canRename = false;
        canDelete = false;
        isUserWorkspaceSyncRoot = isUserWorkspaceSyncRoot(doc);
        canCreateChild = isUserWorkspaceSyncRoot;
    }

    protected UserSyncRootParentFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot rename a virtual folder item.");
    }

    @Override
    public void delete() throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot delete a virtual folder item.");
    }

    @Override
    public FileSystemItem move(FolderItem dest) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot move a virtual folder item.");
    }

    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        if (isUserWorkspaceSyncRoot) {
            return super.getChildren();
        } else {
            List<FileSystemItem> children = new ArrayList<FileSystemItem>();
            Map<String, SynchronizationRoots> syncRootsByRepo = Framework.getLocalService(
                    NuxeoDriveManager.class).getSynchronizationRoots(principal);
            for (String repositoryName : syncRootsByRepo.keySet()) {
                CoreSession session = getSession(repositoryName);
                Set<IdRef> syncRootRefs = syncRootsByRepo.get(repositoryName).refs;
                Iterator<IdRef> syncRootRefsIt = syncRootRefs.iterator();
                while (syncRootRefsIt.hasNext()) {
                    IdRef idRef = syncRootRefsIt.next();
                    // TODO: handle DocumentSecurityException
                    DocumentModel doc = session.getDocument(idRef);
                    // Filter by creator
                    // TODO: allow filtering by dc:creator in
                    // NuxeoDriveManager#getSynchronizationRoots(Principal
                    // principal)
                    if (session.getPrincipal().getName().equals(
                            doc.getPropertyValue("dc:creator"))) {
                        // TODO: handle null FileSystemItem
                        children.add(getFileSystemItemAdapterService().getFileSystemItem(
                                doc, getId()));
                    }
                }
            }
            Collections.sort(children);
            return children;
        }
    }

    private boolean isUserWorkspaceSyncRoot(DocumentModel doc)
            throws ClientException {
        NuxeoDriveManager nuxeoDriveManager = Framework.getLocalService(NuxeoDriveManager.class);
        return nuxeoDriveManager.isSynchronizationRoot(principal, doc);
    }

}
