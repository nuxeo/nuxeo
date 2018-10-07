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
package org.nuxeo.drive.hierarchy.permission.adapter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractVirtualFolderItem;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * User workspace and permission based implementation of the top level {@link FolderItem}.
 * <p>
 * Implements the following tree:
 *
 * <pre>
 * Nuxeo Drive
 *  |-- My Docs (= user workspace if synchronized else user synchronization roots)
 *  |      |-- Folder 1
 *  |      |-- Folder 2
 *  |      |-- ...
 *  |-- Other Docs (= user's shared synchronized roots with ReadWrite permission)
 *  |      |-- Other folder 1
 *  |      |-- Other folder 2
 *  |      |-- ...
 * </pre>
 *
 * @author Antoine Taillefer
 */
public class PermissionTopLevelFolderItem extends AbstractVirtualFolderItem {

    private static final long serialVersionUID = 5179858544427598560L;

    protected List<String> childrenFactoryNames;

    public PermissionTopLevelFolderItem(String factoryName, NuxeoPrincipal principal, String folderName,
            List<String> childrenFactoryNames) {
        super(factoryName, principal, null, null, folderName);
        this.childrenFactoryNames = childrenFactoryNames;
    }

    protected PermissionTopLevelFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public List<FileSystemItem> getChildren() {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        for (String childFactoryName : childrenFactoryNames) {
            VirtualFolderItemFactory factory = getFileSystemItemAdapterService().getVirtualFolderItemFactory(
                    childFactoryName);
            FolderItem child = factory.getVirtualFolderItem(principal);
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

}
