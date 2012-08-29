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
package org.nuxeo.ecm.platform.task.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
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

    List<Task> tasks = new ArrayList<Task>();

    public CreateTaskUnrestricted(CoreSession session,
            NuxeoPrincipal principal, DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath) {
        this(session, principal, document, taskName, null, null,
                prefixedActorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath);
    }

    /**
     * @since 5.6
     */
    public CreateTaskUnrestricted(CoreSession session,
            NuxeoPrincipal principal, DocumentModel document, String taskName,
            String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath) {
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
    }

    /**
     * @since 5.6
     */
    public CreateTaskUnrestricted(CoreSession session,
            NuxeoPrincipal principal, DocumentModel document,
            String taskDocumentType, String taskName, String taskType,
            String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath) {
        this(session, principal, document, taskName, taskType, processId,
                prefixedActorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath);
        this.taskDocumentType = taskDocumentType;
    }

    @Override
    public void run() throws ClientException {
        if (StringUtils.isEmpty(taskDocumentType)) {
            taskDocumentType = TaskConstants.TASK_TYPE_NAME;
        }
        createTask(session, principal, document, taskDocumentType, taskName,
                taskType, processId, prefixedActorIds, createOneTaskPerActor,
                directive, comment, dueDate, taskVariables, parentPath);
    }

    /**
     * @since 5.6
     */
    public void createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskDocumentType, String taskName,
            String taskType, String processId, List<String> prefixedActorIds,
            boolean createOneTaskPerActor, String directive, String comment,
            Date dueDate, Map<String, String> taskVariables, String parentPath)
            throws ClientException {
        if (createOneTaskPerActor) {
            for (String prefixedActorId : prefixedActorIds) {
                createTask(coreSession, principal, document, taskName,
                        Collections.singletonList(prefixedActorId), false,
                        directive, comment, dueDate, taskVariables, parentPath);
            }
        } else {
            try {
                // use task type as a docName (is actually the nodeId so it
                // doesn't contain "/" characters), but fallback on task name
                // if task type is null (for old API kept for compat)
                String docName = taskType == null ? taskName : taskType;
                DocumentModel taskDocument = session.createDocumentModel(
                        parentPath, docName, taskDocumentType);
                Task task = taskDocument.getAdapter(Task.class);
                if (task == null) {
                    throw new ClientRuntimeException("Document "
                            + taskDocumentType
                            + "  can not be adapted to a Task");
                }
                task.setName(taskName);
                task.setType(taskType);
                task.setProcessId(processId);
                task.setCreated(new Date());
                if (principal != null) {
                    task.setInitiator(principal.getName());
                }
                task.setActors(prefixedActorIds);
                task.setDueDate(dueDate);
                if (document != null) {
                    task.setTargetDocumentId(document.getId());
                }
                task.setDirective(directive);

                if (!StringUtils.isEmpty(comment)) {
                    task.addComment(principal.getName(), comment);
                }

                // add variables
                Map<String, String> variables = new HashMap<String, String>();
                if (document != null) {
                    variables.put(TaskService.VariableName.documentId.name(),
                            document.getId());
                    variables.put(
                            TaskService.VariableName.documentRepositoryName.name(),
                            document.getRepositoryName());
                }
                variables.put(TaskService.VariableName.directive.name(),
                        directive);
                variables.put(
                        TaskService.VariableName.createdFromTaskService.name(),
                        "true");
                if (taskVariables != null) {
                    variables.putAll(taskVariables);
                }
                task.setVariables(variables);

                // create document in order to set its ACP
                taskDocument = session.createDocument(taskDocument);

                // re-fetch task from task document to set its id
                task = taskDocument.getAdapter(Task.class);

                // Set rights
                List<String> actorIds = new ArrayList<String>();
                for (String actor : prefixedActorIds) {
                    if (actor.contains(":")) {
                        actorIds.add(actor.split(":")[1]);
                    } else {
                        actorIds.add(actor);
                    }
                }
                ACP acp = taskDocument.getACP();
                ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
                if (principal != null) {
                    acl.add(new ACE(principal.getName(),
                            SecurityConstants.EVERYTHING, true));

                }
                for (String actorId : actorIds) {
                    acl.add(new ACE(actorId, SecurityConstants.EVERYTHING, true));
                }
                acp.addACL(acl);
                taskDocument.setACP(acp, true);
                taskDocument = session.saveDocument(taskDocument);
                tasks.add(task);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
        }
    }

    public void createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, String> taskVariables, String parentPath)
            throws ClientException {
        createTask(coreSession, principal, document,
                TaskConstants.TASK_TYPE_NAME, taskName, null, null,
                prefixedActorIds, createOneTaskPerActor, directive, comment,
                dueDate, taskVariables, parentPath);
    }

    public List<Task> getTasks() {
        return tasks;
    }

}
