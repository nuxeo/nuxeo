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

    public DefaultSyncRootFolderItem(String factoryName, String parentId,
            DocumentModel doc) throws ClientException {
        super(factoryName, parentId, doc);
        this.canRename = false;
    }

    @Override
    public void rename(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot rename a synchronization root folder item.");
    }

    @Override
    public void delete() throws ClientException {
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        Framework.getLocalService(NuxeoDriveManager.class).unregisterSynchronizationRoot(
                principal.getName(), doc, session);
    }

}
