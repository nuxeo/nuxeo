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
package org.nuxeo.drive.service.impl;

import java.security.Principal;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.api.LifeCycleConstants;
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

    private static final Log log = LogFactory.getLog(DefaultFileSystemItemFactory.class);

    protected static final String VERSIONING_DELAY_PARAM = "versioningDelay";

    protected static final String VERSIONING_OPTION_PARAM = "versioningOption";

    // Versioning delay in seconds, default value: 1 hour
    protected double versioningDelay = 3600;

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
     * <li>AND it is not in the "deleted" life cycle state, unless {@code includeDeleted} is true</li>
     * <li>AND it is Folderish or it can be adapted as a {@link BlobHolder} with a blob</li>
     * <li>AND it is not a synchronization root registered for the current user, unless {@code relaxSyncRootConstraint}
     * is true</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint) {
        // Check version
        if (doc.isVersion()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a version, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check Collections
        if (CollectionConstants.COLLECTIONS_TYPE.equals(doc.getType())) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Document %s is the collection root folder (type=%s, path=%s), it cannot be adapted as a FileSystemItem.",
                        doc.getId(), CollectionConstants.COLLECTIONS_TYPE, doc.getPathAsString()));
            }
            return false;
        }
        // Check HiddenInNavigation
        if (doc.hasFacet("HiddenInNavigation")) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is HiddenInNavigation, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check "deleted" life cycle state
        if (!includeDeleted && LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Document %s is in the '%s' life cycle state, it cannot be adapted as a FileSystemItem.",
                        doc.getId(), LifeCycleConstants.DELETED_STATE));
            }
            return false;
        }
        // Check Folderish or BlobHolder with a blob
        if (!doc.isFolder() && !hasBlob(doc)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Document %s is not Folderish nor a BlobHolder with a blob, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }

        // Check for blobs backed by extended blob providers (ex: Google Drive)
        if (!doc.isFolder()) {
            BlobManager blobManager = Framework.getService(BlobManager.class);
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            BlobProvider blobProvider = blobManager.getBlobProvider(bh.getBlob());
            if (blobProvider != null
                    && (!blobProvider.supportsUserUpdate() || blobProvider.getBinaryManager() == null)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Blob for Document %s is backed by a BlobProvider preventing updates, it cannot be adapted as a FileSystemItem.",
                            doc.getId()));
                }
                return false;
            }
        }

        if (!relaxSyncRootConstraint && doc.isFolder()) {
            // Check not a synchronization root registered for the current user
            NuxeoDriveManager nuxeoDriveManager = Framework.getLocalService(NuxeoDriveManager.class);
            Principal principal = doc.getCoreSession().getPrincipal();
            boolean isSyncRoot = nuxeoDriveManager.isSynchronizationRoot(principal, doc);
            if (isSyncRoot) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Document %s is a registered synchronization root for user %s, it cannot be adapted as a DefaultFileSystemItem.",
                            doc.getId(), principal.getName()));
                }
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
     */
    @Override
    public boolean needsVersioning(DocumentModel doc) {

        String lastContributor = (String) doc.getPropertyValue("dc:lastContributor");
        Principal principal = doc.getCoreSession().getPrincipal();
        boolean contributorChanged = !principal.getName().equals(lastContributor);
        if (contributorChanged) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Contributor %s is different from the last contributor %s => will create a version of the document.",
                        principal.getName(), lastContributor));
            }
            return true;
        }
        Calendar lastModificationDate = (Calendar) doc.getPropertyValue("dc:modified");
        if (lastModificationDate == null) {
            log.debug("Last modification date is null => will not create a version of the document.");
            return true;
        }
        long lastModified = System.currentTimeMillis() - lastModificationDate.getTimeInMillis();
        long versioningDelayMillis = (long) getVersioningDelay() * 1000;
        if (lastModified > versioningDelayMillis) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Last modification was done %d milliseconds ago, this is more than the versioning delay %d milliseconds => will create a version of the document.",
                        lastModified, versioningDelayMillis));
            }
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Contributor %s is the last contributor and last modification was done %d milliseconds ago, this is less than the versioning delay %d milliseconds => will not create a version of the document.",
                    principal.getName(), lastModified, versioningDelayMillis));
        }
        return false;
    }

    @Override
    public double getVersioningDelay() {
        return versioningDelay;
    }

    @Override
    public void setVersioningDelay(double versioningDelay) {
        this.versioningDelay = versioningDelay;
    }

    @Override
    public VersioningOption getVersioningOption() {
        return versioningOption;
    }

    @Override
    public void setVersioningOption(VersioningOption versioningOption) {
        this.versioningOption = versioningOption;
    }

    /*--------------------------- Protected ---------------------------------*/
    protected boolean hasBlob(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is not a BlobHolder.", doc.getId()));
            }
            return false;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a BlobHolder without a blob.", doc.getId()));
            }
            return false;
        }
        return true;
    }

}
