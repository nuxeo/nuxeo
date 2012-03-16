/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Seam component holding tasks actions created using the {@link TaskService} in
 * document context cache.
 *
 * @author Anahide Tchertchian
 */
@Name("taskActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class TaskActionsBean extends DocumentContextBoundActionBean {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

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
    public List<Task> getCurrentDocumentTasks() throws ClientException {
        if (tasks == null) {
            tasks = new ArrayList<Task>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
                tasks = taskService.getTaskInstances(currentDocument,
                        principal, documentManager);
            }
        }
        return tasks;
    }

    @Factory(value = "currentDashBoardItems", scope = ScopeType.EVENT)
    public List<DashBoardItem> getCurrentDashBoardItems()
            throws ClientException {
        if (items == null) {
            items = new ArrayList<DashBoardItem>();
            for (Task task : getCurrentDocumentTasks()) {
                DashBoardItem item = new DashBoardItemImpl(task,
                        navigationContext.getCurrentDocument(),
                        localeSelector.getLocale());
                items.add(item);
            }
        }
        return items;
    }

    @Factory(value = "currentDashBoardItemsExceptPublishingTasks", scope = ScopeType.EVENT)
    public List<DashBoardItem> getCurrentDashBoardItemsExceptPublishingTasks()
            throws ClientException {
        if (items == null) {
            items = new ArrayList<DashBoardItem>();
            for (Task task : getCurrentDocumentTasks()) {
                String taskType = task.getVariable(Task.TaskVariableName.taskType.name());
                if (!"publish_moderate".equals(taskType)) {
                    DashBoardItem item = new DashBoardItemImpl(task,
                            navigationContext.getCurrentDocument(),
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

    public void acceptTask(Task task) throws ClientException {
        acceptTask(task, getComment());
        setComment(null);
    }

    public void acceptTask(Task task, String comment) throws ClientException {
        String seamEventName = taskService.acceptTask(documentManager,
                (NuxeoPrincipal) documentManager.getPrincipal(), task, comment);
        if (seamEventName != null) {
            Events.instance().raiseEvent(seamEventName);
        }
    }

    public void rejectTask(Task task) throws ClientException {
        String userComment = getComment();
        if (userComment != null && !"".equals(userComment)) {
            rejectTask(task, userComment);
            setComment(null);
        } else {
            facesMessages.add(
                    StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.enterComment"));
        }

    }

    public void rejectTask(Task task, String comment) throws ClientException {
        String seamEventName = taskService.rejectTask(documentManager,
                (NuxeoPrincipal) documentManager.getPrincipal(), task, comment);
        if (seamEventName != null) {
            Events.instance().raiseEvent(seamEventName);
        }
    }

    @Override
    public void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCache();
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
    public void resetCache() {
        tasks = null;
        items = null;
    }

}
