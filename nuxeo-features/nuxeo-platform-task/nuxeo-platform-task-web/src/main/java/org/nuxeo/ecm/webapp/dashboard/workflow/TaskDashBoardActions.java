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
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.ecm.webapp.helpers.EventNames;

@Name("taskDashBoardActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class TaskDashBoardActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient TaskService taskService;

    protected Collection<DashBoardItem> currentUserTasks;

    @In(required = false)
    protected transient Principal currentUser;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    private static final Log log = LogFactory.getLog(TaskDashBoardActions.class);

    public Collection<DashBoardItem> computeDashboardItems()
            throws ClientException {
        if (currentUserTasks == null) {
            currentUserTasks = new ArrayList<DashBoardItem>();
            List<Task> tasks = taskService.getCurrentTaskInstances(documentManager);
            if (tasks != null) {
                for (Task task : tasks) {
                    try {
                        if (task.hasEnded() || task.isCancelled()) {
                            continue;
                        }
                        DocumentModel doc = taskService.getTargetDocumentModel(
                                task, documentManager);
                        if (doc != null
                                && !LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                            currentUserTasks.add(new DashBoardItemImpl(task,
                                    doc, localeSelector.getLocale()));
                        } else {
                            log.warn(String.format(
                                    "User '%s' has a task of type '%s' on a "
                                            + "missing or deleted document",
                                    currentUser.getName(), task.getName()));
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
        return currentUserTasks;
    }

    @Observer(value = { TaskEventNames.WORKFLOW_ENDED,
            TaskEventNames.WORKFLOW_NEW_STARTED,
            TaskEventNames.WORKFLOW_TASK_START,
            TaskEventNames.WORKFLOW_TASK_STOP,
            TaskEventNames.WORKFLOW_TASK_REJECTED,
            TaskEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            TaskEventNames.WORKFLOW_TASK_COMPLETED,
            TaskEventNames.WORKFLOW_TASK_REMOVED,
            TaskEventNames.WORK_ITEMS_LIST_LOADED,
            TaskEventNames.WORKFLOW_TASKS_COMPUTED,
            TaskEventNames.WORKFLOW_ABANDONED,
            TaskEventNames.WORKFLOW_CANCELED,
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

}
