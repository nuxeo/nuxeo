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
 *     fox
 */
package org.nuxeo.ecm.platform.routing.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.GC_ROUTES_ACTION_NAME;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 2023
 */
public class GarbageCollectRoutesAction implements StreamProcessorTopology {

    private static final Logger log = LogManager.getLogger(GarbageCollectRoutesAction.class);

    public static final String ACTION_NAME = GC_ROUTES_ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(GarbageCollectRoutesComputation::new, Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class GarbageCollectRoutesComputation extends AbstractBulkComputation {

        public GarbageCollectRoutesComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            DocumentRoutingService routingService = Framework.getService(DocumentRoutingService.class);
            for (DocumentModel docModel : loadDocuments(session, ids)) {
                if (!DOCUMENT_ROUTE_DOCUMENT_TYPE.equals(docModel.getType())) {
                    log.debug("Document: {} is not a route", docModel::getId);
                    continue;
                }
                log.debug("Trying to remove route: {}", docModel::getId);
                routingService.purgeDocumentRoute(session, docModel.getAdapter(DocumentRoute.class));
            }
        }

    }

}
