/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.core.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.scroll.AbstractBlobScroll;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Removes the orphan document blobs.
 *
 * @since 2023
 */
public class GarbageCollectOrphanBlobsAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(GarbageCollectOrphanBlobsAction.class);

    public static final String DRY_RUN_PARAM = "dryRun";

    public static final String RESULT_DELETED_SIZE_KEY = "deletedSize";

    public static final String RESULT_TOTAL_SIZE_KEY = "totalSize";

    public static final String ACTION_NAME = "garbageCollectOrphanBlobs";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(GarbageCollectOrphanBlobsComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class GarbageCollectOrphanBlobsComputation extends AbstractBulkComputation {

        protected boolean dryRun;

        public GarbageCollectOrphanBlobsComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            Serializable dryRunParamValue = command.getParam(DRY_RUN_PARAM);
            dryRun = dryRunParamValue != null && Boolean.parseBoolean(dryRunParamValue.toString());
        }

        @Override
        protected void compute(CoreSession session, List<String> blobKeys, Map<String, Serializable> properties) {
            DocumentBlobManager documentBlobManager = Framework.getService(DocumentBlobManager.class);
            String repository = session.getRepositoryName();
            long deletedSize = 0L;
            long totalSize = 0L;
            for (String k : blobKeys) {
                String key = AbstractBlobScroll.getBlobKey(k);
                Long size = AbstractBlobScroll.getBlobSize(k);
                totalSize += size;
                try {
                    boolean deleted = documentBlobManager.deleteBlob(repository, key, dryRun);
                    if (deleted) {
                        deletedSize += size;
                    } else {
                        delta.incrementSkipCount();
                    }
                    log.trace("CommandId: {} Blob: {} of size: {} from repository: {} deleted: {} dryRun: {}",
                            () -> getCurrentCommand().getId(), () -> key, () -> FileUtils.byteCountToDisplaySize(size),
                            () -> repository, () -> deleted, () -> dryRun);
                } catch (IllegalArgumentException e) {
                    delta.inError(String.format("Cannot delete blob: %s, repository: %s, with error: %s", key,
                            repository, e.getMessage()));
                    log.warn("Cannot delete blob: {}, repository: {}, with error: {}", key, repository, e);
                } catch (IOException e) {
                    // Worth trying again on IOException, could be network or service disruption.
                    throw new NuxeoException(e);
                }
            }
            delta.mergeResult(Map.of(RESULT_DELETED_SIZE_KEY, deletedSize, RESULT_TOTAL_SIZE_KEY, totalSize));
        }

    }

}
