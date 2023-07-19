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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a {@link FileSystemItemFactory}. It is {@link DocumentModel} backed and is the one used by
 * Nuxeo Drive.
 *
 * @author Antoine Taillefer
 */
public class DefaultFileSystemItemFactory extends AbstractFileSystemItemFactory implements FileSystemItemFactory {

    private static final Logger log = LogManager.getLogger(DefaultFileSystemItemFactory.class);

    /*--------------------------- AbstractFileSystemItemFactory -------------------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) {
    }

    /**
     * The default factory considers that a {@link DocumentModel} is adaptable as a {@link FileSystemItem} if:
     * <ul>
     * <li>It is not a version</li>
     * <li>AND it is not HiddenInNavigation</li>
     * <li>AND it is not in the trash, unless {@code includeDeleted} is true</li>
     * <li>AND it is Folderish or it can be adapted as a {@link BlobHolder} with a blob</li>
     * <li>AND its blob is not backed by an extended blob provider</li>
     * <li>AND it is not a synchronization root registered for the current user, unless {@code relaxSyncRootConstraint}
     * is true</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint) {
        // Check version
        if (doc.isVersion()) {
            log.debug("Document {} is a version, it cannot be adapted as a FileSystemItem.", doc::getId);
            return false;
        }
        // Check Collections
        if (CollectionConstants.COLLECTIONS_TYPE.equals(doc.getType())) {
            log.debug(
                    "Document {} is the collection root folder (type={}, path={}), it cannot be adapted as a FileSystemItem.",
                    doc::getId, () -> CollectionConstants.COLLECTIONS_TYPE, doc::getPathAsString);
            return false;
        }
        // Check HiddenInNavigation
        if (doc.hasFacet("HiddenInNavigation")) {
            log.debug("Document {} is HiddenInNavigation, it cannot be adapted as a FileSystemItem.", doc::getId);
            return false;
        }
        // Check if document is in the trash
        if (!includeDeleted && doc.isTrashed()) {
            log.debug("Document {} is trashed, it cannot be adapted as a FileSystemItem.", doc::getId);
            return false;
        }
        // Try to fetch blob
        Blob blob = null;
        try {
            blob = getBlob(doc);
        } catch (NuxeoException e) {
            log.error("Error while fetching blob for document {}, it cannot be adapted as a FileSystemItem.",
                    doc::getId, () -> e);
            return false;
        }

        // Check Folderish or BlobHolder with a blob
        if (!doc.isFolder()) {
            if (blob == null) {
                log.debug(
                        "Document {} is not Folderish nor a BlobHolder with a blob, it cannot be adapted as a FileSystemItem.",
                        doc::getId);
                return false;
            }

            // Check for blobs backed by extended blob providers (ex: Google Drive)
            BlobManager blobManager = Framework.getService(BlobManager.class);
            BlobProvider blobProvider = blobManager.getBlobProvider(blob);
            if (blobProvider != null && !blobProvider.supportsSync()) {
                log.debug(
                        "Blob for Document {} is backed by a BlobProvider preventing sync, it cannot be adapted as a FileSystemItem.",
                        doc::getId);
                return false;
            } else if (blobProvider == null
                    && !Framework.getService(SchemaManager.class).hasSuperType(doc.getType(), "Note")) {
                log.debug(
                        "Document {} has no BlobProvider and is not a Note, it cannot be adapted as a FileSystemItem.",
                        doc::getId);
                return false;
            }
        }

        if (!relaxSyncRootConstraint && doc.isFolder()) {
            // Check not a synchronization root registered for the current user
            NuxeoDriveManager nuxeoDriveManager = Framework.getService(NuxeoDriveManager.class);
            NuxeoPrincipal principal = doc.getPrincipal();
            boolean isSyncRoot = nuxeoDriveManager.isSynchronizationRoot(principal, doc);
            if (isSyncRoot) {
                log.debug(
                        "Document {} is a registered synchronization root for user {}, it cannot be adapted as a DefaultFileSystemItem.",
                        doc::getId, principal::getName);
                return false;
            }
        }
        return true;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        // Doc is either Folderish
        if (doc.isFolder()) {
            if (forceParentItem) {
                return new DocumentBackedFolderItem(name, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
            } else {
                return new DocumentBackedFolderItem(name, doc, relaxSyncRootConstraint, getLockInfo);
            }
        }
        // or a BlobHolder with a blob
        else {
            if (forceParentItem) {
                return new DocumentBackedFileItem(this, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
            } else {
                return new DocumentBackedFileItem(this, doc, relaxSyncRootConstraint, getLockInfo);
            }
        }
    }

    /*--------------------------- Protected ---------------------------------*/
    protected Blob getBlob(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            log.debug("Document {} is not a BlobHolder.", doc::getId);
            return null;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            log.debug("Document {} is a BlobHolder without a blob.", doc::getId);
        }
        return blob;
    }

}
