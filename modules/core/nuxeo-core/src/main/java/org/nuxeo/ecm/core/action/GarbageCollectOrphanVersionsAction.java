/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Removes the orphan versions.
 * <p>
 *
 * @since 2023
 */
public class GarbageCollectOrphanVersionsAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(GarbageCollectOrphanVersionsAction.class);

    public static final String ACTION_NAME = "garbageCollectOrphanVersions";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(GarbageCollectOrphanVersionsComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class GarbageCollectOrphanVersionsComputation extends AbstractBulkComputation {

        protected static final int CACHE_SIZE = 1000;

        /**
         * Cache already deleted
         */
        protected final CircularFifoQueue<String> deletedVersions;

        protected final CircularFifoQueue<String> versionSeriesIds;

        protected String currentCommand;

        public GarbageCollectOrphanVersionsComputation() {
            super(ACTION_FULL_NAME);
            deletedVersions = new CircularFifoQueue<>(CACHE_SIZE);
            versionSeriesIds = new CircularFifoQueue<>(CACHE_SIZE);
        }

        @Override public void startBucket(String bucketKey) {
            super.startBucket(bucketKey);
            if (getCurrentCommand().getId().equals(currentCommand)) {
                deletedVersions.clear();
                versionSeriesIds.clear();
                currentCommand = getCurrentCommand().getId();
            }
        }

        @Override protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            for (DocumentModel doc : loadDocuments(session, ids)) {
                if (!doc.isVersion()) {
                    log.debug("Document: {} is not a version", doc::getId);
                    delta.incrementSkipCount();
                    continue;
                }
                if (deletedVersions.contains(doc.getId())) {
                    log.debug("Version: {} already deleted", doc::getId);
                    continue;
                }
                String versionSeries = doc.getVersionSeriesId();
                if (versionSeriesIds.contains(versionSeries)) {
                    log.debug("Version: {} already check on series: {}", doc::getId, doc::getVersionSeriesId);
                    delta.incrementSkipCount();
                    continue;
                }
                List<DocumentRef> deleted = session.removeOrphanVersions(new IdRef(versionSeries));
                if (deleted.isEmpty()) {
                    log.debug("Version: {} cannot be removed", doc::getId);
                    delta.incrementSkipCount();
                } else {
                    log.debug("Orphan removed for Doc {}: {}", versionSeries, deleted);
                    deleted.forEach(ref -> deletedVersions.add((String) ref.reference()));
                }
            }
        }

    }
}
