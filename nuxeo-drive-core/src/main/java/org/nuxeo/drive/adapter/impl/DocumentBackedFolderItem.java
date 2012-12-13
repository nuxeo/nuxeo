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
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} backed implementation of a {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFolderItem extends
        AbstractDocumentBackedFileSystemItem implements FolderItem {

    private static final String FOLDER_ITEM_CHILDREN_PAGE_PROVIDER = "FOLDER_ITEM_CHILDREN";

    public DocumentBackedFolderItem(String factoryName, DocumentModel doc)
            throws ClientException {
        super(factoryName, doc);
    }

    /*--------------------- AbstractFileSystemItem ---------------------*/
    @Override
    public String getName() throws ClientException {
        DocumentModel doc = getDocument(getSession());
        return (String) doc.getPropertyValue("dc:title");
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public void rename(String name) throws ClientException {
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        doc.setPropertyValue("dc:title", name);
        session.saveDocument(doc);
    }

    /*--------------------- FolderItem -----------------*/
    @Override
    @SuppressWarnings("unchecked")
    public List<FileSystemItem> getChildren() throws ClientException {
        PageProviderService pageProviderService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) getSession());
        PageProvider<DocumentModel> childrenPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                FOLDER_ITEM_CHILDREN_PAGE_PROVIDER, null, null, 0L, props,
                docId);
        List<DocumentModel> dmChildren = childrenPageProvider.getCurrentPage();

        List<FileSystemItem> children = new ArrayList<FileSystemItem>(
                dmChildren.size());
        for (DocumentModel dmChild : dmChildren) {
            FileSystemItem child = dmChild.getAdapter(FileSystemItem.class);
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    @Override
    public FolderItem createFolder(String name) throws ClientException {
        try {
            DocumentModel folder = getFileManager().createFolder(getSession(),
                    name, docPath);
            if (folder == null) {
                throw new ClientException(
                        String.format(
                                "Cannot create folder named '%s' as a child of doc %s. Probably because of the allowed sub-types for this doc type, please check them.",
                                name, docPath));
            }
            return new DocumentBackedFolderItem(getFactoryName(), folder);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    @Override
    public FileItem createFile(Blob blob) throws ClientException {
        try {
            String fileName = blob.getFilename();
            // TODO: manage conflict (overwrite should not necessarily be true)
            DocumentModel file = getFileManager().createDocumentFromBlob(
                    getSession(), blob, docPath, true, fileName);
            if (file == null) {
                throw new ClientException(
                        String.format(
                                "Cannot create file '%s' as a child of doc %s. Probably because there are no file importers registered, please check the contributions to the <extension target=\"org.nuxeo.ecm.platform.filemanager.service.FileManagerService\" point=\"plugins\"> extension point.",
                                fileName, docPath));
            }
            return new DocumentBackedFileItem(getFactoryName(), file);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    /*--------------------- Protected -----------------*/
    protected FileManager getFileManager() {
        return Framework.getLocalService(FileManager.class);
    }

}
