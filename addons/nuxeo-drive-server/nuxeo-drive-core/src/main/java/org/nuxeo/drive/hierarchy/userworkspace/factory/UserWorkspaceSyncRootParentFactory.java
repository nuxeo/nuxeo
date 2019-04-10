/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceSyncRootParentFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.drive.service.impl.AbstractVirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * User workspace based implementation of {@link FileSystemItemFactory} for the parent {@link FolderItem} of the user's
 * synchronization roots.
 *
 * @author Antoine Taillefer
 */
public class UserWorkspaceSyncRootParentFactory extends AbstractVirtualFolderItemFactory {

    @Override
    public FolderItem getVirtualFolderItem(NuxeoPrincipal principal) {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        FolderItem topLevelFolder = fileSystemItemManager.getTopLevelFolder(principal);
        if (topLevelFolder == null) {
            throw new NuxeoException(
                    "Found no top level folder item. Please check your contribution to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"topLevelFolderItemFactory\">.");
        }
        return new UserWorkspaceSyncRootParentFolderItem(getName(), principal, topLevelFolder.getId(),
                topLevelFolder.getPath(), folderName);
    }

}
