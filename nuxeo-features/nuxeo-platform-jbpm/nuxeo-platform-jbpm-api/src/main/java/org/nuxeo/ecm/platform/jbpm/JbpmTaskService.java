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
package org.nuxeo.ecm.platform.jbpm;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Service offering JPBM methods to create and end tasks independently from a
 * process.
 * <p>
 * {@link JbpmService} is called by this service to handle the task.
 *
 * @author Anahide Tchertchian
 */
public interface JbpmTaskService extends Serializable {

    /**
     * Property used to pass task in the notified events properties
     */
    String TASK_INSTANCE_EVENT_PROPERTIES_KEY = "taskInstance";

    /**
     * Variable names added as markers on the created task
     */
    enum TaskVariableName {
        createdFromTaskService,
    }

    /**
     * Creates a task and starts it. Notifies events with names
     * {@link JbpmEventNames#WORKFLOW_TASK_ASSIGNED} and
     * {@link JbpmEventNames#WORKFLOW_TASK_ASSIGNED}, passing the task in the
     * event properties using key {@link #TASK_INSTANCE_EVENT_PROPERTIES_KEY}
     *
     * @param coreSession the session to use when notifying
     * @param principal the principal marked as initiator of the task and used
     *            when notifying.
     * @param document the document to attach to the task.
     * @param taskName the task name.
     * @param prefixedActorIds the list of actor ids, prefixed with 'user:' or
     *            'group:'.
     * @param createOneTaskPerActor if true, one task will be created per
     *            actor, else a single task will be assigned to all actors.
     * @param directive the directive, put in the task variables.
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param dueDate the due date, set on the task instance
     * @param taskVariables additional task variables
     * @throws NuxeoJbpmException
     */
    void createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, Serializable> taskVariables) throws NuxeoJbpmException;

    /**
     * Returns true if user is an administrator, the initiator of the task, or
     * an actor of the task.
     *
     * @throws NuxeoJbpmException
     */
    boolean canEndTask(NuxeoPrincipal principal, TaskInstance task)
            throws NuxeoJbpmException;

    /**
     * Ends the task using event name
     * {@link JbpmEventNames#WORKFLOW_TASK_COMPLETED} and marking the task as
     * validated.
     *
     * @see #endTask(CoreSession, NuxeoPrincipal, TaskInstance, String, String,
     *      boolean)
     * @throws NuxeoJbpmException
     */
    void acceptTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment) throws NuxeoJbpmException;

    /**
     * Ends the task using event name
     * {@link JbpmEventNames#WORKFLOW_TASK_REJECTED} and marking the task as
     * not validated.
     *
     * @see #endTask(CoreSession, NuxeoPrincipal, TaskInstance, String, String,
     *      boolean)
     * @throws NuxeoJbpmException
     */
    void rejectTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment) throws NuxeoJbpmException;

    /**
     * Ends the task
     *
     * @param coreSession the session to use when notifying and resolving of
     *            referenced document for notification.
     * @param principal principal used when notifying
     * @param task the instance to end
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param eventName the event name to use when notifying
     * @param isValidated boolean marker to state if the task was validated or
     *            rejected
     * @throws NuxeoJbpmException when trying to end a task without being
     *             granted the right to do so (see
     *             {@link #canEndTask(NuxeoPrincipal, TaskInstance)}), or when
     *             any other error occurs
     */
    void endTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment, String eventName,
            boolean isValidated) throws NuxeoJbpmException;

}
