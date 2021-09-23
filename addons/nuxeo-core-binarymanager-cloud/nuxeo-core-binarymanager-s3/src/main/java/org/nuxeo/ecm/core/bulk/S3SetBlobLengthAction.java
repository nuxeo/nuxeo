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
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DownloadBlobGuard;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action to set blob length property using the length provided by the s3 binary store.
 *
 * @since 2021.9
 */
public class S3SetBlobLengthAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "s3SetBlobLength";

    protected static final String FORCE_OPTION = "force";

    protected static final String XPATH_FILTER_OPTION = "xpath";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(SetBlobLengthComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class SetBlobLengthComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(SetBlobLengthComputation.class);

        public SetBlobLengthComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DownloadBlobGuard.enable(); // Avoid downloading blob without length during the transaction
            String xpath = (String) properties.get(XPATH_FILTER_OPTION);
            boolean force = properties.containsKey(FORCE_OPTION)
                    && Boolean.parseBoolean(properties.get(FORCE_OPTION).toString());
            for (String id : ids) {
                fixBlobLength((AbstractSession) session, id, xpath, force);
            }
        }

        protected void fixBlobLength(AbstractSession session, String docId, String xpath, boolean force) {
            try {
                Document doc = session.getSession().getDocumentByUUID(docId);
                if (doc == null) {
                    log.debug("Skipping doc: {} with null workingCopy.", docId);
                    return;
                }
                doc.visitBlobs(accessor -> {
                    if (xpath != null && !xpath.equals(accessor.getXPath())) {
                        log.debug("Skipping blob xpath: {} for doc: {} because it doesn't match the xpath filter",
                                accessor.getXPath(), docId);
                        return;
                    }
                    Blob blob = accessor.getBlob();
                    if (blob instanceof BinaryBlob) {
                        Blob fixedBlob = fixBlob(doc.getUUID(), (BinaryBlob) blob, force);
                        if (fixedBlob != null) {
                            accessor.setBlob(fixedBlob);
                        }
                    }
                });
            } catch (DocumentNotFoundException e) {
                log.debug("Skipping deleted doc: {}.", docId);
            } catch (PropertyException e) {
                log.warn("Cannot access blobs for doc: " + docId, e);
            }
        }

        protected Blob fixBlob(String docId, BinaryBlob blob, boolean force) {
            if (blob == null || blob.getKey() == null) {
                log.debug("Skipping null blob for doc: {}", docId);
                return null;
            }
            String blobKey = blob.getKey();
            long length = blob.getLength();
            if (length >= 0 && !force) {
                log.debug("Skipping blob: {} for doc: {}, keeping existing length: {}.", blobKey, docId, length);
                return null;
            }
            long fixedLength = getBlobLengthFromS3(docId, blob);
            if (length == fixedLength) {
                log.debug("Skipping blob: {} for doc: {}, length is correct {}", blobKey, docId, length);
                return null;
            }
            if (fixedLength < 0) {
                log.warn("Skipping blob: {} for doc: {}, binaryManager length: {}, keeping current value: {}.", blobKey,
                        docId, fixedLength, length);
                return null;
            }
            log.info("Fixing length blob: {} for doc: {}, from: {} to: {}.", blob.getKey(), docId, length, fixedLength);
            return new BinaryBlob(blob.getBinary(), blob.getKey(), blob.getFilename(), blob.getMimeType(),
                    blob.getEncoding(), blob.getDigestAlgorithm(), blob.getDigest(), fixedLength);
        }

        protected long getBlobLengthFromS3(String docId, Blob blob) {
            long length = blob.getLength();
            BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob);
            if (blobProvider == null) {
                log.error("Blob provider not found for doc: {}, blob: {}", docId, blob.getDigest());
            } else if (blobProvider instanceof S3BinaryManager) {
                S3BinaryManager sourceBlobProvider = (S3BinaryManager) blobProvider;
                log.debug("Fetching length from s3");
                length = sourceBlobProvider.lengthOfBlob(blob.getDigest());
            } else {
                log.debug("Not supported binary manager impl");
            }
            log.debug("Get length: {} previously: {} for doc: {}, blob: {}", length, blob.getLength(), docId,
                    blob.getDigest());
            return length;
        }
    }
}
