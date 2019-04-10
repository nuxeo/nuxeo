/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.routing.core.api.scripting;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;

/**
 * @since 5.9.3
 */
public class RoutingScriptingFunctions {

    private Log log = LogFactory.getLog(RoutingScriptingFunctions.class);

    public static final String BINDING_KEY = "WorkflowFn";

    protected GraphNode.EscalationRule rule;

    protected OperationContext ctx;

    public RoutingScriptingFunctions(OperationContext ctx) {
        this.ctx = ctx;
    }

    public RoutingScriptingFunctions(OperationContext ctx, GraphNode.EscalationRule rule) {
        this.ctx = ctx;
        this.rule = rule;
    }

    /**
     * Returns the time difference in milliseconds between the current time and the time the current workflow was
     * started
     */
    public long timeSinceWorkflowWasStarted() {
        return Calendar.getInstance().getTimeInMillis() - ((Calendar) ctx.get("workflowStartTime")).getTimeInMillis();
    }

    /**
     * Returns the time difference in milliseconds between the current time and the time the current node was started
     */
    public long timeSinceTaskWasStarted() {
        return Calendar.getInstance().getTimeInMillis() - ((Calendar) ctx.get("nodeStartTime")).getTimeInMillis();
    }

    /**
     * Returns the time difference in milliseconds between the current time and the task due date
     */
    public long timeSinceDueDateIsOver() {
        return Calendar.getInstance().getTimeInMillis() - ((Calendar) ctx.get("taskDueTime")).getTimeInMillis();
    }

    /**
     * Returns -1 if the current rule hasn't been executed or the execution date was not set on this rule; Returns the
     * time difference in milliseconds between the current time and the last time the rule was executed ( equivalent to
     * the rule being evaluated to 'true').
     */
    public long timeSinceRuleHasBeenFalse() {
        if (rule == null) {
            throw new ClientRuntimeException("No escalation rule available in this context");
        }
        Calendar lastExecutionTime = rule.getLastExecutionTime();
        if (lastExecutionTime == null) {
            log.warn("Trying to evaluate timeSinceRuleHasBeenFalse() for the rule " + rule.getId()
                    + " that hasn't been executed yet");
            return -1L;
        }
        if (!rule.isExecuted()) {
            log.warn("Rule " + rule.getId() + " was never executed. Use with " + BINDING_KEY
                    + " ruleAlreadyExecuted().");
            return -1L;
        }
        return Calendar.getInstance().getTimeInMillis() - rule.getLastExecutionTime().getTimeInMillis();
    }

    /**
     * Returns 'true' if the current rule has been executed
     */
    public boolean ruleAlreadyExecuted() {
        if (rule == null) {
            throw new ClientRuntimeException("No escalation rule available in this context");
        }
        return rule.isExecuted();
    }
}