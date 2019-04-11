/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    public Collection<DashBoardItem> computeDashboardItems() {
        if (currentUserTasks == null) {
            currentUserTasks = new ArrayList<>();
            List<Task> tasks = taskService.getCurrentTaskInstances(documentManager);
            if (tasks != null) {
                for (Task task : tasks) {
                    if (task.hasEnded() || task.isCancelled()) {
                        continue;
                    }
                    DocumentModel doc = taskService.getTargetDocumentModel(task, documentManager);
                    if (doc != null && !doc.isTrashed()) {
                        currentUserTasks.add(new DashBoardItemImpl(task, doc, localeSelector.getLocale()));
                    } else {
                        log.warn(
                                String.format("User '%s' has a task of type '%s' on a " + "missing or deleted document",
                                        currentUser.getName(), task.getName()));
                    }
                }
            }
        }
        return currentUserTasks;
    }

    @Observer(value = { TaskEventNames.WORKFLOW_ENDED, TaskEventNames.WORKFLOW_NEW_STARTED,
            TaskEventNames.WORKFLOW_TASK_START, TaskEventNames.WORKFLOW_TASK_STOP,
            TaskEventNames.WORKFLOW_TASK_REJECTED, TaskEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            TaskEventNames.WORKFLOW_TASK_COMPLETED, TaskEventNames.WORKFLOW_TASK_REMOVED,
            TaskEventNames.WORK_ITEMS_LIST_LOADED, TaskEventNames.WORKFLOW_TASKS_COMPUTED,
            TaskEventNames.WORKFLOW_ABANDONED, TaskEventNames.WORKFLOW_CANCELED, EventNames.DOMAIN_SELECTION_CHANGED,
            EventNames.DOCUMENT_PUBLICATION_REJECTED, EventNames.DOCUMENT_PUBLISHED }, create = false)
    @BypassInterceptors
    public void invalidateDashboardItems() {
        currentUserTasks = null;
    }

    public String refreshDashboardItems() {
        currentUserTasks = null;
        return null;
    }

}
