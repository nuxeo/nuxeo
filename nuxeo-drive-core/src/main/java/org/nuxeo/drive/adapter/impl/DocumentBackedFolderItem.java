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

import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} backed implementation of a {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFolderItem extends AbstractDocumentBackedFileSystemItem implements FolderItem {

    private static final long serialVersionUID = 1L;

    private static final String FOLDER_ITEM_CHILDREN_PAGE_PROVIDER = "FOLDER_ITEM_CHILDREN";

    protected boolean canCreateChild;

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc) throws ClientException {
        this(factoryName, doc, false);
    }

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc, boolean relaxSyncRootConstraint)
            throws ClientException {
        this(factoryName, doc, relaxSyncRootConstraint, true);
    }

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc, boolean relaxSyncRootConstraint,
            boolean getLockInfo) throws ClientException {
        super(factoryName, doc, relaxSyncRootConstraint, getLockInfo);
        initialize(doc);
    }

    public DocumentBackedFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc)
            throws ClientException {
        this(factoryName, parentItem, doc, false);
    }

    public DocumentBackedFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) throws ClientException {
        this(factoryName, parentItem, doc, relaxSyncRootConstraint, true);
    }

    public DocumentBackedFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) throws ClientException {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
        initialize(doc);
    }

    protected DocumentBackedFolderItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) throws ClientException {
        // Update doc properties
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        doc.setPropertyValue("dc:title", name);
        doc = session.saveDocument(doc);
        session.save();
        // Update FileSystemItem attributes
        this.docTitle = name;
        this.name = name;
        updateLastModificationDate(doc);
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    @SuppressWarnings("unchecked")
    public List<FileSystemItem> getChildren() throws ClientException {
        PageProviderService pageProviderService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) getSession());
        PageProvider<DocumentModel> childrenPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                FOLDER_ITEM_CHILDREN_PAGE_PROVIDER, null, null, 0L, props, docId);
        Long pageSize = childrenPageProvider.getPageSize();

        List<FileSystemItem> children = new ArrayList<FileSystemItem>();
        int nbChildren = 0;
        boolean reachedPageSize = false;
        boolean hasNextPage = true;
        // Since query results are filtered, make sure we iterate on PageProvider to get at most its page size number of
        // FileSystemItems
        while (nbChildren < pageSize && hasNextPage) {
            List<DocumentModel> dmChildren = childrenPageProvider.getCurrentPage();
            for (DocumentModel dmChild : dmChildren) {
                // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
                FileSystemItem child = getFileSystemItemAdapterService().getFileSystemItem(dmChild, this, false,
                        false, false);
                if (child != null) {
                    children.add(child);
                    nbChildren++;
                    if (nbChildren == pageSize) {
                        reachedPageSize = true;
                        break;
                    }
                }
            }
            if (!reachedPageSize) {
                hasNextPage = childrenPageProvider.isNextPageAvailable();
                if (hasNextPage) {
                    childrenPageProvider.nextPage();
                }
            }
        }

        return children;
    }

    @Override
    public boolean getCanCreateChild() {
        return canCreateChild;
    }

    @Override
    public FolderItem createFolder(String name) throws ClientException {
        try {
            DocumentModel folder = getFileManager().createFolder(getSession(), name, docPath, false);
            if (folder == null) {
                throw new ClientException(
                        String.format(
                                "Cannot create folder named '%s' as a child of doc %s. Probably because of the allowed sub-types for this doc type, please check them.",
                                name, docPath));
            }
            return (FolderItem) getFileSystemItemAdapterService().getFileSystemItem(folder, this);
        } catch (Exception e) {
            throw new ClientException(String.format("Error while trying to create folder %s as a child of doc %s",
                    name, docPath), e);
        }
    }

    @Override
    public FileItem createFile(Blob blob) throws ClientException {
        String fileName = blob.getFilename();
        try {
            DocumentModel file = getFileManager().createDocumentFromBlob(getSession(), blob, docPath, false, fileName);
            if (file == null) {
                throw new ClientException(
                        String.format(
                                "Cannot create file '%s' as a child of doc %s. Probably because there are no file importers registered, please check the contributions to the <extension target=\"org.nuxeo.ecm.platform.filemanager.service.FileManagerService\" point=\"plugins\"> extension point.",
                                fileName, docPath));
            }
            return (FileItem) getFileSystemItemAdapterService().getFileSystemItem(file, this);
        } catch (Exception e) {
            throw new ClientException(String.format("Error while trying to create file %s as a child of doc %s",
                    fileName, docPath), e);
        }
    }

    /*--------------------- Protected -----------------*/
    protected void initialize(DocumentModel doc) throws ClientException {
        this.name = docTitle;
        this.folder = true;
        if (Framework.isBooleanPropertyTrue(PERMISSION_CHECK_OPTIMIZED_PROPERTY)) {
            // In optimized mode consider that canCreateChild <=> canRename because canRename <=> WriteProperties
            // and by default WriteProperties <=> Write <=> AddChildren
            this.canCreateChild = canRename;
        } else {
            // In non optimized mode check AddChildren
            this.canCreateChild = doc.getCoreSession().hasPermission(doc.getRef(), SecurityConstants.ADD_CHILDREN);
        }
    }

    protected FileManager getFileManager() {
        return Framework.getLocalService(FileManager.class);
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setCanCreateChild(boolean canCreateChild) {
        this.canCreateChild = canCreateChild;
    }

}
