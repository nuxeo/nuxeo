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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
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

    public static final String EXECUTE_ESCALATION_RULE_EVENT = "executeEscalationRules";

    @Override
    public void handleEvent(Event event) {
        if (!EXECUTE_ESCALATION_RULE_EVENT.equals(event.getName())) {
            return;
        }
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            triggerEsclationRulesExecution(repositoryName);
        }
    }

    protected void triggerEsclationRulesExecution(String repositoryName) {
        new UnrestrictedSessionRunner(repositoryName) {

            @Override
            public void run() {
                DocumentRoutingEscalationService escalationService = Framework.getService(DocumentRoutingEscalationService.class);
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
