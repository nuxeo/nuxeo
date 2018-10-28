/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.task.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.platform.task.TaskService;

/**
 * @since 5.5
 */
public class CreateTaskUnrestricted extends UnrestrictedSessionRunner {

    private NuxeoPrincipal principal;

    private DocumentModel document;

    private String taskName;

    /**
     * @since 5.6
     */
    private String taskType;

    /**
     * @since 5.6
     */
    private String processId;

    /**
     * @since 5.6
     */
    private String taskDocumentType;

    private List<String> prefixedActorIds;

    private boolean createOneTaskPerActor;

    private String directive;

    private String comment;

    private Date dueDate;

    private Map<String, String> taskVariables;

    private String parentPath;

    /**
     * @since 7.4
     */
    private String processName;

    List<Task> tasks = new ArrayList<>();

    /**
     * @since 5.8 A task can have many target documents
     */
    protected List<DocumentModel> documents;

    public CreateTaskUnrestricted(CoreSession session, NuxeoPrincipal principal, DocumentModel document,
            String taskName, List<String> prefixedActorIds, boolean createOneTaskPerActor, String directive,
            String comment, Date dueDate, Map<String, String> taskVariables, String parentPath) {
        this(session, principal, document, taskName, null, null, prefixedActorIds, createOneTaskPerActor, directive,
                comment, dueDate, taskVariables, parentPath);
    }

    /**
     * @since 5.6
     */
    public CreateTaskUnrestricted(CoreSession session, NuxeoPrincipal principal, DocumentModel document,
            String taskName, String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        super(session);
        this.principal = principal;
        this.document = document;
        this.taskName = taskName;
        this.taskType = taskType;
        this.processId = processId;
        this.prefixedActorIds = prefixedActorIds;
        this.createOneTaskPerActor = createOneTaskPerActor;
        this.directive = directive;
        this.comment = comment;
        this.dueDate = dueDate;
        this.taskVariables = taskVariables;
        this.parentPath = parentPath;
        this.documents = new ArrayList<>();
        this.documents.add(document);
    }

    /**
     * @since 5.6
     */
    public CreateTaskUnrestricted(CoreSession session, NuxeoPrincipal principal, DocumentModel document,
            String taskDocumentType, String taskName, String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        this(session, principal, document, taskName, taskType, processId, prefixedActorIds, createOneTaskPerActor,
                directive, comment, dueDate, taskVariables, parentPath);
        this.taskDocumentType = taskDocumentType;
    }

    /**
     * @since 5.8
     */
    public CreateTaskUnrestricted(CoreSession session, NuxeoPrincipal principal, List<DocumentModel> documents,
            String taskDocumentType, String taskName, String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        this(session, principal, documents != null && documents.size() > 0 ? documents.get(0) : null, taskName,
                taskType, processId, prefixedActorIds, createOneTaskPerActor, directive, comment, dueDate,
                taskVariables, parentPath);
        this.taskDocumentType = taskDocumentType;
        this.documents = documents;
        if (this.documents != null && this.documents.size() > 0) {
            this.document = documents.get(0);
        }
    }

    /**
     * @since 7.4
     */
    public CreateTaskUnrestricted(CoreSession session, NuxeoPrincipal principal, List<DocumentModel> documents,
            String taskDocumentType, String taskName, String taskType, String processId, String processName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath) {
        this(session, principal, documents != null && documents.size() > 0 ? documents.get(0) : null, taskName,
                taskType, processId, prefixedActorIds, createOneTaskPerActor, directive, comment, dueDate,
                taskVariables, parentPath);
        this.taskDocumentType = taskDocumentType;
        this.documents = documents;
        this.processName = processName;
        if (this.documents != null && this.documents.size() > 0) {
            this.document = documents.get(0);
        }
    }

    @Override
    public void run() {
        if (StringUtils.isEmpty(taskDocumentType)) {
            taskDocumentType = TaskConstants.TASK_TYPE_NAME;
        }
        createTask(session, principal, documents, taskDocumentType, taskName, taskType, processId, processName,
                prefixedActorIds, createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath);
    }

