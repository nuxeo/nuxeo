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

import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a {@link FileSystemItemFactory}. It is {@link DocumentModel} backed and is the one used by
 * Nuxeo Drive.
 *
 * @author Antoine Taillefer
 */
public class DefaultFileSystemItemFactory extends AbstractFileSystemItemFactory
        implements VersioningFileSystemItemFactory {

    private static final Logger log = LogManager.getLogger(DefaultFileSystemItemFactory.class);

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Deprecated
    protected static final String VERSIONING_DELAY_PARAM = "versioningDelay";

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Deprecated
    protected static final String VERSIONING_OPTION_PARAM = "versioningOption";

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Deprecated
    // Versioning delay in seconds, default value: 1 hour
    protected double versioningDelay = 3600;

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Deprecated
    // Versioning option, default value: MINOR
    protected VersioningOption versioningOption = VersioningOption.MINOR;

    /*--------------------------- AbstractFileSystemItemFactory -------------------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) {
        String versioningDelayParam = parameters.get(VERSIONING_DELAY_PARAM);
        if (!StringUtils.isEmpty(versioningDelayParam)) {
            versioningDelay = Double.parseDouble(versioningDelayParam);
        }
        String versioningOptionParam = parameters.get(DefaultFileSystemItemFactory.VERSIONING_OPTION_PARAM);
        if (!StringUtils.isEmpty(versioningOptionParam)) {
            versioningOption = VersioningOption.valueOf(versioningOptionParam);
        }
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
        if (!doc.isFolder() && blob == null) {
            log.debug(
                    "Document {} is not Folderish nor a BlobHolder with a blob, it cannot be adapted as a FileSystemItem.",
                    doc::getId);
            return false;
        }

        // Check for blobs backed by extended blob providers (ex: Google Drive)
        if (!doc.isFolder()) {
            BlobManager blobManager = Framework.getService(BlobManager.class);
            BlobProvider blobProvider = blobManager.getBlobProvider(blob);
            if (blobProvider != null
                    && (!blobProvider.supportsUserUpdate() || blobProvider.getBinaryManager() == null)) {
                log.debug(
                        "Blob for Document {} is backed by a BlobProvider preventing updates, it cannot be adapted as a FileSystemItem.",
                        doc::getId);
                return false;
            }
        }

        if (!relaxSyncRootConstraint && doc.isFolder()) {
            // Check not a synchronization root registered for the current user
            NuxeoDriveManager nuxeoDriveManager = Framework.getService(NuxeoDriveManager.class);
            NuxeoPrincipal principal = doc.getCoreSession().getPrincipal();
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

    /*--------------------------- FileSystemItemVersioning -------------------------*/
    /**
     * Need to version the doc if the current contributor is different from the last contributor or if the last
     * modification was done more than {@link #versioningDelay} seconds ago.
     *
     * @deprecated since 9.1 versioning policy is now handled at versioning service level, as versioning is removed at
     *             drive level, this method is not used anymore
     */
    @Override
    @Deprecated
    public boolean needsVersioning(DocumentModel doc) {

        String lastContributor = (String) doc.getPropertyValue("dc:lastContributor");
        NuxeoPrincipal principal = doc.getCoreSession().getPrincipal();
        boolean contributorChanged = !principal.getName().equals(lastContributor);
        if (contributorChanged) {
            log.debug(
                    "Contributor {} is different from the last contributor {} => will create a version of the document.",
                    principal, lastContributor);
            return true;
        }
        Calendar lastModificationDate = (Calendar) doc.getPropertyValue("dc:modified");
        if (lastModificationDate == null) {
            log.debug("Last modification date is null => will create a version of the document.");
            return true;
        }
        long lastModified = System.currentTimeMillis() - lastModificationDate.getTimeInMillis();
        long versioningDelayMillis = (long) getVersioningDelay() * 1000;
        if (lastModified > versioningDelayMillis) {
            log.debug(
                    "Last modification was done {} milliseconds ago, this is more than the versioning delay {} milliseconds => will create a version of the document.",
                    lastModified, versioningDelayMillis);
            return true;
        }
        log.debug(
                "Contributor {} is the last contributor and last modification was done {} milliseconds ago, this is less than the versioning delay {} milliseconds => will not create a version of the document.",
                principal, lastModified, versioningDelayMillis);
        return false;
    }

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Override
    @Deprecated
    public double getVersioningDelay() {
        return versioningDelay;
    }

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Override
    @Deprecated
    public void setVersioningDelay(double versioningDelay) {
        this.versioningDelay = versioningDelay;
    }

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Override
    @Deprecated
    public VersioningOption getVersioningOption() {
        return versioningOption;
    }

    /**
     * @deprecated since 9.1 automatic versioning is directly done by versioning system which holds the policies
     */
    @Override
    @Deprecated
    public void setVersioningOption(VersioningOption versioningOption) {
        this.versioningOption = versioningOption;
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
