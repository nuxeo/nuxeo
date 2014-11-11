/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.task;

/**
 * Event identifiers.
 *
 * @since 5.5
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class TaskEventNames {

    /**
     * Fired when WF Seam Bean are first created. Used to create the WF
     * EventListener that catches Seam events
     */
    public static final String WF_INIT = "workflowInit";

    /**
     * Fired when a new process is started.
     */
    public static final String WORKFLOW_NEW_STARTED = "workflowNewProcessStarted";

    /**
     * Fired when a process has been ended.
     */
    public static final String WORKFLOW_ENDED = "workflowProcessEnded";

    /**
     * Fired when a process has been abandoned.
     */
    public static final String WORKFLOW_ABANDONED = "workflowAbandoned";

    /**
     * Fired when a process has been canceled.
     */
    public static final String WORKFLOW_CANCELED = "workflowProcessCanceled";

    /**
     * Fired when a task has been started.
     */
    public static final String WORKFLOW_TASK_START = "workflowTaskStart";

    /**
     * Fired when a task has been ended.
     */
    public static final String WORKFLOW_TASK_STOP = "workflowTaskStop";

    /**
     * Fired when a task has been completed.
     */
    public static final String WORKFLOW_TASK_COMPLETED = "workflowTaskCompleted";

    /**
     * Fired when a task has been removed.
     */
    public static final String WORKFLOW_TASK_REMOVED = "workflowTaskRemoved";

    /**
     * Fired when a task has been rejected.
     */
    public static final String WORKFLOW_TASK_REJECTED = "workflowTaskRejected";

    public static final String WORKFLOW_TASK_ASSIGNED = "workflowTaskAssigned";

    /**
     * Fired when an assignment has been updated.
     */
    public static final String WORKFLOW_USER_ASSIGNMENT_CHANGED = "workflowUserAssignmentChanged";

    public static final String WORKFLOW_TASKS_COMPUTED = "workflowTasksComputed";

    /**
     * Fired when a new work items list is created.
     */
    public static final String WORK_ITEMS_LIST_ADDED = "workItemsListAdded";

    /**
     * Fired when a work items list is deleted.
     */
    public static final String WORK_ITEMS_LIST_REMOVED = "workItemsListRemoved";

    /**
     * Fired when a work items list is loaded.
     */
    public static final String WORK_ITEMS_LIST_LOADED = "workItemsListLoaded";

    /**
     * A task variable key to disable the notification service. (i.e. no email
     * sending through this service)
     */
    public static final String DISABLE_NOTIFICATION_SERVICE = "disableNotificationService";

    // Constant utility class.
    private TaskEventNames() {
    }

}
