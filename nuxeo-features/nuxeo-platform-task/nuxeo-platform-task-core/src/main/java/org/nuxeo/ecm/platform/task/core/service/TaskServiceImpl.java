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
 *     ldoguin, Antoine Taillefer
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.task.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskPersisterDescriptor;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskProviderDescriptor;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @since 5.5
 */
public class TaskServiceImpl extends DefaultComponent implements TaskService {

    private static final long serialVersionUID = 1L;

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.task.core.TaskService");

    public static final String DEFAULT_TASK_PROVIDER = "documentTaskProvider";

    private static final String TASK_PROVIDER_XP = "taskProvider";

    private static final String TASK_PERSISTER_XP = "taskPersister";

    private Map<String, TaskProvider> tasksProviders;

    private String parentPath = "/task-root";

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        tasksProviders = new HashMap<String, TaskProvider>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        tasksProviders = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(TASK_PROVIDER_XP)) {
            if (contribution instanceof TaskProviderDescriptor) {
                TaskProviderDescriptor taskProviderDescriptor = (TaskProviderDescriptor) contribution;
                String providerId = taskProviderDescriptor.getId();
                if (taskProviderDescriptor.isEnabled()) {
                    tasksProviders.put(providerId,
                            taskProviderDescriptor.getNewInstance());
                } else {
                    if (tasksProviders.get(providerId) != null) {
                        tasksProviders.remove(providerId);
                    }
                }
            }
        } else if (extensionPoint.equals(TASK_PERSISTER_XP)) {
            if (contribution instanceof TaskPersisterDescriptor) {
                TaskPersisterDescriptor taskPersisterDescriptor = (TaskPersisterDescriptor) contribution;
                parentPath = taskPersisterDescriptor.getPath();
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(TASK_PROVIDER_XP)) {
            if (contribution instanceof TaskProviderDescriptor) {
                TaskProviderDescriptor taskProviderDescriptor = (TaskProviderDescriptor) contribution;
                String providerId = taskProviderDescriptor.getId();
                if (tasksProviders.get(providerId) != null) {
                    tasksProviders.remove(providerId);
                }
            }
        }
    }

    @Override
    public List<Task> createTask(CoreSession coreSession,
            NuxeoPrincipal principal, DocumentModel document, String taskName,
            List<String> actorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath)
            throws ClientException {
        return createTask(coreSession, principal, document, taskName, null,
                null, actorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath);
    }

    /**
     * @since 5.6
     */
    @Override
    public List<Task> createTask(CoreSession coreSession,
            NuxeoPrincipal principal, DocumentModel document,
            String taskDocumentType, String taskName, String taskType,
            String processId, List<String> actorIds,
            boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath,
            Map<String, Serializable> eventInfo) throws ClientException {
        if (StringUtils.isBlank(parentPath)) {
            parentPath = getTaskRootParentPath(coreSession);
        }
        CreateTaskUnrestricted runner = new CreateTaskUnrestricted(coreSession,
                principal, document, taskDocumentType, taskName, taskType,
                processId, actorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath);
        runner.runUnrestricted();

        List<Task> tasks = runner.getTasks();

        for (Task task : tasks) {
            // notify
            Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
            ArrayList<String> notificationRecipients = new ArrayList<String>();
            notificationRecipients.addAll(actorIds);
            if (principal != null) {
                if (!notificationRecipients.contains(NuxeoPrincipal.PREFIX
                        + principal.getName())) {
                    notificationRecipients.add(NuxeoPrincipal.PREFIX
                            + principal.getName());
                }
            }
            eventProperties.put(
                    NotificationConstants.RECIPIENTS_KEY,
                    notificationRecipients.toArray(new String[notificationRecipients.size()]));
            if (eventInfo != null) {
                eventProperties.putAll(eventInfo);
            }
            TaskEventNotificationHelper.notifyEvent(coreSession, document,
                    principal, task, TaskEventNames.WORKFLOW_TASK_ASSIGNED,
                    eventProperties, comment, null);
        }
        return tasks;
    }

    /**
     * @since 5.6
     */
    @Override
    public List<Task> createTask(CoreSession coreSession,
            NuxeoPrincipal principal, DocumentModel document, String taskName,
            String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath)
            throws ClientException {
        return createTask(coreSession, principal, document,
                TaskConstants.TASK_TYPE_NAME, taskName, taskType, processId,
                prefixedActorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath, null);
    }

    @Override
    public String acceptTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment) throws ClientException {
        return endTask(coreSession, principal, task, comment,
                TaskEventNames.WORKFLOW_TASK_COMPLETED, true);
    }

    @Override
    public String rejectTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment) throws ClientException {
        return endTask(coreSession, principal, task, comment,
                TaskEventNames.WORKFLOW_TASK_REJECTED, false);
    }

    /**
     * Use the task provider held by the {@link Task#TASK_PROVIDER_KEY} task
     * variable to end the {@code task}. If null use the
     * {@link #DEFAULT_TASK_PROVIDER}.
     */
    @Override
    public String endTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment, String eventName, boolean isValidated)
            throws ClientException {

        if (!canEndTask(principal, task)) {
            throw new ClientException(String.format(
                    "User with id '%s' cannot end this task",
                    principal.getName()));
        }
        String taskProviderId = task.getVariable(Task.TASK_PROVIDER_KEY);
        if (taskProviderId == null) {
            taskProviderId = DEFAULT_TASK_PROVIDER;
        }
        TaskProvider taskProvider = tasksProviders.get(taskProviderId);
        if (taskProvider == null) {
            throw new ClientException(
                    String.format(
                            "No task provider registered, cannot end task. Please contribute at least the default task provider: %s.",
                            DEFAULT_TASK_PROVIDER));
        }
        return taskProvider.endTask(coreSession, principal, task, comment,
                eventName, isValidated);
    }

    @Override
    public boolean canEndTask(NuxeoPrincipal principal, Task task)
            throws ClientException {
        if (task != null && (!task.isCancelled() && !task.hasEnded())) {
            return principal.isAdministrator()
                    || principal.getName().equals(task.getInitiator())
                    || isTaskAssignedToUser(task, principal);
        }
        return false;
    }

    protected boolean isTaskAssignedToUser(Task task, NuxeoPrincipal user)
            throws ClientException {
        if (task != null && user != null) {
            // user actors
            List<String> actors = user.getAllGroups();
            actors.add(user.getName());

            // initiator
            if (actors.contains(task.getInitiator())) {
                return true;
            }
            // users
            List<String> users = task.getActors();
            if (users != null) {
                for (String userName : users) {
                    if (userName.contains(":")) {
                        if (actors.contains(userName.split(":")[1])) {
                            return true;
                        }
                    }
                    if (actors.contains(userName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Task getTask(CoreSession coreSession, String taskId)
            throws ClientException {
        DocumentRef docRef = new IdRef(taskId);
        DocumentModel taskDoc = coreSession.getDocument(docRef);
        if (taskDoc != null) {
            Task task = taskDoc.getAdapter(Task.class);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    @Override
    public void deleteTask(CoreSession coreSession, String taskId)
            throws ClientException {
        final DocumentRef docRef = new IdRef(taskId);
        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                coreSession) {
            @Override
            public void run() throws ClientException {
                session.removeDocument(docRef);
            }
        };
        runner.runUnrestricted();
    }

    @Override
    public DocumentModel getTargetDocumentModel(Task task,
            CoreSession coreSession) throws ClientException {
        return coreSession.getDocument(new IdRef(task.getTargetDocumentId()));
    }

    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession)
            throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getCurrentTaskInstances(coreSession);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    /**
     * Returns a list of task instances assigned to one of the actors in the
     * list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     * @return
     * @throws ClientException
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors,
            CoreSession coreSession) throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getCurrentTaskInstances(actors, coreSession);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            CoreSession coreSession) throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getTaskInstances(dm, user, coreSession);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            CoreSession coreSession) throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getTaskInstances(dm, actors, coreSession);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, CoreSession session)
            throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId,
            NuxeoPrincipal user, CoreSession session) throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, user,
                    session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId,
            List<String> actors, CoreSession session) throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, actors,
                    session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    /**
     * @deprecated since 5.6
     */
    @Deprecated
    protected List<Task> wrapDocModelInTask(DocumentModelList taskDocuments) {
        List<Task> tasks = new ArrayList<Task>();
        for (DocumentModel doc : taskDocuments) {
            tasks.add(doc.getAdapter(Task.class));
        }
        return tasks;
    }

    @Override
    public String getTaskRootParentPath(CoreSession coreSession) {
        GetTaskRootParentPathUnrestricted runner = new GetTaskRootParentPathUnrestricted(
                coreSession);
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        return runner.getParentPath();
    }

    public class GetTaskRootParentPathUnrestricted extends
            UnrestrictedSessionRunner {

        protected DocumentModel taskRootDoc;

        public GetTaskRootParentPathUnrestricted(CoreSession session) {
            super(session);
        }

        @Override
        public void run() throws ClientException {
            DocumentRef pathRef = new PathRef(parentPath);
            if (session.exists(pathRef)) {
                taskRootDoc = session.getDocument(pathRef);
            } else {
                Path path = new Path(parentPath);
                taskRootDoc = session.createDocumentModel(
                        path.removeLastSegments(1).toString(),
                        path.lastSegment(), TaskConstants.TASK_ROOT_TYPE_NAME);
                taskRootDoc = session.createDocument(taskRootDoc);
            }

        }

        public DocumentModel getTaskRootDoc() {
            return taskRootDoc;
        }

        public String getParentPath() {
            return taskRootDoc.getPathAsString();
        }
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, String nodeId,
            CoreSession session) throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, nodeId,
                    session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }
}