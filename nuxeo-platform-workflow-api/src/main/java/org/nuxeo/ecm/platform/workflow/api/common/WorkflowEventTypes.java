/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: WorkflowEventTypes.java 23567 2007-08-08 14:29:36Z npaslaru $
 */

package org.nuxeo.ecm.platform.workflow.api.common;

/**
 * Workflow event types.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class WorkflowEventTypes {

    public static final String WORKFLOW_STARTED = "workflowStarted";

    public static final String WORKFLOW_ENDED = "workflowEnded";

    public static final String WORKFLOW_ABANDONED = "workflowAbandoned";

    public static final String WORKFLOW_TASK_CREATED = "workflowTaskCreated";

    public static final String WORKFLOW_TASK_STARTED = "workflowTaskStarted";

    public static final String WORKFLOW_TASK_ENDED = "workflowTaskEnded";

    public static final String WORKFLOW_TASK_REMOVED = "workflowTaskRemoved";

    public static final String WORKFLOW_TASK_REJECTED = "workflowTaskRejected";

    public static final String WORKFLOW_TASK_SUSPENDED = "workflowTaskSuspended";

    public static final String WORKFLOW_TASK_ASSIGNED = "workflowTaskAssigned";

    public static final String WORKFLOW_TASK_RETURNED = "workflowTaskReturned";

    public static final String WORKFLOW_TASK_UNASSIGNED = "workflowTaskUnassigned";

    public static final String APPROBATION_WORKFLOW_STARTED = "approbationWorkflowStarted";

    private WorkflowEventTypes() {
    }

}
