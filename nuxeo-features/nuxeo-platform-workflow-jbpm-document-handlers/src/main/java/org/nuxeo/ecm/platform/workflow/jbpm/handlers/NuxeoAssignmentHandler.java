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
 * $Id: NuxeoAssignmentHandler.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm.handlers;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.Assignable;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentAssignmentHandler;

/**
 * Nuxeo assignment handler.
 * <p>
 * Deals with rights beside assignment.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class NuxeoAssignmentHandler extends
        AbstractWorkflowDocumentAssignmentHandler {

    private static final long serialVersionUID = 8840489637026383157L;

    public void assign(Assignable assignable, ExecutionContext ec)
            throws Exception {
        // Injected by the JbpmWorkflowEngine implementation.
        String assignee = (String) ec.getVariable(WorkflowConstants.WORKFLOW_TASK_ASSIGNEE);
        if (assignee == null) {
            String creatorId = getProcessInstanceCreator(ec);
            if (creatorId != null) {
                log.debug("Assigning task to author :" + creatorId);
            }
            assignable.setActorId(creatorId);
        } else {
            assignable.setActorId(assignee);
            log.debug("Assigning task to assignee :" + assignee);
        }
        // Setup rights.
        //setupRightsFromPolicy(ec);
    }

}
