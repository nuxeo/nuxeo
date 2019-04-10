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

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a synchronization root {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultSyncRootFolderItem extends DocumentBackedFolderItem
        implements FolderItem {

    private static final long serialVersionUID = 1L;

    public DefaultSyncRootFolderItem(String factoryName, FolderItem parentItem,
            DocumentModel doc) throws ClientException {
        super(factoryName, parentItem, doc);
        // A sync root can always be deleted since deletion is implemented as
        // unregistration
        this.canDelete = true;
    }

    protected DefaultSyncRootFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public void delete() throws ClientException {
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        Framework.getLocalService(NuxeoDriveManager.class).unregisterSynchronizationRoot(
                principal, doc, session);
    }

    @Override
    public boolean canMove(FolderItem dest) throws ClientException {
        return false;
    }

    @Override
    public FileSystemItem move(FolderItem dest) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot move a synchronization root folder item.");
    }

}
