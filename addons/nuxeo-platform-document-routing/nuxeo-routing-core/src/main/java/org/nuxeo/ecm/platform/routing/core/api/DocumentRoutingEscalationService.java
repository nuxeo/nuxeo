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

    /**
     * Query all running workflows and returns a list of nodes that are suspended and have escalation rules that can be
     * inspected. Uses an unrestricted session.
     *
     * @return
     * @since 5.7.2
     */
    List<String> queryForSuspendedNodesWithEscalation(CoreSession session);

    /**
     * Computes the list of escalation rules to be executed after their conditions are evaluated for the given node.
     *
     * @return
     * @since 5.7.2
     */
    List<EscalationRule> computeEscalationRulesToExecute(GraphNode node);

    /**
     * Schedules for execution an escalation rule. Uses an unrestricted session.
     *
     * @param rule
     * @since 5.7.2
     */
    void scheduleExecution(EscalationRule rule, CoreSession session);
}
