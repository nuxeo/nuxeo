/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService.SUSPENDED_NODES_WITH_ESCALATION_QUERY;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.bulk.DocumentRoutingEscalationAction;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.runtime.api.Framework;

/**
 * Triggers the execution of all escalation rules on all running workflows. This listener is notified by the
 * 'executeEscalationRules' event.
 *
 * @since 5.7.2
 */
public class DocumentRoutingEscalationListener implements EventListener {

    private static final Logger log = LogManager.getLogger(DocumentRoutingEscalationListener.class);

    public static final String EXECUTE_ESCALATION_RULE_EVENT = "executeEscalationRules";

    /**
     * Allows to use legacy escalation rules execution mechanism.
     *
     * @since 2023.0
     * @deprecated since 2023.0, no replacement
     */
    @Deprecated
    public static final String USE_LEGACY_CONF_KEY = "nuxeo.document.routing.escalation.legacy";

    @Override
    public void handleEvent(Event event) {
        if (!EXECUTE_ESCALATION_RULE_EVENT.equals(event.getName())) {
            return;
        }
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        if (Boolean.parseBoolean(Framework.getProperty(USE_LEGACY_CONF_KEY, "false"))) {
            for (String repositoryName : repositoryManager.getRepositoryNames()) {
                triggerEsclationRulesExecution(repositoryName);
            }
        } else {
            var bulkService = Framework.getService(BulkService.class);
            var escalationService = Framework.getService(DocumentRoutingEscalationService.class);
            // services might be null when the scheduler fired the event at the same time as Nuxeo is shutting down
            if (bulkService != null && escalationService != null) {
                for (String repositoryName : repositoryManager.getRepositoryNames()) {
                    if (escalationService.isExecutionRunning(repositoryName)) {
                        log.warn(
                                "Not scheduling Workflow Escalation execution on repository: {} because one is already running",
                                repositoryName);
                    } else {
                        var command = new BulkCommand.Builder(DocumentRoutingEscalationAction.ACTION_NAME,
                                SUSPENDED_NODES_WITH_ESCALATION_QUERY, SYSTEM_USERNAME).repository(repositoryName)
                                                                                       .build();
                        try {
                            bulkService.submit(command);
                        } catch (IllegalStateException e) {
                            log.warn(
                                    "Not scheduling Workflow Escalation execution on repository: {} because one is already running",
                                    repositoryName);
                        }
                    }
                }
            }
        }
    }

    /**
     * @deprecated since 2023.0, use {@link DocumentRoutingEscalationAction} instead
     */
    @Deprecated
    protected void triggerEsclationRulesExecution(String repositoryName) {
        new UnrestrictedSessionRunner(repositoryName) {

            @Override
            public void run() {
                DocumentRoutingEscalationService escalationService = Framework.getService(
                        DocumentRoutingEscalationService.class);
                List<String> nodeIds = escalationService.queryForSuspendedNodesWithEscalation(session);
                for (String id : nodeIds) {
                    DocumentModel nodeDoc = session.getDocument(new IdRef(id));
                    GraphNode node = nodeDoc.getAdapter(GraphNode.class);
                    List<EscalationRule> rules = escalationService.computeEscalationRulesToExecute(node);
                    for (EscalationRule rule : rules) {
                        escalationService.scheduleExecution(rule, session);
                    }
                }
            }
        }.runUnrestricted();
    }
}
