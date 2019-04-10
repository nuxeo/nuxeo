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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2
 */
public class DocumentRoutingEscalationServiceImpl implements DocumentRoutingEscalationService {

    private static Log log = LogFactory.getLog(DocumentRoutingEscalationServiceImpl.class);

    public static final String queryForSuspendedNodesWithEscalation = "Select DISTINCT ecm:uuid from RouteNode WHERE ecm:currentLifeCycleState = 'suspended' "
            + "AND ( rnode:escalationRules/*1/executed = 0 OR rnode:escalationRules/*1/multipleExecution = 1 )";

    @Override
    public List<String> queryForSuspendedNodesWithEscalation(CoreSession session) {
        final List<String> nodesDocIds = new ArrayList<String>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                IterableQueryResult results = session.queryAndFetch(queryForSuspendedNodesWithEscalation, "NXQL");
                for (Map<String, Serializable> result : results) {
                    nodesDocIds.add(result.get("ecm:uuid").toString());
                    log.trace("Inspecting node for escalation rules:" + result.get("ecm:uuid").toString());
                }
                results.close();
            }
        }.runUnrestricted();
        return nodesDocIds;
    }

    @Override
    public List<EscalationRule> computeEscalationRulesToExecute(GraphNode node) {
        return node.evaluateEscalationRules();
    }

    @Override
    public void scheduleExecution(EscalationRule rule, CoreSession session) {
        WorkManager manager = Framework.getLocalService(WorkManager.class);
        manager.schedule(
                new EscalationRuleWork(rule.getId(), rule.getNode().getDocument().getId(), session.getRepositoryName()),
                WorkManager.Scheduling.IF_NOT_SCHEDULED);
    }

    public static class EscalationRuleWork extends AbstractWork {

        private static final long serialVersionUID = 1L;

        protected String escalationRuleId;

        protected String nodeDocId;

        public static final String CATEGORY = "routingEscalation";

        public EscalationRuleWork(String escalationRuleId, String nodeDocId, String repositoryName) {
            super(repositoryName + ":" + nodeDocId + ":escalationRule:" + escalationRuleId);
            this.repositoryName = repositoryName;
            this.escalationRuleId = escalationRuleId;
            this.nodeDocId = nodeDocId;
        }

        @Override
        public String getTitle() {
            return getId();
        }

        @Override
        public String getCategory() {
            return CATEGORY;
        }

        @Override
        public void work() {
            openSystemSession();
            DocumentModel nodeDoc = session.getDocument(new IdRef(nodeDocId));
            GraphNode node = nodeDoc.getAdapter(GraphNode.class);
            if (node == null) {
                throw new NuxeoException("Can't execute worker '" + getId() + "' : the document '" + nodeDocId
                        + "' can not be adapted to a GraphNode");
            }
            List<EscalationRule> rules = node.getEscalationRules();
            EscalationRule rule = null;
            for (EscalationRule escalationRule : rules) {
                if (escalationRuleId.equals(escalationRule.getId())) {
                    rule = escalationRule;
                    break;
                }
            }
            if (rule == null) {
                throw new NuxeoException("Can't execute worker '" + getId() + "' : the rule '" + escalationRuleId
                        + "' was not found on the node '" + nodeDocId + "'");
            }
            try (OperationContext context = new OperationContext(session)) {
                context.putAll(node.getWorkflowContextualInfo(session, true));
                context.setInput(context.get("documents"));
                // check to see if the rule wasn't executed meanwhile
                boolean alreadyExecuted = getExecutionStatus(rule, session);
                if (alreadyExecuted && !rule.isMultipleExecution()) {
                    log.trace("Rule " + rule.getId() + "on node " + node.getId() + " already executed");
                    return;
                }
                node.executeChain(rule.getChain());
                // mark the rule as resolved
                markRuleAsExecuted(nodeDocId, escalationRuleId, session);
            } catch (NuxeoException e) {
                e.addInfo("Error when executing worker: " + getTitle());
                throw e;
            } catch (OperationException e) {
                throw new NuxeoException("Error when executing worker: " + getTitle(), e);
            }
        }

        /**
         * Used to check the executed status when the escalationRule is run by a worker in a work queue
         *
         * @param session
         */
        public boolean getExecutionStatus(EscalationRule rule, CoreSession session) {
            DocumentModel nodeDoc = session.getDocument(new IdRef(rule.getNode().getDocument().getId()));
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

    private static void markRuleAsExecuted(String nodeDocId, String escalationRuleId, CoreSession session)
            {
        DocumentModel nodeDoc = session.getDocument(new IdRef(nodeDocId));
        GraphNode node = nodeDoc.getAdapter(GraphNode.class);
        List<EscalationRule> rules = node.getEscalationRules();
        EscalationRule rule = null;
        for (EscalationRule escalationRule : rules) {
            if (escalationRuleId.equals(escalationRule.getId())) {
                rule = escalationRule;
                break;
            }
        }
        rule.setExecuted(true);
        session.saveDocument(nodeDoc);
    }
}
