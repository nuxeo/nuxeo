/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a collection synchronization root {@link FolderItem}.
 *
 * @author Antoine Taillefer
 * @since 6.0
 */
public class CollectionSyncRootFolderItem extends DefaultSyncRootFolderItem {

    private static final long serialVersionUID = 1L;

    public CollectionSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc) {
        this(factoryName, parentItem, doc, false);
    }

    public CollectionSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        this(factoryName, parentItem, doc, relaxSyncRootConstraint, true);
    }

    public CollectionSyncRootFolderItem(String factoryName, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        super(factoryName, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
        canScrollDescendants = false;
    }

    protected CollectionSyncRootFolderItem() {
        // Needed for JSON deserialization
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileSystemItem> getChildren() {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
            PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
            Map<String, Serializable> props = new HashMap<String, Serializable>();
            props.put(CORE_SESSION_PROPERTY, (Serializable) session);
            PageProvider<DocumentModel> childrenPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                    CollectionConstants.COLLECTION_CONTENT_PAGE_PROVIDER, null, null, 0L, props, docId);
            List<DocumentModel> dmChildren = childrenPageProvider.getCurrentPage();

            List<FileSystemItem> children = new ArrayList<FileSystemItem>(dmChildren.size());
            for (DocumentModel dmChild : dmChildren) {
                // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
                FileSystemItem child = getFileSystemItemAdapterService().getFileSystemItem(dmChild, this, false, false,
                        false);
                if (child != null) {
                    children.add(child);
                }
            }
            return children;
        }
    }

    @Override
    public ScrollFileSystemItemList scrollDescendants(String scrollId, int batchSize, long keepAlive) {
        throw new UnsupportedOperationException(
                "Cannot scroll through the descendants of a collection sync root folder item, please call getChildren() instead.");
    }

    @Override
    public FolderItem createFolder(String name, boolean overwrite) {
        throw new UnsupportedOperationException("Cannot create a folder in a collection synchronization root.");
    }

    @Override
    public FileItem createFile(Blob blob, boolean overwrite) {
        throw new UnsupportedOperationException("Cannot create a file in a collection synchronization root.");
    }

    @Override
    protected final void initialize(DocumentModel doc) {
        super.initialize(doc);
        // Cannot create a document in a collection sync root (could be
        // implemented as adding it to the collection if only we new the doc
        // path).
        this.canCreateChild = false;
    }

}
