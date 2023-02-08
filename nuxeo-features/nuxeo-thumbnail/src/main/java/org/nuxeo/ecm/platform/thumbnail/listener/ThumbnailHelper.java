/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 * Nelson Silva <nsilva@nuxeo.com>
 * bdelbosc
 */
package org.nuxeo.ecm.platform.thumbnail.listener;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTOMATIC_VERSIONING;
import static org.nuxeo.ecm.platform.thumbnail.listener.UpdateThumbnailListener.THUMBNAIL_UPDATED;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Helper to create document thumbnail if necessary.
 *
 * @since 11.5
 */
public class ThumbnailHelper {

    private static final Logger log = LogManager.getLogger(ThumbnailHelper.class);

    public static final String THUMBNAIL_TX_TIMEOUT_PROPERTY = "nuxeo.thumbnail.transaction.timeout.seconds";

    public static final int DEFAULT_TX_TIMEOUT_SECONDS = 300;

    protected Integer transactionTimeout;

    /**
     * Creates a thumbnail if needed and synchronizes the document facet.
     */
    public void createThumbnailIfNeeded(CoreSession session, DocumentModel doc) {
        Blob thumbnailBlob = getManagedThumbnail(doc);
        if (thumbnailBlob != null) {
            log.debug("Found a managed thumbnail for doc: {}", doc::getId);
        } else {
            ThumbnailAdapter thumbnailAdapter = doc.getAdapter(ThumbnailAdapter.class);
            if (thumbnailAdapter == null) {
                log.debug("No thumbnail adapter for doc: {}, skipping", doc::getId);
                return;
            }
            log.debug("Fetching or building a thumbnail for doc: {}, session ttl: {}s", doc::getId,
                    TransactionHelper::getTransactionTimeToLive);
            thumbnailBlob = thumbnailAdapter.computeThumbnail(session);
        }
        boolean forceUpdate = false;
        if (thumbnailBlob == null) {
            log.debug("Failed to create thumbnail for doc: {}", doc::getId);
            if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.removeFacet(ThumbnailConstants.THUMBNAIL_FACET);
                log.debug("Removing thumbnail facet from doc: {}", doc::getId);
            }
            forceUpdate = true;
        } else {
            if (!doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
                log.debug("Adding thumbnail facet to doc: {}", doc::getId);
            }
            doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, (Serializable) thumbnailBlob);
        }
        if (forceUpdate || doc.isDirty()) {
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
            doc.putContextData(DISABLE_AUTOMATIC_VERSIONING, Boolean.TRUE);
            doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
            doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, Boolean.TRUE);
            doc.putContextData(CoreSession.DISABLE_AUDIT_LOGGER, Boolean.TRUE);
            if (doc.isVersion()) {
                doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
            }
            doc.putContextData(THUMBNAIL_UPDATED, true);
            session.saveDocument(doc);
            log.debug("Thumbnail updated for doc: {}", doc::getId);
        }
    }

    /**
     * Commits and starts a new transaction with a custom timeout.
     */
    public void newTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        log.debug("Commit and start transaction with timeout {}s", this::getTransactionTimeout);
        TransactionHelper.startTransaction(getTransactionTimeout());
        // timeout of command line executions will be aligned with the transaction timeout
    }

    public int getTransactionTimeout() {
        if (transactionTimeout == null) {
            String maxDurationStr = Framework.getProperty(THUMBNAIL_TX_TIMEOUT_PROPERTY,
                    String.valueOf(DEFAULT_TX_TIMEOUT_SECONDS));
            transactionTimeout = Integer.parseInt(maxDurationStr);
        }
        return transactionTimeout;
    }

    protected Blob getManagedThumbnail(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            return null;
        }
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            InputStream is = blobManager.getThumbnail(blob);
            if (is == null) {
                return null;
            }
            return Blobs.createBlob(is);
        } catch (IOException e) {
            throw new NuxeoException("Failed to get managed blob thumbnail", e);
        }
    }
}
