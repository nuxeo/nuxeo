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
package org.nuxeo.drive.hierarchy.userworkspace.factory;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.impl.AbstractSyncRootFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
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
    public void handleParameters(Map<String, String> parameters) throws ClientException {
        String syncRootParentFactoryParam = parameters.get(SYNC_ROOT_PARENT_FACTORY_PARAM);
        if (StringUtils.isEmpty(syncRootParentFactoryParam)) {
            throw new ClientException(
                    String.format(
                            "Factory %s has no %s parameter, please provide it in the factory contribution to set the name of the factory for the parent folder of the synchronization roots.",
                            getName(), SYNC_ROOT_PARENT_FACTORY_PARAM));
        }
        syncRootParentFactoryName = syncRootParentFactoryParam;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) throws ClientException {
        return new DefaultSyncRootFolderItem(name, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
    }

    /*------------------ AbstractSyncRootFolderItemFactory ------------------*/
    @Override
    protected FolderItem getParentItem(DocumentModel doc) throws ClientException {
        Principal principal = doc.getCoreSession().getPrincipal();
        FolderItem parent = getFileSystemAdapterService().getVirtualFolderItemFactory(syncRootParentFactoryName)
                                                         .getVirtualFolderItem(principal);
        if (parent == null) {
            throw new ClientException(String.format(
                    "Cannot find the parent of document %s: virtual folder from factory %s.", doc.getId(),
                    syncRootParentFactoryName));
        }
        return parent;
    }

    protected FileSystemItemAdapterService getFileSystemAdapterService() {
        return Framework.getLocalService(FileSystemItemAdapterService.class);
    }

}
