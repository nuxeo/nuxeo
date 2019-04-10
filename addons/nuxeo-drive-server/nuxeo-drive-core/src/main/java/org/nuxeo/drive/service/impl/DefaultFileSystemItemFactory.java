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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a {@link FileSystemItemFactory}. It is {@link DocumentModel} backed and is the one used by
 * Nuxeo Drive.
 *
 * @author Antoine Taillefer
 */
public class DefaultFileSystemItemFactory extends AbstractFileSystemItemFactory implements
        VersioningFileSystemItemFactory {

    private static final Log log = LogFactory.getLog(DefaultFileSystemItemFactory.class);

    protected static final String RENDITION_FACET = "Rendition";

    protected static final String VERSIONING_DELAY_PARAM = "versioningDelay";

    protected static final String VERSIONING_OPTION_PARAM = "versioningOption";

    // Versioning delay in seconds, default value: 1 hour
    protected double versioningDelay = 3600;

    // Versioning option, default value: MINOR
    protected VersioningOption versioningOption = VersioningOption.MINOR;

    /*--------------------------- AbstractFileSystemItemFactory -------------------------*/
    @Override
    public void handleParameters(Map<String, String> parameters) throws ClientException {
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
     * <li>It is not a version nor a proxy nor a rendition</li>
     * <li>AND it is not HiddenInNavigation</li>
     * <li>AND it is not in the "deleted" life cycle state, unless {@code includeDeleted} is true</li>
     * <li>AND it is Folderish or it can be adapted as a {@link BlobHolder} with a blob</li>
     * <li>AND it is not a synchronization root registered for the current user, unless {@code relaxSyncRootConstraint}
     * is true</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {
        // Check version
        if (doc.isVersion()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a version, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check proxy
        if (doc.isProxy()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a proxy, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check rendition
        if (doc.hasFacet(RENDITION_FACET)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a rendition, it cannot be adapted as a FileSystemItem.",
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
            boolean relaxSyncRootConstraint) throws ClientException {
        // Doc is either Folderish
        if (doc.isFolder()) {
            if (forceParentItem) {
                return new DocumentBackedFolderItem(name, parentItem, doc, relaxSyncRootConstraint);
            } else {
                return new DocumentBackedFolderItem(name, doc, relaxSyncRootConstraint);
            }
        }
        // or a BlobHolder with a blob
        else {
            if (forceParentItem) {
                return new DocumentBackedFileItem(this, parentItem, doc, relaxSyncRootConstraint);
            } else {
                return new DocumentBackedFileItem(this, doc, relaxSyncRootConstraint);
            }
        }
    }

    /*--------------------------- FileSystemItemVersioning -------------------------*/
    /**
     * Need to version the doc if the current contributor is different from the last contributor or if the last
     * modification was done more than {@link #versioningDelay} seconds ago.
     */
    @Override
    public boolean needsVersioning(DocumentModel doc) throws ClientException {

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
    protected boolean hasBlob(DocumentModel doc) throws ClientException {
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
