/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.core.storage.action;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action to fix the binary fulltext storage when switching storage from repository to blob using
 * nuxeo.fulltext.storedInBlob=true on an existing instance.
 * <p>
 * This action is copying existing fulltext from the repository to blobs without extracting the binary fulltext or
 * triggering any events or reindexing.
 * <p>
 * When a storedInBlobThreshold is configured, it also moves blob-stored fulltext back inline if it is below the
 * threshold (reverse migration).
 *
 * @since 2023.27
 */
public class FixBinaryFulltextStorageAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "fixBinaryFulltextStorage";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(FixBinaryFulltextStorageComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class FixBinaryFulltextStorageComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(FixBinaryFulltextStorageComputation.class);

        public FixBinaryFulltextStorageComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            Session lowSession = ((AbstractSession) session).getSession();
            var threshold = lowSession.getFulltextStoredInBlobThreshold();
            int updated = 0;
            for (String id : ids) {
                Document doc = lowSession.getDocumentByUUID(id);
                if (doc.isProxy()) {
                    delta.incrementSkipCount();
                    continue;
                }
                String fulltext = (String) doc.getPropertyValue("ecm:fulltextBinary");
                if (fulltext == null) {
                    delta.incrementSkipCount();
                    continue;
                }
                boolean isBlobKey = AbstractSession.isFulltextValueABlobKey(fulltext);
                if (!isBlobKey) {
                    // inline text that may need to be moved to blob (respects threshold)
                    int fulltextLength = fulltext.getBytes(UTF_8).length;
                    if (threshold.toBytes() > 0 && fulltextLength < threshold.toBytes()) {
                        // already inline and below threshold: no storage change needed
                        delta.incrementSkipCount();
                        continue;
                    }
                    updated++;
                    doc.setSystemProp("fulltextBinary", fulltext);
                    String key = (String) doc.getPropertyValue("ecm:fulltextBinary");
                    boolean resultIsBlobKey = key != null && AbstractSession.isFulltextValueABlobKey(key);
                    log.warn("Fix fulltext storage of: {}, isBlobKey: {}, size: {}", id, resultIsBlobKey,
                            new ByteSize(fulltextLength));
                } else if (threshold.toBytes() > 0) {
                    try {
                        // blob key that may need to be moved back inline if below threshold
                        Map<String, String> ftMap = lowSession.getBinaryFulltext(id);
                        String binaryFulltext = ftMap.get("binarytext");
                        int fulltextLength = binaryFulltext.getBytes(UTF_8).length;
                        if (fulltextLength < threshold.toBytes()) {
                            updated++;
                            doc.setSystemProp("fulltextBinary", binaryFulltext);
                            log.warn("Move fulltext of: {}, back to inline, size: {}", id,
                                    new ByteSize(fulltextLength));
                        } else {
                            delta.incrementSkipCount();
                        }
                    } catch (PropertyException e) {
                        log.warn("Cannot read binary fulltext for document: {}", id, e);
                        delta.incrementSkipCount();
                    }
                } else {
                    delta.incrementSkipCount();
                }
            }
            if (updated > 0) {
                try {
                    lowSession.save();
                } catch (PropertyException e) {
                    // corrupted docs
                    log.warn("Cannot save session", e);
                }
            }
        }
    }
}
