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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEscalationService;
import org.nuxeo.ecm.platform.routing.core.api.scripting.RoutingScriptingExpression;
import org.nuxeo.ecm.platform.routing.core.api.scripting.RoutingScriptingFunctions;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 5.7.2
 */
public class DocumentRoutingEscalationServiceImpl implements DocumentRoutingEscalationService {

    private static final Log log = LogFactory.getLog(DocumentRoutingEscalationServiceImpl.class);

    /**
     * @deprecated since 2023.0, use {@link DocumentRoutingEscalationService#SUSPENDED_NODES_WITH_ESCALATION_QUERY}
     *             instead
     */
    @Deprecated
    public static final String queryForSuspendedNodesWithEscalation = SUSPENDED_NODES_WITH_ESCALATION_QUERY;

    /** @since 2023.0 */
    protected static final String WORKFLOW_ESCALATION_KV_STORE_NAME = "workflowEscalationRunning";

    /** @since 2023.0 */
    protected static final String ESCALATION_RUNNING_TTL_KEY = "nuxeo.document.routing.escalation.running.flag.ttl.duration";

    @Override
    @Deprecated
    public List<String> queryForSuspendedNodesWithEscalation(CoreSession session) {
        final List<String> nodesDocIds = new ArrayList<>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                IterableQueryResult results = session.queryAndFetch(SUSPENDED_NODES_WITH_ESCALATION_QUERY, "NXQL");
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
    public boolean isExecutionRunning(String repositoryName) {
        var kvStore = getKvStore();
        return Boolean.TRUE.toString().equals(kvStore.getString(repositoryName));
    }

    @Override
    public void setExecutionRunning(String repositoryName) {
        var kvStore = getKvStore();
        var configurationService = Framework.getService(ConfigurationService.class);
        kvStore.put(repositoryName, Boolean.TRUE.toString(),
                configurationService.getDuration(ESCALATION_RUNNING_TTL_KEY, Duration.ofMinutes(3)).toSeconds());
    }

    protected static KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(WORKFLOW_ESCALATION_KV_STORE_NAME);
    }

    @Override
    public List<EscalationRule> computeEscalationRulesToExecute(GraphNode node, boolean handleTransaction) {
        CoreSession session = node.getDocument().getCoreSession();
        List<EscalationRule> rulesToExecute = new ArrayList<>();
        for (EscalationRule rule : node.getEscalationRules()) {
            try (OperationContext context = getExecutionContext(session, node, handleTransaction)) {
                Expression expr = new RoutingScriptingExpression(rule.getCondition(),
                        new RoutingScriptingFunctions(context, rule));
                Object res = expr.eval(context);
                if (!(res instanceof Boolean)) {
                    throw new DocumentRouteException(String.format(
                            "Condition for rule: %s of node: %s of graph: %s does not evaluate to a boolean: %s", rule,
                            node.getId(), node.getGraph().getName(), rule.getCondition()));
                }
                boolean bool = Boolean.TRUE.equals(res);
                if ((!rule.isExecuted() || rule.isMultipleExecution()) && bool) {
                    rulesToExecute.add(rule);
                }
            } catch (DocumentRouteException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new DocumentRouteException("Error evaluating condition: " + rule.getCondition(), e);
            }
        }
        if (handleTransaction) {
            session.saveDocument(node.getDocument());
        }
        return rulesToExecute;
    }

    @Override
    public void executeEscalationRule(EscalationRule rule, boolean handleTransaction) {
        GraphNode node = rule.getNode();
        DocumentModel nodeDoc = node.getDocument();
        CoreSession session = nodeDoc.getCoreSession();
        // don't call GraphNode#executeChain in order to control the transaction
        try (OperationContext context = getExecutionContext(session, node, handleTransaction)) {
            AutomationService automationService = Framework.getService(AutomationService.class);
            automationService.run(context, rule.getChain());

            node.setAllVariables(context, true, handleTransaction);
            rule.setExecuted(true);
            if (handleTransaction) {
                session.saveDocument(nodeDoc);
            }
        } catch (OperationException e) {
            throw new DocumentRouteException(
                    String.format("Error when running chain: %s from escalation rule: %s of node: %s", rule.getChain(),
                            rule, node.getId()),
                    e);
        }
    }

    protected OperationContext getExecutionContext(CoreSession session, GraphNode node, boolean handleTransaction) {
        var context = new OperationContext(session);
        context.putAll(node.getWorkflowContextualInfo(session, true));
        context.handleTransaction(handleTransaction);
        context.setInput(context.get("documents"));
        return context;
    }

    @Override
    @Deprecated
    public void scheduleExecution(EscalationRule rule, CoreSession session) {
        WorkManager manager = Framework.getService(WorkManager.class);
        manager.schedule(
                new EscalationRuleWork(rule.getId(), rule.getNode().getDocument().getId(), session.getRepositoryName()),
                WorkManager.Scheduling.IF_NOT_SCHEDULED);
    }

    /**
     * @deprecated since 2023.0, it was replaced by
     *             {@link org.nuxeo.ecm.platform.routing.core.bulk.DocumentRoutingEscalationAction}
     */
    @Deprecated
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
        public int getRetryCount() {
            // retry when there is concurrency
            return 2;
        }

        @Override
        public boolean isIdempotent() {
            return false;
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
            // check to see if the rule wasn't executed meanwhile
            boolean alreadyExecuted = getExecutionStatus(rule, session);
            if (alreadyExecuted && !rule.isMultipleExecution()) {
                log.trace("Rule " + rule.getId() + "on node " + node.getId() + " already executed");
                return;
            }
            Framework.getService(DocumentRoutingEscalationService.class).executeEscalationRule(rule);
            // mark the rule as resolved
            markRuleAsExecuted(nodeDocId, escalationRuleId, session);
        }

        /**
         * Used to check the executed status when the escalationRule is run by a worker in a work queue
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
    /**
     * @deprecated since 2023.0, only used by {@link EscalationRuleWork}
     */
    @Deprecated
    private static void markRuleAsExecuted(String nodeDocId, String escalationRuleId, CoreSession session) {
        DocumentModel nodeDoc = session.getDocument(new IdRef(nodeDocId));
        GraphNode node = nodeDoc.getAdapter(GraphNode.class);
        List<EscalationRule> rules = node.getEscalationRules();
        for (EscalationRule escalationRule : rules) {
            if (escalationRuleId.equals(escalationRule.getId())) {
                escalationRule.setExecuted(true);
                break;
            }
        }
        session.saveDocument(nodeDoc);
    }
}
