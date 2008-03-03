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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.Assignable;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;

/**
 * Assign the author to a start task if defined.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class DefaultAssignmentHandler extends JbpmWorkflowAssignmentHandler {

    protected static final Log log = LogFactory.getLog(DefaultAssignmentHandler.class);

    private static final long serialVersionUID = 1L;

    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
        String assignee = (String) executionContext.getVariable(WorkflowConstants.WORKFLOW_TASK_ASSIGNEE);
        if (assignee == null) {
            String creatorId = (String) executionContext.getProcessInstance().getContextInstance().getVariable(WorkflowConstants.WORKFLOW_CREATOR);
            if (creatorId != null) {
                log.debug("Assigning task to author :" + creatorId);
            }
            assignable.setActorId(creatorId);
        } else {
            assignable.setActorId(assignee);
            log.debug("Assigning task to assignee :" + assignee);
        }
    }

}
