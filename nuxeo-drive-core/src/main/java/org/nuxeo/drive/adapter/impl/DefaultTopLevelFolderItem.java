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
package org.nuxeo.drive.adapter.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of the top level {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultTopLevelFolderItem extends AbstractFileSystemItem implements
        FolderItem {

    private static final long serialVersionUID = 1L;

    protected boolean canCreateChild;

    public DefaultTopLevelFolderItem(String factoryName, String userName)
            throws ClientException {
        super(factoryName,
                Framework.getLocalService(UserManager.class).getPrincipal(
                        userName));
        this.parentId = null;
        this.name = "Nuxeo Drive";
        this.folder = true;
        this.creator = "system";
        Calendar cal = Calendar.getInstance();
        this.creationDate = cal;
        this.lastModificationDate = this.creationDate;
        this.canRename = false;
        this.canDelete = false;
        this.canCreateChild = false;
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot rename a system folder item.");
    }

    @Override
    public void delete() throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot delete a system folder item.");
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        Map<String, SynchronizationRoots> syncRootsByRepo = Framework.getLocalService(
                NuxeoDriveManager.class).getSynchronizationRoots(principal);
        for (String repositoryName : syncRootsByRepo.keySet()) {
            CoreSession session = getSession(repositoryName);
            Set<IdRef> syncRootRefs = syncRootsByRepo.get(repositoryName).refs;
            Iterator<IdRef> syncRootRefsIt = syncRootRefs.iterator();
            while (syncRootRefsIt.hasNext()) {
                IdRef idRef = syncRootRefsIt.next();
                DocumentModel doc = session.getDocument(idRef);
                children.add(getFileSystemItemAdapterService().getFileSystemItem(
                        doc, getId()));
            }
        }
        Collections.sort(children);
        return children;
    }

    @Override
    public boolean getCanCreateChild() {
        return canCreateChild;
    }

    @Override
    public FolderItem createFolder(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a folder in a system folder item.");
    }

    @Override
    public FileItem createFile(Blob blob) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a file in a system folder item.");
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setCanCreateChild(boolean canCreateChild) {
        this.canCreateChild = canCreateChild;
    }

}
