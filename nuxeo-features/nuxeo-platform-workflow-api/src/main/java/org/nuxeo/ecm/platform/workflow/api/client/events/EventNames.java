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
 * $Id: EventNames.java 29075 2008-01-16 09:12:59Z jcarsique $
 */

package org.nuxeo.ecm.platform.workflow.api.client.events;

/**
 * Seam event identifiers.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class EventNames {

    /**
    * Fired when WF Seam Bean are first created.
    * Used to create the WF EventListner that catches Seam events
    */
    public static final String WF_INIT = "workflowInit";

    /**
     * Fired when a document selection changes (file, folder, etc., but not
     * workspace or above).
     */
    // XXX: this is a duplicate from the webapp which I can't specify as a
    // dependency
    public static final String DOCUMENT_SELECTION_CHANGED = "documentSelectionChanged";

    /**
     * This is fired when the user selection changes. This should be listened by
     * components that want to do some work when the user selection changes,
     * regardless of the type of selected document.
     */
    // :XXX: this is a duplicate from the webapp which I can't specify as a
    // dependency
    public static final String USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED = "userAllDocumentTypesSelectionChanged";

    /**
     * Should be raised when a document is edited.
     */
    // :XXX: this is a duplicate from the webapp which I can't specify as a
    // dependency
    public static final String DOCUMENT_CHANGED = "documentChanged";

    /**
     * This is raised when a proxy is created and need a moderation
     */
    public static final String PROXY_PUSLISHING_PENDING = "proxyPublishingPending";

    /**
     * Fired when a new process is started.
     */
    public static final String WORKFLOW_NEW_STARTED = "workflowNewProcessStarted";

    /**
     * Fired when a process has been ended.
     */
    public static final String WORKFLOW_ENDED = "workflowProcessEnded";

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

    /**
     * Fired when an assignment has been updated.
     */
    public static final String WORKFLOW_USER_ASSIGNMENT_CHANGED = "workflowUserAssignmentChanged";

    /**
     * Fired when content root selection is changed (like workspaces, sections,
     * etc. types).
     */
    public static final String CONTENT_ROOT_SELECTION_CHANGED = "contentRootSelectionChanged";

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
     * Fired when workflow actions change life cycle.
     */
    public static final String CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED = "currentDocumentLifeCycleChanged";

    // Constant utility class.
    private EventNames() {
    }

}
