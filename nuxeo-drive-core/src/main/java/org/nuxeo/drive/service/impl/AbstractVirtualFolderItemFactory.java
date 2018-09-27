/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Base {@link FileSystemItemFactory} for a virtual {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public abstract class AbstractVirtualFolderItemFactory implements VirtualFolderItemFactory {

    private static final Log log = LogFactory.getLog(AbstractVirtualFolderItemFactory.class);

    protected static final String FOLDER_NAME_PARAM = "folderName";

    protected static final String DEFAULT_FOLDER_NAME = "Nuxeo Drive";

    protected String name;

    protected String folderName = DEFAULT_FOLDER_NAME;

    @Override
    public abstract FolderItem getVirtualFolderItem(Principal principal);

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void handleParameters(Map<String, String> parameters) {
        // Look for the "folderName" parameter
        String folderNameParam = parameters.get(FOLDER_NAME_PARAM);
        if (!StringUtils.isEmpty(folderNameParam)) {
            folderName = folderNameParam;
        } else {
            log.info(String.format(
                    "Factory %s has no %s parameter, you can provide one in the factory contribution to avoid using the default value '%s'.",
                    getName(), FOLDER_NAME_PARAM, DEFAULT_FOLDER_NAME));
        }
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc) {
        return isFileSystemItem(doc, false);
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return isFileSystemItem(doc, false, false);
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint) {
        return false;
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc) {
        return getFileSystemItem(doc, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return getFileSystemItem(doc, false, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, false, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        return null;
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem) {
        return getFileSystemItem(doc, parentItem, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted) {
        return getFileSystemItem(doc, parentItem, false, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, parentItem, false, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return null;
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        return (getName() + AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR).equals(id);
    }

    @Override
    public boolean exists(String id, Principal principal) {
        if (!canHandleFileSystemItemId(id)) {
            throw new UnsupportedOperationException(String.format(
                    "Cannot check if a file system item exists for an id that cannot be handled from factory %s.",
                    getName()));
        }
        return true;
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal) {
        if (!canHandleFileSystemItemId(id)) {
            throw new UnsupportedOperationException(String.format(
                    "Cannot get the file system item for an id that cannot be handled from factory %s.", getName()));
        }
        return getVirtualFolderItem(principal);
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, String parentId, Principal principal) {
        return getFileSystemItemById(id, principal);
    }

    @Override
    public String getFolderName() {
        return folderName;
    }

    @Override
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

}
