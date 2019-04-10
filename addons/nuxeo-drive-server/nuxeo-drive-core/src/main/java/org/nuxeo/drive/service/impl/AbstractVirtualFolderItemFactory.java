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
package org.nuxeo.drive.service.impl;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Base {@link FileSystemItemFactory} for a virtual {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public abstract class AbstractVirtualFolderItemFactory implements
        VirtualFolderItemFactory {

    private static final Log log = LogFactory.getLog(AbstractVirtualFolderItemFactory.class);

    protected static final String FOLDER_NAME_PARAM = "folderName";

    protected static final String DEFAULT_FOLDER_NAME = "Nuxeo Drive";

    protected String name;

    protected String folderName = DEFAULT_FOLDER_NAME;

    @Override
    public abstract FolderItem getVirtualFolderItem(Principal principal)
            throws ClientException;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void handleParameters(Map<String, String> parameters)
            throws ClientException {
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
    public boolean isFileSystemItem(DocumentModel doc) throws ClientException {
        return isFileSystemItem(doc, false);
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted)
            throws ClientException {
        return isFileSystemItem(doc, false, false);
    }

    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException {
        return false;
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc)
            throws ClientException {
        return getFileSystemItem(doc, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted) throws ClientException {
        return getFileSystemItem(doc, false, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {
        return null;
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            FolderItem parentItem) throws ClientException {
        return getFileSystemItem(doc, parentItem, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            FolderItem parentItem, boolean includeDeleted)
            throws ClientException {
        return getFileSystemItem(doc, parentItem, false, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException {
        return null;
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        return (getName() + AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR).equals(id);
    }

    @Override
    public boolean exists(String id, Principal principal)
            throws ClientException {
        if (!canHandleFileSystemItemId(id)) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot check if a file system item exists for an id that cannot be handled from factory %s.",
                            getName()));
        }
        return true;
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException {
        if (!canHandleFileSystemItemId(id)) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot get the file system item for an id that cannot be handled from factory %s.",
                            getName()));
        }
        return getVirtualFolderItem(principal);
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, String parentId,
            Principal principal) throws ClientException {
        return getFileSystemItemById(parentId, principal);
    }

    @Override
    public DocumentModel getDocumentByFileSystemId(String id,
            Principal principal) throws ClientException {
        throw new UnsupportedOperationException(
                String.format(
                        "Cannot get document by file system item id from VirtualFolderItemFactory %s.",
                        getName()));
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
