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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of the top level {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultTopLevelFolderItem extends AbstractVirtualFolderItem {

    private static final long serialVersionUID = 1L;

    public DefaultTopLevelFolderItem(String factoryName, Principal principal,
            String folderName) throws ClientException {
        super(factoryName, principal, null, folderName);
    }

    protected DefaultTopLevelFolderItem() {
        // Needed for JSON deserialization
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

}
