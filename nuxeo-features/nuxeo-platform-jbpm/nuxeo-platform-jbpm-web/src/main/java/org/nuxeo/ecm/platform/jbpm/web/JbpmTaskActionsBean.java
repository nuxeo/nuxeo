/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.jbpm.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.TaskCreateDateComparator;
import org.nuxeo.ecm.platform.jbpm.TaskVariableFilter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Seam component holding tasks actions created using the
 * {@link JbpmTaskService} in document context cache.
 *
 * @author Anahide Tchertchian
 */
@Name("jbpmTaskActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class JbpmTaskActionsBean extends DocumentContextBoundActionBean {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient JbpmService jbpmService;

    @In(create = true)
    protected transient JbpmTaskService jbpmTaskService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected List<TaskInstance> tasks;

    @Factory(value = "currentSingleTasks", scope = ScopeType.EVENT)
    public List<TaskInstance> getCurrentDocumentTasks()
            throws NuxeoJbpmException {
        if (tasks == null) {
            tasks = new ArrayList<TaskInstance>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                tasks.addAll(jbpmService.getTaskInstances(
                        currentDocument,
                        (NuxeoPrincipal) null,
                        new TaskVariableFilter(
                                JbpmTaskService.TaskVariableName.createdFromTaskService.name(),
                                "true")));
                Collections.sort(tasks, new TaskCreateDateComparator());
            }
        }
        return tasks;
    }

    public void acceptTask(TaskInstance task, String comment)
            throws NuxeoJbpmException {
        jbpmTaskService.acceptTask(documentManager,
                (NuxeoPrincipal) documentManager.getPrincipal(), task, comment);
        Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_COMPLETED);
    }

    public void rejectTask(TaskInstance task, String comment)
            throws NuxeoJbpmException {
        jbpmTaskService.rejectTask(documentManager,
                (NuxeoPrincipal) documentManager.getPrincipal(), task, comment);
        Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_REJECTED);
    }

    @Override
    public void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCache();
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
    public void resetCache() {
        tasks = null;
    }

}
