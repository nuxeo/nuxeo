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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DownloadBlobGuard;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action to update the file:content/length property using the value in the binary store.
 *
 * @since 2021.8
 */
public class S3SetContentLengthAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "s3SetContentLength";

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    public static final String DISABLE_DUBLINCORE_LISTENER = "disableDublinCoreListener";

    public static final String DISABLE_NOTIFICATION_SERVICE = "disableNotificationService";

    public static final String DISABLE_THUMBNAIL_SERVICE = "disableThumbnailComputation";

    public static final String DISABLE_AUTO_CHECKOUT = "DisableAutoCheckOut";

    public static final String DISABLE_DOMAIN_LISTENER = "disableDomainListener";

    public static final String DISABLE_HTMLSANITIZER_LISTENER = "disableHtmlSanitizerListener";

    public static final String DISABLE_AUTO_INDEXING = "disableAutoIndexing";

    private static final String FORCE_OPTION = "force";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(SetContentLengthComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class SetContentLengthComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(SetContentLengthComputation.class);

        public SetContentLengthComputation() {
            super(ACTION_NAME);
        }

        protected String currentProvider;

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DownloadBlobGuard.enable(); // Avoid downloading main content blob during the transaction
            boolean force = properties.containsKey(FORCE_OPTION)
                    && Boolean.parseBoolean(properties.get(FORCE_OPTION).toString());
            for (DocumentModel doc : loadDocuments(session, ids)) {
                updateContentLength(session, doc, force);
            }
        }

        protected void updateContentLength(CoreSession session, DocumentModel doc, boolean force) {
            BlobInfo blobInfo = getBlobInfo(doc);
            if (blobInfo == null) {
                log.debug("Skipping {}: no blob", doc);
                return;
            }
            if (blobInfo.length >= 0 && !force) {
                log.debug("Skipping {}, keeping existing length: {}", doc, blobInfo.length);
                return;
            }
            long length = getContentLength(doc, blobInfo);
            if (length == blobInfo.length) {
                log.debug("Skipping {}, length is correct {}", doc, length);
                return;
            }
            disableListeners(doc);
            try {
                log.info("Updating {}, length from {} to {}", doc, blobInfo.length, length);
                blobInfo.length = length;
                Blob blob = new SimpleManagedBlob(currentProvider, blobInfo);
                doc.setProperty("file", "content", blob);
                session.saveDocument(doc);
            } catch (PropertyException e) {
                log.warn("Cannot set content/length: {} of document: {}", length, doc.getId(), e);
            }
        }

        protected void disableListeners(DocumentModel doc) {
            doc.putContextData(PARAM_DISABLE_AUDIT, Boolean.TRUE);
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
            doc.putContextData(DISABLE_QUOTA_CHECK_LISTENER, Boolean.TRUE);
            doc.putContextData(DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
            doc.putContextData(DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
            doc.putContextData(DISABLE_NOTIFICATION_SERVICE, Boolean.TRUE);
            doc.putContextData(DISABLE_THUMBNAIL_SERVICE, Boolean.TRUE);
            doc.putContextData(DISABLE_DOMAIN_LISTENER, Boolean.TRUE);
            doc.putContextData(DISABLE_HTMLSANITIZER_LISTENER, Boolean.TRUE);
            doc.putContextData(DISABLE_AUTO_INDEXING, Boolean.TRUE);
        }

        protected BlobInfo getBlobInfo(DocumentModel doc) {
            BlobInfo ret = new BlobInfo();
            ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
            if (blob == null) {
                currentProvider = null;
                return null;
            }
            ret.length = blob.getLength(); // cannot be null, unknown length will be -1
            ret.digest = blob.getDigest();
            ret.encoding = blob.getEncoding();
            ret.filename = blob.getFilename();
            ret.mimeType = blob.getMimeType();
            currentProvider = blob.getProviderId();
            ret.key = currentProvider + ":" + blob.getKey();
            return ret;
        }

        protected long getContentLength(DocumentModel doc, BlobInfo blobInfo) {
            long length = blobInfo.length != null ? blobInfo.length : -1;
            BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(currentProvider);
            if (blobProvider == null) {
                log.error("Blob provider {} not found on doc {}", currentProvider, doc);
            } else if (blobProvider instanceof S3BinaryManager) {
                S3BinaryManager sourceBlobProvider = (S3BinaryManager) blobProvider;
                log.debug("Fetching length from s3");
                length = sourceBlobProvider.lengthOfBlob(blobInfo.digest);
            } else {
                log.debug("Not supported binary manager impl");
            }
            log.debug("Get length of {} previously {} for doc {}", length, blobInfo.length, doc.getId());
            return length;
        }
    }
}
