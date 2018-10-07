/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian, Antoine Taillefer
 */
package org.nuxeo.ecm.platform.task.web;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Seam component holding tasks actions created using the {@link TaskService} in document context cache.
 *
 * @author Anahide Tchertchian
 */
@Name("taskActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class TaskActionsBean extends DocumentContextBoundActionBean {

    private static final long serialVersionUID = 1L;

    /**
     * @since 7.2
     */
    public static final String TASKS_CACHE_RESET = "tasksCacheReset";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected transient TaskService taskService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected List<Task> tasks;

    protected List<DashBoardItem> items;

    protected String comment;

    @Factory(value = "currentSingleTasks", scope = ScopeType.EVENT)
    public List<Task> getCurrentDocumentTasks() {
        if (tasks == null) {
            tasks = new ArrayList<Task>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                NuxeoPrincipal principal = documentManager.getPrincipal();
                List<String> actors = new ArrayList<String>();
                actors.addAll(TaskActorsHelper.getTaskActors(principal));
                tasks = taskService.getTaskInstances(currentDocument, actors, true, documentManager);
            }
        }
        return tasks;
    }

    @Factory(value = "currentDashBoardItems", scope = ScopeType.EVENT)
    public List<DashBoardItem> getCurrentDashBoardItems() {
        if (items == null) {
            items = new ArrayList<DashBoardItem>();
            for (Task task : getCurrentDocumentTasks()) {
                DashBoardItem item = new DashBoardItemImpl(task, navigationContext.getCurrentDocument(),
                        localeSelector.getLocale());
                items.add(item);
            }
        }
        return items;
    }

    @Factory(value = "currentDashBoardItemsExceptPublishingTasks", scope = ScopeType.EVENT)
    public List<DashBoardItem> getCurrentDashBoardItemsExceptPublishingTasks() {
        if (items == null) {
            items = new ArrayList<DashBoardItem>();
            for (Task task : getCurrentDocumentTasks()) {
                String taskType = task.getVariable(Task.TaskVariableName.taskType.name());
                if (!"publish_moderate".equals(taskType)) {
                    DashBoardItem item = new DashBoardItemImpl(task, navigationContext.getCurrentDocument(),
                            localeSelector.getLocale());
                    items.add(item);
                }
            }
        }
        return items;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void acceptTask(Task task) {
        acceptTask(task, getComment());
        setComment(null);
    }

    public void acceptTask(Task task, String comment) {
        String seamEventName = taskService.acceptTask(documentManager, documentManager.getPrincipal(),
                task, comment);
        if (seamEventName != null) {
            Events.instance().raiseEvent(seamEventName);
        }
    }

    public void rejectTask(Task task) {
        String userComment = getComment();
        if (userComment != null && !"".equals(userComment)) {
            rejectTask(task, userComment);
            setComment(null);
        } else {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.review.task.enterComment"));
        }

    }

    public void rejectTask(Task task, String comment) {
        String seamEventName = taskService.rejectTask(documentManager, documentManager.getPrincipal(),
                task, comment);
        if (seamEventName != null) {
            Events.instance().raiseEvent(seamEventName);
        }
    }

    @Override
    public void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCache();
    }

    @Observer(value = { TaskEventNames.WORKFLOW_ENDED, TaskEventNames.WORKFLOW_NEW_STARTED,
            TaskEventNames.WORKFLOW_TASK_START, TaskEventNames.WORKFLOW_TASK_STOP,
            TaskEventNames.WORKFLOW_TASK_REJECTED, TaskEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            TaskEventNames.WORKFLOW_TASK_REASSIGNED, TaskEventNames.WORKFLOW_TASK_DELEGATED,
            TaskEventNames.WORKFLOW_TASK_COMPLETED, TaskEventNames.WORKFLOW_TASK_REMOVED,
            TaskEventNames.WORK_ITEMS_LIST_LOADED, TaskEventNames.WORKFLOW_TASKS_COMPUTED,
            TaskEventNames.WORKFLOW_ABANDONED, TaskEventNames.WORKFLOW_CANCELED, EventNames.DOMAIN_SELECTION_CHANGED,
            "documentPublicationRejected", "documentPublished" }, create = false)
    @BypassInterceptors
    public void resetCache() {
        tasks = null;
        items = null;
        Events.instance().raiseEvent(TASKS_CACHE_RESET);
    }

    /**
     * @since 7.2
     */
    @Observer({ TASKS_CACHE_RESET })
    public void resetTasksCache() {
        contentViewActions.refreshOnSeamEvent(TASKS_CACHE_RESET);
        contentViewActions.resetPageProviderOnSeamEvent(TASKS_CACHE_RESET);
    }

}
