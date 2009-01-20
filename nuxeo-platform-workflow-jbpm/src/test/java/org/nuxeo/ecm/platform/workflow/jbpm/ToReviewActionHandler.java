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
 * $Id: ToReviewActionHandler.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ToReviewActionHandler extends JbpmWorkflowActionHandler {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ToReviewActionHandler.class);

    public void execute(ExecutionContext executionContext) throws Exception {
        ProcessInstance processInstance = executionContext.getProcessInstance();
        String creatorId = (String) processInstance.getContextInstance().getVariable(
                WorkflowConstants.WORKFLOW_CREATOR);
        String destinationState = (String) processInstance.getContextInstance().getVariable(
                WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION);

        if (creatorId != null) {
            log.info("WORKFLOW CREATOR=" + creatorId);
            log.info("LIFE_CYCLE_STATE_DESTINATION=" + destinationState);
        }
    }

}
