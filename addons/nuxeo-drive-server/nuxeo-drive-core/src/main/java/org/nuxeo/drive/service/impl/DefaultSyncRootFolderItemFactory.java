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
package org.nuxeo.drive.service.impl;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * Default {@link FileSystemItemFactory} for a synchronization root {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultSyncRootFolderItemFactory extends AbstractSyncRootFolderItemFactory {

    /*------------------- AbstractFileSystemItemFactory ---------------------*/
    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return new DefaultSyncRootFolderItem(name, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
    }

    /*------------------ AbstractSyncRootFolderItemFactory ------------------*/
    @Override
    protected FolderItem getParentItem(DocumentModel doc) {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        NuxeoPrincipal principal = doc.getCoreSession().getPrincipal();
        return fileSystemItemManager.getTopLevelFolder(principal);
    }

}
