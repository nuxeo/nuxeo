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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.impl.AbstractSyncRootFolderItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of {@link FileSystemItemFactory} for a synchronization root {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceSyncRootFactory extends AbstractSyncRootFolderItemFactory {

    protected static final String SYNC_ROOT_PARENT_FACTORY_PARAM = "syncRootParentFactory";

    protected String syncRootParentFactoryName;

    /*------------------- AbstractFileSystemItemFactory ---------------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) {
        String syncRootParentFactoryParam = parameters.get(SYNC_ROOT_PARENT_FACTORY_PARAM);
        if (StringUtils.isEmpty(syncRootParentFactoryParam)) {
            throw new NuxeoException(String.format(
                    "Factory %s has no %s parameter, please provide it in the factory contribution to set the name of the factory for the parent folder of the synchronization roots.",
                    getName(), SYNC_ROOT_PARENT_FACTORY_PARAM));
        }
        syncRootParentFactoryName = syncRootParentFactoryParam;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return new DefaultSyncRootFolderItem(name, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
    }

    /*------------------ AbstractSyncRootFolderItemFactory ------------------*/
    @Override
    protected FolderItem getParentItem(DocumentModel doc) {
        NuxeoPrincipal principal = doc.getCoreSession().getPrincipal();
        FolderItem parent = getFileSystemAdapterService().getVirtualFolderItemFactory(syncRootParentFactoryName)
                                                         .getVirtualFolderItem(principal);
        if (parent == null) {
            throw new NuxeoException(
                    String.format("Cannot find the parent of document %s: virtual folder from factory %s.", doc.getId(),
                            syncRootParentFactoryName));
        }
        return parent;
    }

    protected FileSystemItemAdapterService getFileSystemAdapterService() {
        return Framework.getService(FileSystemItemAdapterService.class);
    }

}
