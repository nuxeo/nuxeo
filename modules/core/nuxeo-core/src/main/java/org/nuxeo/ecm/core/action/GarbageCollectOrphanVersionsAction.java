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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.CoreService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Removes the orphan versions.
 * <p>
 * A version stays referenced, and therefore is not removed, if any proxy points to a version in the version history of
 * any live document, or in the case of tree snapshot if there is a snapshot containing a version in the version history
 * of any live document.
 *
 * @since 2023
 */
public class GarbageCollectOrphanVersionsAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(GarbageCollectOrphanVersionsAction.class);

    public static final String ACTION_NAME = "garbageCollectOrphanVersions";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String PROXY_DOC_QUERY_TEMPLATE = "SELECT " + NXQL.ECM_UUID + " FROM Document WHERE "
            + NXQL.ECM_ISPROXY + " = 1 AND " + NXQL.ECM_PROXY_VERSIONABLEID + " = '%s'";

    public static final String VERSIONS_OF_LIVE_DOC_QUERY_TEMPLATE = "SELECT " + NXQL.ECM_UUID + " FROM Document WHERE "
            + NXQL.ECM_ISVERSION + " = 1 AND " + NXQL.ECM_PROXY_VERSIONABLEID + " = '%s'";

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
         * Cache that keeps the ids of the live doc for which we can remove associated versions.
         */
        protected final CircularFifoQueue<String> canBeRemovedWithVersionsSeriesIds;

        /**
         * Cache that keeps the ids of the live doc for which we cannot remove associated versions.
         */
        protected final CircularFifoQueue<String> cannotBeRemovedWithVersionsSeriesIds;

        protected Collection<OrphanVersionRemovalFilter> filters;

        protected String lastCommandId;

        public GarbageCollectOrphanVersionsComputation() {
            super(ACTION_FULL_NAME);
            canBeRemovedWithVersionsSeriesIds = new CircularFifoQueue<>(CACHE_SIZE);
            cannotBeRemovedWithVersionsSeriesIds = new CircularFifoQueue<>(CACHE_SIZE);
            CoreService coreService = Framework.getService(CoreService.class);
            filters = coreService.getOrphanVersionRemovalFilters();
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            for (DocumentModel version : loadDocuments(session, ids)) {
                if (!version.isVersion()) {
                    log.debug("Document: {} is not a version", version::getId);
                    continue;
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
            log.debug("Checking if version: {} is orphan of live document: {}", version.getRef(), versionSeriesId);
            if (canBeRemovedWithVersionsSeriesIds.contains(versionSeriesId)) {
                log.debug("With versionSeriesId: {} is cached as can be removed", versionSeriesId);
                return true;
            }
            if (cannotBeRemovedWithVersionsSeriesIds.contains(versionSeriesId)) {
                log.debug("With versionSeriesId: {} is cached as cannot be removed", versionSeriesId);
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
            if (!res.isEmpty()) {
                log.debug("Keep version: {} because a proxy exists: {}", version.getRef(), versionSeriesId);
                cannotBeRemovedWithVersionsSeriesIds.add(versionSeriesId);
                return false;
            }
            if (!filters.isEmpty()) {
                // Hopefully, this OrphanVersionRemovalFilter extension point is rarely used
                // and we don't pass here else it will overwhelm the DB
                String otherVersions = String.format(VERSIONS_OF_LIVE_DOC_QUERY_TEMPLATE, versionSeriesId);
                List<String> versionIds = new ArrayList<>();
                try (IterableQueryResult result = session.queryAndFetch(otherVersions, NXQL.NXQL)) {
                    for (Map<String, Serializable> map : res) {
                        String id = (String) map.get(NXQL.ECM_UUID);
                        versionIds.add(id);
                    }
                }
                for (OrphanVersionRemovalFilter filter : filters) {
                    List<String> removableVersionIds = filter.getRemovableVersionIds(session, null, versionIds);
                    if (!removableVersionIds.contains(version.getId())) {
                        log.debug("Keep version: {} because filter: {} forbids its removal", version.getRef(),
                                filter.getClass().getName());
                        cannotBeRemovedWithVersionsSeriesIds.add(versionSeriesId);
                        return false;
                    }
                }
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
