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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2
 */
public class DocumentRoutingEscalationServiceImpl implements
        DocumentRoutingEscalationService {

    private static Log log = LogFactory.getLog(DocumentRoutingEscalationServiceImpl.class);

    public static final String queryForSuspendedNodesWithEscalation = "Select DISTINCT ecm:uuid from RouteNode WHERE ecm:currentLifeCycleState = 'suspended' "
            + "AND rnode:escalationRules/*/executed = 0 OR (rnode:escalationRules/*/executed = 1 AND rnode:escalationRules/*/multipleExecution = 1) ";

    @Override
    public List<String> queryForSuspendedNodesWithEscalation(CoreSession session)
            throws ClientException {
        final List<String> nodesDocIds = new ArrayList<String>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                IterableQueryResult results = session.queryAndFetch(
                        queryForSuspendedNodesWithEscalation, "NXQL");
                for (Map<String, Serializable> result : results) {
                    nodesDocIds.add(result.get("ecm:uuid").toString());
                    log.trace("Inspecting node for escalation rules:"
                            + result.get("ecm:uuid").toString());
                }
                results.close();
            }
        }.runUnrestricted();
        return nodesDocIds;
    }

    @Override
    public List<EscalationRule> computeEscalationRulesToExecute(GraphNode node)
            throws ClientException {
        return node.evaluateEscalationRules();
    }

    @Override
    public void scheduleExecution(EscalationRule rule, CoreSession session) {
        WorkManager manager = Framework.getLocalService(WorkManager.class);
        manager.schedule(
                new EscalationRuleWork(rule, session.getRepositoryName()),
                WorkManager.Scheduling.IF_NOT_SCHEDULED);
    }

    class EscalationRuleWork extends AbstractWork {

        protected String repositoryName;

        protected String workerId;

        protected EscalationRule rule;

        public static final String CATEGORY = "routingEscalation";

        public EscalationRuleWork(EscalationRule rule, String repositoryName) {
            this.rule = rule;
            workerId = rule.getNode().getId() + "_" + rule.getId();
            this.repositoryName = repositoryName;
        }

        @Override
        public String getTitle() {
            return workerId;
        }

        @Override
        public String getCategory() {
            return CATEGORY;
        }

        @Override
        public void afterRun(boolean ok) {
            super.afterRun(ok);
            // if ok, mark rule as resolved
            if (ok) {
                try {
                    new UnrestrictedSessionRunner(repositoryName) {
                        @Override
                        public void run() throws ClientException {
                            rule.setExecuted(true);
                            session.saveDocument(rule.getNode().getDocument());
                        }
                    }.runUnrestricted();

                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }
            }
        }

        @Override
        public void work() throws Exception {
            CoreSession session = initSession(repositoryName);
            GraphNode node = rule.getNode();
            OperationContext context = node.getExecutionContext(session);
            try {
                // check to see if the rule wasn't executed meanwhile
                boolean alreadyExecuted = getExecutionStatus(session);
                if (alreadyExecuted && !rule.isMultipleExecution()) {
                    log.trace("Rule " + rule.getId() + "on node "
                            + node.getId() + " already executed");
                    return;
                }
                Framework.getLocalService(AutomationService.class).run(context,
                        rule.getChain());
            } catch (InterruptedException e) {
                // restore interrupted state
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ClientException("Error when executing worker: "
                        + getTitle(), e);
            }
        }

        /**
         * Used to check the executed status when the escalationRule is run by a
         * worker in a work queue
         *
         * @param session
         * @throws ClientException
         */
        public boolean getExecutionStatus(CoreSession session)
                throws ClientException {
            DocumentModel nodeDoc = session.getDocument(new IdRef(
                    rule.getNode().getDocument().getId()));
            GraphNode node = nodeDoc.getAdapter(GraphNode.class);
            List<EscalationRule> rules = node.getEscalationRules();
            for (EscalationRule escalationRule : rules) {
                if (rule.compareTo(escalationRule) == 0) {
                    return escalationRule.isExecuted();
                }
            }
            return false;
        }
    }
}