    /**
     * @since 5.6
     */
    public void createTask(CoreSession coreSession, NuxeoPrincipal principal, DocumentModel document,
            String taskDocumentType, String taskName, String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        List<DocumentModel> docs = new ArrayList<>();
        docs.add(document);
        createTask(coreSession, principal, docs, taskDocumentType, taskName, taskType, processId, prefixedActorIds,
                createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath);

    }

    /**
     * @since 5.8
     */
    public void createTask(CoreSession coreSession, NuxeoPrincipal principal, List<DocumentModel> documents,
            String taskDocumentType, String taskName, String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        createTask(coreSession, principal, documents, taskDocumentType, taskName, taskType, processId, null,
                prefixedActorIds, createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath);
    }

    /**
     * @since 7.4
     */
    public void createTask(CoreSession coreSession, NuxeoPrincipal principal, List<DocumentModel> documents,
            String taskDocumentType, String taskName, String taskType, String processId, String processName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath) {
        if (createOneTaskPerActor) {
            for (String prefixedActorId : prefixedActorIds) {
                createTask(coreSession, principal, documents, taskDocumentType, taskName, taskType, processId,
                        Collections.singletonList(prefixedActorId), false, directive, comment, dueDate, taskVariables,
                        parentPath);
            }
        } else {
            // use task type as a docName (is actually the nodeId so it
            // doesn't contain "/" characters), but fallback on task name
            // if task type is null (for old API kept for compat)
            String docName = taskType == null ? taskName : taskType;
            DocumentModel taskDocument = session.createDocumentModel(parentPath, docName, taskDocumentType);
            Task task = taskDocument.getAdapter(Task.class);
            if (task == null) {
                throw new NuxeoException("Document " + taskDocumentType + "  can not be adapted to a Task");
            }
            task.setName(taskName);
            task.setType(taskType);
            task.setProcessId(processId);
            task.setProcessName(processName);
            task.setCreated(new Date());
            task.setInitiator(principal.getActingUser());
            task.setActors(prefixedActorIds);
            task.setDueDate(dueDate);

            if (documents != null) {
                List<String> docIds = new ArrayList<>();
                for (DocumentModel doc : documents) {
                    docIds.add(doc.getId());
                }
                task.setTargetDocumentsIds(docIds);
            }
            task.setDirective(directive);

            if (!StringUtils.isEmpty(comment)) {
                task.addComment(principal.getName(), comment);
            }

            // add variables
            Map<String, String> variables = new HashMap<>();
            if (document != null) {
                variables.put(TaskService.VariableName.documentId.name(), document.getId());
                variables.put(TaskService.VariableName.documentRepositoryName.name(), document.getRepositoryName());
            }
            variables.put(TaskService.VariableName.directive.name(), directive);
            variables.put(TaskService.VariableName.createdFromTaskService.name(), "true");
            if (taskVariables != null) {
                variables.putAll(taskVariables);
            }
            task.setVariables(variables);

            // create document in order to set its ACP
            taskDocument = session.createDocument(taskDocument);

            // re-fetch task from task document to set its id
            task = taskDocument.getAdapter(Task.class);

            // Set rights
            List<String> actorIds = new ArrayList<>();
            for (String actor : prefixedActorIds) {
                if (actor.startsWith(NotificationConstants.GROUP_PREFIX)
                        || actor.startsWith(NotificationConstants.USER_PREFIX)) {
                    // prefixed assignees with "user:" or "group:"
                    actorIds.add(actor.substring(actor.indexOf(":") + 1));
                } else {
                    actorIds.add(actor);
                }
            }
            ACP acp = taskDocument.getACP();
            ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
            if (principal != null) {
                acl.add(new ACE(principal.getName(), SecurityConstants.EVERYTHING, true));

            }
            for (String actorId : actorIds) {
                acl.add(new ACE(actorId, SecurityConstants.EVERYTHING, true));
            }
            acp.addACL(acl);
            taskDocument.setACP(acp, true);
            taskDocument = session.saveDocument(taskDocument);
            tasks.add(task);
        }
    }

    public void createTask(CoreSession coreSession, NuxeoPrincipal principal, DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath) {
        createTask(coreSession, principal, document, TaskConstants.TASK_TYPE_NAME, taskName, null, null,
                prefixedActorIds, createOneTaskPerActor, directive, comment, dueDate, taskVariables, parentPath);
    }

    public List<Task> getTasks() {
        return tasks;
    }

}
