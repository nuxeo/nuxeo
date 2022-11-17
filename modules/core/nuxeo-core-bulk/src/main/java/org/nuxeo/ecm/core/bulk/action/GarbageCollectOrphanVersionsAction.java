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

package org.nuxeo.ecm.core.bulk.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Removes the orphan versions.
 * <p>
 * A version stays referenced, and therefore is not removed, if any proxy points to a version in the version history of
 * any live document.
 * <p>
 * Warning: this implementation does NOT take into account contribution made to the orphanVersionRemovalFilter extension
 * point.
 *
 * @since 2023
 */
public class GarbageCollectOrphanVersionsAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(GarbageCollectOrphanVersionsAction.class);

    public static final String ACTION_NAME = "garbageCollectOrphanVersions";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String PROXY_DOC_QUERY_TEMPLATE = "SELECT " + NXQL.ECM_UUID + " FROM Document WHERE "
            + NXQL.ECM_ISPROXY + " = 1 AND " + NXQL.ECM_PROXY_VERSIONABLEID + " = '%s'";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(GarbageCollectOrphanVersionsComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class GarbageCollectOrphanVersionsComputation extends AbstractBulkComputation {

        /**
         * Cache that keeps the ids of the live doc for which we can remove associated versions.
         */
        protected final CircularFifoBuffer canBeRemovedWithVersionsSeriesIds;

        /**
         * Cache that keeps the ids of the live doc for which we cannot remove associated versions.
         */
        protected final CircularFifoBuffer cannotBeRemovedWithVersionsSeriesIds;

        protected static final int cacheSize = 1000;

        protected String lastCommandId;

        public GarbageCollectOrphanVersionsComputation() {
            super(ACTION_FULL_NAME);
            canBeRemovedWithVersionsSeriesIds = new CircularFifoBuffer(cacheSize);
            cannotBeRemovedWithVersionsSeriesIds = new CircularFifoBuffer(cacheSize);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            for (DocumentModel version : loadDocuments(session, ids)) {
                if (!version.isVersion()) {
                    throw new IllegalArgumentException(String.format("Document %s is not a version", version.getId()));
                }
                if (canRemove(session, version)) {
                    try {
                        log.debug("Remove orphan version: {}", version::getRef);
                        session.removeDocument(version.getRef());
                    } catch (DocumentNotFoundException e) {
                        log.trace("Remove orphan version: {} already deleted", version::getRef);
                    }
                }
            }
        }

        protected boolean canRemove(CoreSession session, DocumentModel version) {
            String versionSeriesId = version.getVersionSeriesId();
            log.debug("Checking if version {} is orphan of live document {}", version.getRef(), versionSeriesId);
            if (canBeRemovedWithVersionsSeriesIds.contains(versionSeriesId)) {
                log.debug("With versionSeriesId {} is cached as can be removed", versionSeriesId);
                return true;
            }
            if (cannotBeRemovedWithVersionsSeriesIds.contains(versionSeriesId)) {
                log.debug("With versionSeriesId {} is cached as cannot be removed", versionSeriesId);
                return false;
            }
            if (session.exists(new IdRef(versionSeriesId))) {
                log.debug("Keep version: {} because a live document exists: {}", version.getRef(), versionSeriesId);
                cannotBeRemovedWithVersionsSeriesIds.add(versionSeriesId);
                return false;
            }
            // find the version series for proxies
            String findProxies = String.format(PROXY_DOC_QUERY_TEMPLATE, versionSeriesId);
            PartialList<Map<String, Serializable>> res = session.queryProjection(findProxies, 1, 0);
            if (res.size() > 0) {
                log.debug("Keep version: {} because a proxy exists: {}", version.getRef(), versionSeriesId);
                cannotBeRemovedWithVersionsSeriesIds.add(versionSeriesId);
                return false;
            }
            canBeRemovedWithVersionsSeriesIds.add(versionSeriesId);
            return true;
        }

        /**
         * We want to detect change of command id to reset local cache.
         */
        @Override
        public void startBucket(String bucketKey) {
            super.startBucket(bucketKey);
            var currentCommandId = getCurrentCommand().getId();
            if (!currentCommandId.equals(this.lastCommandId)) {
                log.debug("Command id has changed, lets clear local cache");
                canBeRemovedWithVersionsSeriesIds.clear();
                cannotBeRemovedWithVersionsSeriesIds.clear();
            }
            this.lastCommandId = currentCommandId;
        }
    }

}
