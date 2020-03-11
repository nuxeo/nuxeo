/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Fired when WF Seam Bean are first created. Used to create the WF EventListener that catches Seam events
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
     *
     * @deprecated @since 5.7.3 WORKFLOW_TASK_REASSIGNED is used
     */
    @Deprecated
    public static final String WORKFLOW_USER_ASSIGNMENT_CHANGED = "workflowUserAssignmentChanged";

    public static final String WORKFLOW_TASKS_COMPUTED = "workflowTasksComputed";

    /**
     * @since 5.7.3
     */
    public static final String WORKFLOW_TASK_REASSIGNED = "workflowTaskReassigned";

    /**
     * @since 5.8
     */
    public static final String WORKFLOW_TASK_DELEGATED = "workflowTaskDelegated";

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
     * A task variable key to disable the notification service. (i.e. no email sending through this service)
     */
    public static final String DISABLE_NOTIFICATION_SERVICE = "disableNotificationService";

    // Constant utility class.
    private TaskEventNames() {
    }

}
