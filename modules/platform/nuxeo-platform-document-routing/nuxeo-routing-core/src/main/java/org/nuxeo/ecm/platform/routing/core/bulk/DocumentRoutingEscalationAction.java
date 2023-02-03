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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.routing.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Computes the escalation rules to execute on given documents and then execute them.
 *
 * @since 2023.0
 */
public class DocumentRoutingEscalationAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "documentRoutingEscalation";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(DocumentRoutingEscalationComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class DocumentRoutingEscalationComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(DocumentRoutingEscalationComputation.class);

        public DocumentRoutingEscalationComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            Framework.getService(DocumentRoutingEscalationService.class).setExecutionRunning(command.getRepository());
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            var escalationService = Framework.getService(DocumentRoutingEscalationService.class);
            for (DocumentModel doc : loadDocuments(session, ids)) {
                GraphNode node = doc.getAdapter(GraphNode.class);
                try {
                    for (EscalationRule rule : escalationService.computeEscalationRulesToExecute(node, false)) {
                        escalationService.executeEscalationRule(rule, false);
                    }
                    session.saveDocument(doc);
                } catch (DocumentRouteException e) {
                    log.error("Unable to execute escalation rules on node: {}, skip it", node, e);
                    delta.inError(
                            String.format("Cannot execute escalation rules on node: %s, %s", node, e.getMessage()));
                }
            }
        }
    }

}
