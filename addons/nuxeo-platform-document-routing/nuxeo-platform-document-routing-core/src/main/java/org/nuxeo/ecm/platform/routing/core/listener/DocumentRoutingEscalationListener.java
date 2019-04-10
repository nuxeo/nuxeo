/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.runtime.api.Framework;

/**
 * Triggers the execution of all escalation rules on all running workflows. This
 * listener is notified by the 'executeEscalationRules' event.
 *
 * @since 5.7.2
 *
 */
public class DocumentRoutingEscalationListener implements EventListener {

    public static final String EXECUTE_ESCALATION_RULE_EVENT = "executeEscalationRules";

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!EXECUTE_ESCALATION_RULE_EVENT.equals(event.getName())) {
            return;
        }
        RepositoryManager repositoryManager = ((RepositoryManager) Framework.getLocalService(RepositoryManager.class));
        for (Repository repo : repositoryManager.getRepositories()) {
            triggerEsclationRulesExecution(repo.getName());
        }

    }

    protected void triggerEsclationRulesExecution(String repositoryName)
            throws ClientException {
        new UnrestrictedSessionRunner(repositoryName) {

            @Override
            public void run() throws ClientException {
                DocumentRoutingEscalationService escalationService = Framework.getLocalService(DocumentRoutingEscalationService.class);
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
