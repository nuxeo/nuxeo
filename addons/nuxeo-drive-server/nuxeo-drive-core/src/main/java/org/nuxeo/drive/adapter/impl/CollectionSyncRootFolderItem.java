/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a collection synchronization root
 * {@link FolderItem}.
 *
 * @author Antoine Taillefer
 * @since 6.0
 */
public class CollectionSyncRootFolderItem extends DefaultSyncRootFolderItem
        implements FolderItem {

    private static final long serialVersionUID = 1L;

    public CollectionSyncRootFolderItem(String factoryName,
            FolderItem parentItem, DocumentModel doc) throws ClientException {
        super(factoryName, parentItem, doc);
    }

    protected CollectionSyncRootFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileSystemItem> getChildren() throws ClientException {

        PageProviderService pageProviderService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) getSession());
        PageProvider<DocumentModel> childrenPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                CollectionConstants.COLLECTION_CONTENT_PAGE_PROVIDER, null,
                null, 0L, props, docId);
        List<DocumentModel> dmChildren = childrenPageProvider.getCurrentPage();

        List<FileSystemItem> children = new ArrayList<FileSystemItem>(
                dmChildren.size());
        for (DocumentModel dmChild : dmChildren) {
            FileSystemItem child = getFileSystemItemAdapterService().getFileSystemItem(
                    dmChild, this);
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    @Override
    public FolderItem createFolder(String name) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a folder in a collection synchronization root.");
    }

    @Override
    public FileItem createFile(Blob blob) throws ClientException {
        throw new UnsupportedOperationException(
                "Cannot create a file in a collection synchronization root.");
    }

    @Override
    protected final void initialize(DocumentModel doc) throws ClientException {
        super.initialize(doc);
        // Cannot create a document in a collection sync root (could be
        // implemented as adding it to the collection if only we new the doc
        // path).
        this.canCreateChild = false;
    }

}
