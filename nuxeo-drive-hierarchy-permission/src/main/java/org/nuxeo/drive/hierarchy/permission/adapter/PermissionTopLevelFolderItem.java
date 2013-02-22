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
package org.nuxeo.drive.hierarchy.permission.adapter;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.adapter.impl.AbstractVirtualFolderItem;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Permission based implementation of the top level {@link FolderItem}.
 * <p>
 * Implements the following tree:
 *
 * <pre>
 * Nuxeo Drive
 *  |-- My Documents (= user synchronization roots)
 *  |      |-- Folder 1
 *  |      |-- Folder 2
 *  |      |-- ...
 *  |-- Other Documents (= user's shared synchronized roots with ReadWrite permission)
 *  |      |-- Other folder 1
 *  |      |-- Other folder 2
 *  |      |-- ...
 * </pre>
 *
 * @author Antoine Taillefer
 */
public class PermissionTopLevelFolderItem extends AbstractVirtualFolderItem {

    private static final long serialVersionUID = 5179858544427598560L;

    private static final Log log = LogFactory.getLog(PermissionTopLevelFolderItem.class);

    protected List<String> childrenFactoryNames;

    public PermissionTopLevelFolderItem(String factoryName,
            Principal principal, String folderName,
            List<String> childrenFactoryNames) throws ClientException {
        super(factoryName, principal, null, folderName);
        this.childrenFactoryNames = childrenFactoryNames;
    }

    protected PermissionTopLevelFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    public List<FileSystemItem> getChildren() throws ClientException {

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        for (String childFactoryName : childrenFactoryNames) {
            String childFileSystemItemId = childFactoryName
                    + AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR;
            FileSystemItem childFileSystemItem = getFileSystemItemAdapterService().getFileSystemItemFactoryForId(
                    childFileSystemItemId).getFileSystemItemById(
                    childFileSystemItemId, principal);
            if (childFileSystemItem == null) {
                log.warn(String.format("No FileSystemItem found for id %s.",
                        childFileSystemItemId));
            } else {
                children.add(childFileSystemItem);
            }
        }
        return children;
    }

}
