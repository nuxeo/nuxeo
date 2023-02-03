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
package org.nuxeo.ecm.platform.routing.core.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;

/**
 * @since 5.7.2
 */
public interface DocumentRoutingEscalationService {

    /** @since 2023.0 */
    String SUSPENDED_NODES_WITH_ESCALATION_QUERY = "SELECT DISTINCT ecm:uuid FROM RouteNode WHERE " //
            + "ecm:currentLifeCycleState = 'suspended' "
            + "AND ( rnode:escalationRules/*1/executed = 0 OR rnode:escalationRules/*1/multipleExecution = 1 )";

    /**
     * Query all running workflows and returns a list of nodes that are suspended and have escalation rules that can be
     * inspected. Uses an unrestricted session.
     *
     * @since 5.7.2
     * @deprecated since 2023.0, this method doesn't scale, use the {@link CoreSession} query APIs with
     *             {@link #SUSPENDED_NODES_WITH_ESCALATION_QUERY} instead
     */
    @Deprecated
    List<String> queryForSuspendedNodesWithEscalation(CoreSession session);

    /**
     * Returns whether a workflow escalation execution is running on the given {@code repositoryName}.
     *
     * @since 2023.0
     */
    default boolean isExecutionRunning(String repositoryName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Sets that a workflow escalation execution on given {@code repositoryName} is currently running.
     *
     * @since 2023.0
     */
    default void setExecutionRunning(String repositoryName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Computes the list of escalation rules to be executed after their conditions are evaluated for the given node.
     *
     * @param node the {@link GraphNode} to retrieve and evaluate from the escalation rules
     * @since 5.7.2
     */
    default List<EscalationRule> computeEscalationRulesToExecute(GraphNode node) {
        return computeEscalationRulesToExecute(node, true);
    }

    /**
     * Computes the list of escalation rules to be executed after their conditions are evaluated for the given node.
     *
     * @param node the {@link GraphNode} to retrieve and evaluate from the escalation rules
     * @param handleTransaction whether the escalation rule evaluation should handle the transaction and save
     * @since 2023.0
     */
    default List<EscalationRule> computeEscalationRulesToExecute(GraphNode node, boolean handleTransaction) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Executes the given escalation rule.
     *
     * @param rule the rule to execute
     * @since 2023.0
     */
    default void executeEscalationRule(EscalationRule rule) {
        executeEscalationRule(rule, true);
    }

    /**
     * Executes the given escalation rule.
     *
     * @param rule the rule to execute
     * @param handleTransaction whether the escalation rule execution should handle the transaction and save
     * @since 2023.0
     */
    default void executeEscalationRule(EscalationRule rule, boolean handleTransaction) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Schedules for execution an escalation rule. Uses an unrestricted session.
     *
     * @since 5.7.2
     * @deprecated since 2023.0, it was replaced by
     *             {@link org.nuxeo.ecm.platform.routing.core.bulk.DocumentRoutingEscalationAction}
     */
    @Deprecated
    void scheduleExecution(EscalationRule rule, CoreSession session);
}
