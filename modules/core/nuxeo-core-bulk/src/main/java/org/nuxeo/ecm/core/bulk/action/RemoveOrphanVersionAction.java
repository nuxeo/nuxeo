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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Removes the orphan versions.
 * <p>
 * A version stays referenced, and therefore is not removed, if any proxy points to a
 * version in the version history of any live document, or in the case of tree snapshot if there is a snapshot
 * containing a version in the version history of any live document.
 *
 * @since 2023
 */
public class RemoveOrphanVersionAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "removeOrphanVersion";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    public static final String PROXY_DOC_QUERY_TEMPLATE = "SELECT * FROM Document WHERE " + NXQL.ECM_ISPROXY
            + " = 1 AND " + NXQL.ECM_PROXY_VERSIONABLEID + " = '%s'";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RemoveProxyComputation::new, Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class RemoveProxyComputation extends AbstractBulkComputation {

        public RemoveProxyComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            String prevVersionSeriesId = null;
            for (DocumentModel version : loadDocuments(session, ids)) {
                String versionSeriesId = version.getVersionSeriesId();
                // if same versionSeriesId than previous iteration, don't do check again and remove directly
                if (!versionSeriesId.equals(prevVersionSeriesId)) {
                    if (session.exists(new IdRef(versionSeriesId))) {
                        continue;
                    }
                    // find the version series for proxies
                    String findProxies = String.format(PROXY_DOC_QUERY_TEMPLATE, versionSeriesId);
                    PartialList<Map<String, Serializable>> res = session.queryProjection(findProxies, 1, 0);
                    if (res.size() > 0) {
                        continue;
                    }
                }
                session.removeDocument(version.getRef());
            }

        }
    }
}
