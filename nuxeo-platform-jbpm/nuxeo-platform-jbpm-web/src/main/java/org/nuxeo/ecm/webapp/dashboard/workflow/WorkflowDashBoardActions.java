/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.dashboard.workflow;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItem;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItemImpl;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.webapp.helpers.EventNames;

@Name("workflowDashBoardActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class WorkflowDashBoardActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient JbpmService jbpmService;

    protected Collection<DocumentProcessItem> currentUserProcesses;

    protected Collection<DashBoardItem> currentUserTasks;

    @In(required = false)
    protected transient Principal currentUser;

    @In(required = false)
    protected transient TaskDashBoardActions taskDashBoardActions;

    private static final Log log = LogFactory.getLog(WorkflowDashBoardActions.class);

    public Collection<DashBoardItem> computeDashboardItems()
            throws ClientException {
        if (currentUserTasks == null) {
            currentUserTasks = taskDashBoardActions.computeDashboardItems();
        }
        return currentUserTasks;
    }

    public Collection<DocumentProcessItem> computeDocumentProcessItems()
            throws ClientException {
        if (currentUserProcesses == null) {
            currentUserProcesses = new ArrayList<DocumentProcessItem>();
            NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
            List<ProcessInstance> processes = jbpmService.getCurrentProcessInstances(
                    (NuxeoPrincipal) currentUser, null);
            if (processes != null) {
                for (ProcessInstance process : processes) {
                    try {
                        if (process.hasEnded()) {
                            continue;
                        }
                        DocumentModel doc = jbpmService.getDocumentModel(
                                process, pal);
                        if (doc != null
                                && !LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                            currentUserProcesses.add(new DocumentProcessItemImpl(
                                    process, doc));
                        } else {
                            log.warn(String.format(
                                    "User '%s' has a process of type '%s' on a "
                                            + "missing or deleted document",
                                    currentUser.getName(),
                                    process.getProcessDefinition().getName()));
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
        return currentUserProcesses;
    }

    @Observer(value = { JbpmEventNames.WORKFLOW_ENDED,
            JbpmEventNames.WORKFLOW_NEW_STARTED,
            JbpmEventNames.WORKFLOW_TASK_STOP,
            JbpmEventNames.WORKFLOW_TASK_REJECTED,
            JbpmEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            JbpmEventNames.WORKFLOW_TASK_COMPLETED,
            JbpmEventNames.WORKFLOW_TASK_REMOVED,
            JbpmEventNames.WORK_ITEMS_LIST_LOADED,
            JbpmEventNames.WORKFLOW_TASKS_COMPUTED,
            JbpmEventNames.WORKFLOW_ABANDONED,
            JbpmEventNames.WORKFLOW_CANCELED,
            EventNames.DOMAIN_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void invalidateDocumentProcessItems() {
        currentUserProcesses = null;
    }

    @Observer(value = { JbpmEventNames.WORKFLOW_ENDED,
            JbpmEventNames.WORKFLOW_NEW_STARTED,
            JbpmEventNames.WORKFLOW_TASK_START,
            JbpmEventNames.WORKFLOW_TASK_STOP,
            JbpmEventNames.WORKFLOW_TASK_REJECTED,
            JbpmEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            JbpmEventNames.WORKFLOW_TASK_COMPLETED,
            JbpmEventNames.WORKFLOW_TASK_REMOVED,
            JbpmEventNames.WORK_ITEMS_LIST_LOADED,
            JbpmEventNames.WORKFLOW_TASKS_COMPUTED,
            JbpmEventNames.WORKFLOW_ABANDONED,
            JbpmEventNames.WORKFLOW_CANCELED,
            EventNames.DOMAIN_SELECTION_CHANGED, "documentPublicationRejected",
            "documentPublished" }, create = false)
    @BypassInterceptors
    public void invalidateDashboardItems() {
        currentUserTasks = null;
    }

    public String refreshDashboardItems() {
        currentUserTasks = null;
        return null;
    }

    public String refreshDocumentProcessItems() {
        currentUserProcesses = null;
        return null;
    }

}
