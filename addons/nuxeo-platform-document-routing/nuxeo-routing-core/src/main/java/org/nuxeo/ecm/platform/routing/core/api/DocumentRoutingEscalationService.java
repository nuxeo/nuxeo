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
package org.nuxeo.ecm.platform.routing.core.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.EscalationRule;

/**
 *
 * @since 5.7.2
 */
public interface DocumentRoutingEscalationService {

    /**
     * Query all running workflows and returns a list of nodes that are
     * suspended and have escalation rules that can be inspected. Uses an
     * unrestricted session.
     *
     * @return
     * @throws ClientException
     * @since 5.7.2
     */
    List<String> queryForSuspendedNodesWithEscalation(CoreSession session)
            throws ClientException;

    /**
     * Computes the list of escalation rules to be executed after their
     * conditions are evaluated for the given node.
     *
     * @return
     * @throws ClientException
     * @since 5.7.2
     */
    public List<EscalationRule> computeEscalationRulesToExecute(GraphNode node)
            throws ClientException;

    /**
     * Schedules for execution an escalation rule. Uses an unrestricted session.
     *
     * @param rule
     * @since 5.7.2
     */
    void scheduleExecution(EscalationRule rule, CoreSession session);
}