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
 * $Id: DocumentTaskActions.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;

/**
 * Workflow tasks actions listener interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface DocumentTaskActions extends Serializable {

    /**
     * Updates document tasks stack.
     *
     * Seam <code>@Factory</code>
     *
     * @throws WorkflowException
     */
    List<WMWorkItemInstance> computeDocumentTasks() throws WMWorkflowException;

    /**
     * Updates can manage workflow flag.
     *
     * Seam <code>@Factory</code>
     *
     * @return TODO
     */
    Boolean getCanManageWorkflow();

    /**
     * Creates a task for a given principal.
     *
     * @throws WorkflowException
     */
    String createTaskFor() throws WMWorkflowException;

    /**
     * Assigns a task to a given principal.
     * <p>
     * Expect <code>selectedPrincipal</code> injected.
     *
     */
    String assignTask(WMWorkItemInstance taskInstance, String principalName,
            boolean isGroup) throws WMWorkflowException;

    String startTask(String taskIdentifier) throws WMWorkflowException;

    String removeOneTask(ActionEvent event);

    /**
     * End a task.
     *
     * @param taskId the task identifier
     *
     * @return redirect view id
     */
    String endTask(String taskId) throws WMWorkflowException;

    /**
     * End a task and follow a transition.
     *
     * @param taskId the task identifier
     * @param transition the transition name
     *
     * @return redirect view id
     */
    String endTask(String taskId, String transition) throws WMWorkflowException;

    /**
     * Returns the current principal in the context.
     * <p>
     * It is extracted from the EJBContext.
     *
     * @return a Principal instance
     */
    Principal getPrincipal() throws WMWorkflowException;

    boolean canRemoveWorkItem(WMWorkItemInstance wi);

    boolean canApproveWorkItem(WMWorkItemInstance wi);

    boolean canRejectWorkItem(WMWorkItemInstance wi);

    boolean canMoveUpWorkItem(WMWorkItemInstance wi);

    boolean canMoveDownWorkItem(WMWorkItemInstance wi);

    /**
     * Invalidate workflow context variables.
     *
     * Aimed at being called by <code>@Observer</code>
     */
    void invalidateContextVariables();

    void cleanContext();

    String getSelectedTaskDirective();

    void setSelectedTaskDirective(String selectedTaskDirective);

    Date getSelectedTaskDueDate();

    void setSelectedTaskDueDate(Date selectedTaskDueDate);

    String getSelectedTaskInsertionLevel();

    void setSelectedTaskInsertionLevel(String selectedTaskInsertionLevel);

    String rejectOneTask();

    String getTaskActionComment();

    void setTaskActionComment(String taskActionComment);

    String getUserComment();

    void setUserComment(String userComment);

    String moveWorkItemUp(String wiid) throws WMWorkflowException;

    String moveWorkItemDown(String wiid) throws WMWorkflowException;

    int getNextMaxReviewLevel();

    Map<String, Serializable> getTaskProperties(int order, String directive,
            Date dueDate, String comment);
}
