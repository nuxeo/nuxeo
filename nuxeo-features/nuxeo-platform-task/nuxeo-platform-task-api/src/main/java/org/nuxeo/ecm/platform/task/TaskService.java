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
 * Contributors: Laurent Doguin, Antoine Taillefer
 */
package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * The task service handle document based Tasks. You can create, search, accept,
 * end or reject tasks. An event is launched when a task is ended, hence giving
 * you the possibility to execute specific code.
 *
 * @author Laurent Doguin
 * @since 5.5
 */
public interface TaskService extends Serializable, TaskProvider {

    /**
     * Property used to pass task in the notified events properties
     */
    String TASK_INSTANCE_EVENT_PROPERTIES_KEY = "taskInstance";

    /**
     * The variable used as process instance variables.
     */
    enum VariableName {
        documentId, documentRepositoryName, endLifecycleTransition, initiator, document, principal, createdFromTaskService, directive, validated, right;
    }

    /**
     * Creates a task and starts it. Notifies events with names
     * {@link TaskEventNames#WORKFLOW_TASK_ASSIGNED} and
     * {@link TaskEventNames#WORKFLOW_TASK_ASSIGNED}, passing the task in the
     * event properties using key {@link #TASK_INSTANCE_EVENT_PROPERTIES_KEY}
     *
     * @param coreSession the session to use when notifying
     * @param principal the principal marked as initiator of the task and used
     *            when notifying.
     * @param document the document to attach to the task.
     * @param taskName the task name.
     * @param prefixedActorIds the list of actor ids, prefixed with 'user:' or
     *            'group:'.
     * @param createOneTaskPerActor if true, one task will be created per actor,
     *            else a single task will be assigned to all actors.
     * @param directive the directive, put in the task variables.
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param dueDate the due date, set on the task instance
     * @param taskVariables additional task variables
     * @param parentPath /task-root if null
     * @throws ClientException
     */
    List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath)
            throws ClientException;

    /**
     * Creates a task and starts it. Notifies events with names
     * {@link TaskEventNames#WORKFLOW_TASK_ASSIGNED} and
     * {@link TaskEventNames#WORKFLOW_TASK_ASSIGNED}, passing the task in the
     * event properties using key {@link #TASK_INSTANCE_EVENT_PROPERTIES_KEY}
     *
     * @param coreSession the session to use when notifying
     * @param principal the principal marked as initiator of the task and used
     *            when notifying.
     * @param document the document to attach to the task.
     * @param taskName the task name.
     * @param taskType the task type.
     * @param processId the process ID linked to this task if any.
     * @param prefixedActorIds the list of actor ids, prefixed with 'user:' or
     *            'group:'.
     * @param createOneTaskPerActor if true, one task will be created per actor,
     *            else a single task will be assigned to all actors.
     * @param directive the directive, put in the task variables.
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param dueDate the due date, set on the task instance
     * @param taskVariables additional task variables
     * @param parentPath /task-root if null
     * @throws ClientException
     * @since 5.6
     */
    List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskName, String taskType, String processId,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath)
            throws ClientException;

    /**
     * Creates a task of the given document type and starts it. Notifies events with names
     * {@link TaskEventNames#WORKFLOW_TASK_ASSIGNED} and
     * {@link TaskEventNames#WORKFLOW_TASK_ASSIGNED}, passing the task in the
     * event properties using key {@link #TASK_INSTANCE_EVENT_PROPERTIES_KEY}
     * Also the map eventInfo is passed in the event properties 
     *
     * @param coreSession the session to use when notifying
     * @param principal the principal marked as initiator of the task and used
     *            when notifying.
     * @param document the document to attach to the task.
     * @param the task document type
     * @param taskName the task name.
     * @param taskType the task type.
     * @param processId the process ID linked to this task if any.
     * @param prefixedActorIds the list of actor ids, prefixed with 'user:' or
     *            'group:'.
     * @param createOneTaskPerActor if true, one task will be created per actor,
     *            else a single task will be assigned to all actors.
     * @param directive the directive, put in the task variables.
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param dueDate the due date, set on the task instance
     * @param taskVariables additional task variables
     * @param parentPath /task-root if null
     * @param eventInfo
     * @throws ClientException
     * @since 5.6
     */
    List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskDocumentType, String taskName, String taskType, String processId,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath, Map<String, Serializable> eventInfo)
            throws ClientException;


    /**
     * Returns true if user is an administrator, the initiator of the task, or
     * an actor of the task.
     *
     * @throws ClientException
     */
    boolean canEndTask(NuxeoPrincipal principal, Task task)
            throws ClientException;

    /**
     * Ends the task using event name
     * {@link TaskEventNames#WORKFLOW_TASK_COMPLETED} and marking the task as
     * validated.
     *
     * @see #endTask(CoreSession, NuxeoPrincipal, Task, String, String, boolean)
     * @return the name of the Seam event to raise
     * @throws ClientException
     */
    String acceptTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment) throws ClientException;

    /**
     * Ends the task using event name
     * {@link TaskEventNames#WORKFLOW_TASK_REJECTED} and marking the task as not
     * validated.
     *
     * @see #endTask(CoreSession, NuxeoPrincipal, Task, String, String, boolean)
     * @return the name of the Seam event to raise
     * @throws ClientException
     */
    String rejectTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment) throws ClientException;

    /**
     * Ends the task
     *
     * @param coreSession the session to use when notifying and resolving of
     *            referenced document for notification.
     * @param principal principal used when notifying
     * @param task the instance to end
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param eventName the core event name to use when notifying
     * @param isValidated boolean marker to state if the task was validated or
     *            rejected
     * @throws ClientException when trying to end a task without being granted
     *             the right to do so (see
     *             {@link #canEndTask(NuxeoPrincipal, Task)}), or when any other
     *             error occurs
     * @return the name of the Seam event to raise
     */
    @Override
    String endTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment, String eventName, boolean isValidated)
            throws ClientException;

    /**
     * Remove the documentTask identified by the given taskId if coreSession's
     * principal has the Remove permission.
     *
     * @param coreSession
     * @param taskId
     * @throws ClientException
     * @Since 5.5
     */
    void deleteTask(CoreSession coreSession, String taskId)
            throws ClientException;

    /**
     *
     * @param ti the task.
     * @param user the user.
     * @return the task's target document.
     */
    DocumentModel getTargetDocumentModel(Task ti, CoreSession coreSession)
            throws ClientException;

    /**
     *
     * @param coreSession
     * @param taskId
     * @return the taskDocument with the given taskId
     * @throws ClientException
     */
    Task getTask(CoreSession coreSession, String taskId) throws ClientException;

    /**
     * Default value is /task-root
     *
     * @return the path registered in the taskPersister extension point.
     */
    String getTaskRootParentPath(CoreSession coreSession);
}
