/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.adapter.impl;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a synchronization root {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultSyncRootFolderItem extends DocumentBackedFolderItem {

    private static final long serialVersionUID = 1L;

    public DefaultSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc) {
        this(factoryName, parentItem, doc, false);
    }

    public DefaultSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        this(factoryName, parentItem, doc, relaxSyncRootConstraint, true);
    }

    public DefaultSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
        // A sync root can always be deleted since deletion is implemented as
        // unregistration
        this.canDelete = true;
    }

    protected DefaultSyncRootFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public void delete() {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            DocumentModel doc = getDocument(session);
            Framework.getService(NuxeoDriveManager.class).unregisterSynchronizationRoot(principal, doc, session);
        }
    }

    @Override
    public boolean canMove(FolderItem dest) {
        return false;
    }

    @Override
    public FileSystemItem move(FolderItem dest) {
        throw new UnsupportedOperationException("Cannot move a synchronization root folder item.");
    }

}
