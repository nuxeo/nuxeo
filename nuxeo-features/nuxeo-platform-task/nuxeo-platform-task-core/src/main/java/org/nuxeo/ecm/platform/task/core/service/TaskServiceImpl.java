/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin, Antoine Taillefer
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
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

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.platform.task.core.TaskService");

    public static final String DEFAULT_TASK_PROVIDER = "documentTaskProvider";

    private static final String TASK_PROVIDER_XP = "taskProvider";

    private static final String TASK_PERSISTER_XP = "taskPersister";

    private Map<String, TaskProvider> tasksProviders;

    private String parentPath = "/task-root";

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        tasksProviders = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        tasksProviders = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(TASK_PROVIDER_XP)) {
            if (contribution instanceof TaskProviderDescriptor) {
                TaskProviderDescriptor taskProviderDescriptor = (TaskProviderDescriptor) contribution;
                String providerId = taskProviderDescriptor.getId();
                if (taskProviderDescriptor.isEnabled()) {
                    tasksProviders.put(providerId, taskProviderDescriptor.getNewInstance());
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
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
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
    public List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal, DocumentModel document,
            String taskName, List<String> actorIds, boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath) {
        return createTask(coreSession, principal, document, taskName, null, null, actorIds, createOneTaskPerActor,
                directive, comment, dueDate, taskVariables, parentPath);
    }

    /**
     * @since 5.6
     */
    @Override
    public List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal, DocumentModel document,
            String taskDocumentType, String taskName, String taskType, String processId, List<String> actorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath, Map<String, Serializable> eventInfo) {
        List<DocumentModel> docs = new ArrayList<>();
        docs.add(document);
        return createTaskForProcess(coreSession, principal, docs, taskDocumentType, taskName, taskType, processId, null,
                actorIds, createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath, eventInfo);
    }

    /**
     * @since 5.6
     */
    @Override
    public List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal, DocumentModel document,
            String taskName, String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        return createTask(coreSession, principal, document, TaskConstants.TASK_TYPE_NAME, taskName, taskType, processId,
                prefixedActorIds, createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath, null);
    }

    @Override
    public String acceptTask(CoreSession coreSession, NuxeoPrincipal principal, Task task, String comment) {
        return endTask(coreSession, principal, task, comment, TaskEventNames.WORKFLOW_TASK_COMPLETED, true);
    }

    @Override
    public String rejectTask(CoreSession coreSession, NuxeoPrincipal principal, Task task, String comment) {
        return endTask(coreSession, principal, task, comment, TaskEventNames.WORKFLOW_TASK_REJECTED, false);
    }

    /**
     * Use the task provider held by the {@link Task#TASK_PROVIDER_KEY} task variable to end the {@code task}. If null
     * use the {@link #DEFAULT_TASK_PROVIDER}.
     */
    @Override
    public String endTask(CoreSession coreSession, NuxeoPrincipal principal, Task task, String comment,
            String eventName, boolean isValidated) {

        if (!canEndTask(principal, task)) {
            throw new NuxeoException(String.format("User with id '%s' cannot end this task", principal.getName()));
        }
        String taskProviderId = task.getVariable(Task.TASK_PROVIDER_KEY);
        if (taskProviderId == null) {
            taskProviderId = DEFAULT_TASK_PROVIDER;
        }
        TaskProvider taskProvider = tasksProviders.get(taskProviderId);
        if (taskProvider == null) {
            throw new NuxeoException(String.format(
                    "No task provider registered, cannot end task. Please contribute at least the default task provider: %s.",
                    DEFAULT_TASK_PROVIDER));
        }
        return taskProvider.endTask(coreSession, principal, task, comment, eventName, isValidated);
    }

    @Override
    public boolean canEndTask(NuxeoPrincipal principal, Task task) {
        if (task != null && (!task.isCancelled() && !task.hasEnded())) {
            return principal.isAdministrator() || principal.getName().equals(task.getInitiator())
                    || isTaskAssignedToUser(task, principal, true);
        }
        return false;
    }

    protected boolean isTaskAssignedToUser(Task task, NuxeoPrincipal user, boolean checkDelegatedActors) {
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
            if (checkDelegatedActors) {
                users.addAll(task.getDelegatedActors());
            }
            if (users != null) {
                for (String userName : users) {
                    if (userName.startsWith(NuxeoPrincipal.PREFIX)) {
                        if (actors.contains(userName.substring(NuxeoPrincipal.PREFIX.length()))) {
                            return true;
                        }
                    } else if (userName.startsWith(NuxeoGroup.PREFIX)) {
                        if (actors.contains(userName.substring(NuxeoGroup.PREFIX.length()))) {
                            return true;
                        }
                    } else if (actors.contains(userName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Task getTask(CoreSession coreSession, String taskId) {
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
    public void deleteTask(CoreSession coreSession, String taskId) {
        final DocumentRef docRef = new IdRef(taskId);
        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(coreSession) {
            @Override
            public void run() {
                session.removeDocument(docRef);
            }
        };
        runner.runUnrestricted();
    }

    @Override
    public DocumentModel getTargetDocumentModel(Task task, CoreSession coreSession) {
        try {
            // TODO handle while target documents from task
            return coreSession.getDocument(new IdRef(task.getTargetDocumentsIds().get(0)));
        } catch (DocumentNotFoundException e) {
            return null;
        }
    }

    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession) {
        List<Task> tasks = new ArrayList<>();
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
     * Provide @param sortInfo to handle sort page-provider contributions (see {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession, List<SortInfo> sortInfos) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getCurrentTaskInstances(coreSession, sortInfos);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getAllCurrentTaskInstances(CoreSession coreSession, List<SortInfo> sortInfos) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllCurrentTaskInstances(coreSession, sortInfos);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    /**
     * Returns a list of task instances assigned to one of the actors in the list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors, CoreSession coreSession) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getCurrentTaskInstances(actors, coreSession);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    /**
     * Provide @param sortInfo to handle sort page-provider contributions (see {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors, CoreSession coreSession, List<SortInfo> sortInfos) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getCurrentTaskInstances(actors, coreSession, sortInfos);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user, CoreSession coreSession) {
        List<Task> tasks = new ArrayList<>();
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
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors, CoreSession coreSession) {
        List<Task> tasks = new ArrayList<>();
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
    public List<Task> getAllTaskInstances(String processId, CoreSession session) {
        List<Task> tasks = new ArrayList<>();
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
    public List<Task> getAllTaskInstances(String processId, NuxeoPrincipal user, CoreSession session) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, user, session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, List<String> actors, CoreSession session) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, actors, session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public String getTaskRootParentPath(CoreSession coreSession) {
        GetTaskRootParentPathUnrestricted runner = new GetTaskRootParentPathUnrestricted(coreSession);
        runner.runUnrestricted();
        return runner.getParentPath();
    }

    public class GetTaskRootParentPathUnrestricted extends UnrestrictedSessionRunner {

        protected DocumentModel taskRootDoc;

        public GetTaskRootParentPathUnrestricted(CoreSession session) {
            super(session);
        }

        @Override
        public void run() {
            DocumentRef pathRef = new PathRef(parentPath);
            if (session.exists(pathRef)) {
                taskRootDoc = session.getDocument(pathRef);
            } else {
                Path path = new Path(parentPath);
                taskRootDoc = session.createDocumentModel(path.removeLastSegments(1).toString(), path.lastSegment(),
                        TaskConstants.TASK_ROOT_TYPE_NAME);
                taskRootDoc = session.createDocument(taskRootDoc);
                ACP acp = taskRootDoc.getACP();
                ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
                acl.add(new ACE("Everyone", "Everything", false));
                taskRootDoc.setACP(acp, true);
                taskRootDoc = session.saveDocument(taskRootDoc);
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
    public List<Task> getAllTaskInstances(String processId, String nodeId, CoreSession session) {
        List<Task> tasks = new ArrayList<>();
        List<Task> newTasks;
        for (TaskProvider taskProvider : tasksProviders.values()) {
            newTasks = taskProvider.getAllTaskInstances(processId, nodeId, session);
            if (newTasks != null) {
                tasks.addAll(newTasks);
            }
        }
        return tasks;
    }

    @Override
    public void reassignTask(CoreSession session, final String taskId, final List<String> newActors,
            final String comment) {

        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
                Task task = taskDoc.getAdapter(Task.class);
                if (task == null) {
                    throw new NuxeoException("Invalid taskId: " + taskId);
                }
                List<String> currentAssignees = task.getActors();
                List<String> currentActors = new ArrayList<>();
                for (String currentAssignee : currentAssignees) {
                    if (currentAssignee.startsWith(NotificationConstants.GROUP_PREFIX)
                            || currentAssignee.startsWith(NotificationConstants.USER_PREFIX)) {
                        // prefixed assignees with "user:" or "group:"
                        currentActors.add(currentAssignee.substring(currentAssignee.indexOf(":") + 1));
                    } else {
                        currentActors.add(currentAssignee);
                    }
                }
                String taskInitator = task.getInitiator();

                // remove ACLs set for current assignees
                ACP acp = taskDoc.getACP();
                ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
                List<ACE> toRemove = new ArrayList<>();

                for (ACE ace : acl.getACEs()) {
                    if (currentActors.contains(ace.getUsername()) || taskInitator.equals(ace.getUsername())) {
                        toRemove.add(ace);
                    }
                }
                acl.removeAll(toRemove);

                // grant EVERYTHING on task doc to the new actors
                List<String> actorIds = new ArrayList<>();
                for (String actor : newActors) {
                    if (actor.startsWith(NotificationConstants.GROUP_PREFIX)
                            || actor.startsWith(NotificationConstants.USER_PREFIX)) {
                        // prefixed assignees with "user:" or "group:"
                        actorIds.add(actor.substring(actor.indexOf(":") + 1));
                    } else {
                        actorIds.add(actor);
                    }
                }
                for (String actorId : actorIds) {
                    acl.add(new ACE(actorId, SecurityConstants.EVERYTHING, true));
                }

                taskDoc.setACP(acp, true);
                task.setActors(actorIds);
                String currentUser = ((NuxeoPrincipal) session.getPrincipal()).getActingUser();
                task.addComment(currentUser, comment);
                session.saveDocument(taskDoc);

                List<DocumentModel> docs = new ArrayList<>();
                for (String string : task.getTargetDocumentsIds()) {
                    docs.add(session.getDocument(new IdRef(string)));

                }
                notifyEvent(session, task, docs, TaskEventNames.WORKFLOW_TASK_REASSIGNED, new HashMap<>(), comment,
                        (NuxeoPrincipal) session.getPrincipal(), actorIds);

            }
        }.runUnrestricted();

    }

    @Override
    public void delegateTask(CoreSession session, final String taskId, final List<String> delegatedActors,
            final String comment) {

        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
                Task task = taskDoc.getAdapter(Task.class);
                if (task == null) {
                    throw new NuxeoException("Invalid taskId: " + taskId);
                }
                // grant EVERYTHING on task doc to the delegated actors
                List<String> actorIds = new ArrayList<>();
                ACP acp = taskDoc.getACP();
                ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);

                for (String actor : delegatedActors) {
                    if (actor.startsWith(NotificationConstants.GROUP_PREFIX)
                            || actor.startsWith(NotificationConstants.USER_PREFIX)) {
                        // prefixed assignees with "user:" or "group:"
                        actorIds.add(actor.substring(actor.indexOf(":") + 1));
                    } else {
                        actorIds.add(actor);
                    }
                }
                for (String actorId : actorIds) {
                    ACE ace = new ACE(actorId, SecurityConstants.EVERYTHING, true);
                    if (!acl.contains(ace)) {
                        acl.add(ace);
                    }
                }
                taskDoc.setACP(acp, true);

                List<String> allDelegatedActors = new ArrayList<>();
                allDelegatedActors.addAll(task.getDelegatedActors());
                for (String actor : actorIds) {
                    if (!allDelegatedActors.contains(actor)) {
                        allDelegatedActors.add(actor);
                    }
                }
                task.setDelegatedActors(allDelegatedActors);

                String currentUser = ((NuxeoPrincipal) session.getPrincipal()).getActingUser();
                task.addComment(currentUser, comment);
                session.saveDocument(taskDoc);
                List<DocumentModel> docs = new ArrayList<>();
                for (String string : task.getTargetDocumentsIds()) {
                    docs.add(session.getDocument(new IdRef(string)));

                }
                notifyEvent(session, task, docs, TaskEventNames.WORKFLOW_TASK_DELEGATED, new HashMap<>(),
                        String.format("Task delegated by '%s' to '%s'", currentUser, StringUtils.join(actorIds, ","))
                                + (!StringUtils.isEmpty(comment) ? " with the following comment: " + comment : ""),
                        (NuxeoPrincipal) session.getPrincipal(), actorIds);
            }

        }.runUnrestricted();
    }

    protected void notifyEvent(CoreSession session, Task task, List<DocumentModel> docs, String event,
            Map<String, Serializable> eventInfo, String comment, NuxeoPrincipal principal, List<String> actorIds) {
        Map<String, Serializable> eventProperties = new HashMap<>();
        ArrayList<String> notificationRecipients = new ArrayList<>();
        notificationRecipients.addAll(actorIds);
        eventProperties.put(NotificationConstants.RECIPIENTS_KEY,
                notificationRecipients.toArray(new String[notificationRecipients.size()]));
        if (eventInfo != null) {
            eventProperties.putAll(eventInfo);
        }
        for (DocumentModel doc : docs) {
            TaskEventNotificationHelper.notifyEvent(session, doc, principal, task, event, eventProperties, comment,
                    null);
        }
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors, boolean includeDelegatedTasks,
            CoreSession session) {
        List<Task> tasks = new ArrayList<>();
        for (TaskProvider taskProvider : tasksProviders.values()) {
            tasks.addAll(taskProvider.getTaskInstances(dm, actors, includeDelegatedTasks, session));
        }
        return tasks;
    }

    /**
     * @since 5.8
     * @deprecated since 7.4 use
     *             {@link #createTaskForProcess(CoreSession, NuxeoPrincipal, List, String, String, String, String, String, List, boolean, String, String, Date, Map, String, Map)}
     *             instead
     */
    @Override
    @Deprecated
    public List<Task> createTask(CoreSession coreSession, NuxeoPrincipal principal, List<DocumentModel> documents,
            String taskDocumentType, String taskName, String taskType, String processId, List<String> actorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath, Map<String, Serializable> eventInfo) {
        return createTaskForProcess(coreSession, principal, documents, taskDocumentType, taskName, taskType, processId,
                null, actorIds, createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath,
                eventInfo);
    }

    /**
     * @since 7.4
     */
    @Override
    public List<Task> createTaskForProcess(CoreSession coreSession, NuxeoPrincipal principal,
            List<DocumentModel> documents, String taskDocumentType, String taskName, String taskType, String processId,
            String processName, List<String> actorIds, boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath, Map<String, Serializable> eventInfo) {
        if (StringUtils.isBlank(parentPath)) {
            parentPath = getTaskRootParentPath(coreSession);
        }
        CreateTaskUnrestricted runner = new CreateTaskUnrestricted(coreSession, principal, documents, taskDocumentType,
                taskName, taskType, processId, processName, actorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath);
        runner.runUnrestricted();

        List<Task> tasks = runner.getTasks();

        for (Task task : tasks) {
            // notify
            notifyEvent(coreSession, task, documents, TaskEventNames.WORKFLOW_TASK_ASSIGNED, eventInfo, comment,
                    principal, task.getActors());
        }
        return tasks;
    }
}
