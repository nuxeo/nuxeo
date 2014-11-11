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
 *    Nuxeo, Antoine Taillefer
 */
package org.nuxeo.ecm.plateform.jbpm.core.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.core.helper.AbandonProcessUnrestricted;
import org.nuxeo.ecm.platform.jbpm.core.helper.EndProcessUnrestricted;
import org.nuxeo.ecm.platform.jbpm.operations.AddCommentOperation;
import org.nuxeo.ecm.platform.jbpm.operations.GetRecipientsForTaskOperation;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class JBPMDocTaskProvider implements TaskProvider {

    private static final long serialVersionUID = 1L;

    protected final static Log log = LogFactory.getLog(JBPMDocTaskProvider.class);

    public static final String PUBLISHER_JBPMTASK_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    public static final String PUBLISHER_TASK_NAME = "org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory";

    /** @since 5.6 */
    public static final String WORKFLOW_REJECT_TRANSITION = "reject";

    private JbpmService jbpmService;

    private TaskService taskService;

    private UserManager userManager;

    public static void eagerLoadTaskInstance(TaskInstance ti) {
        if (ti.getPooledActors() != null) {
            ti.getPooledActors().size();
        }
        if (ti.getVariableInstances() != null) {
            ti.getVariableInstances().size();
        }
        if (ti.getComments() != null) {
            ti.getComments().size();
        }
        if (ti.getToken() != null) {
            ti.getToken().getId();
        }
    }

    @Override
    public List<Task> getCurrentTaskInstances(final CoreSession coreSession)
            throws ClientException {
        @SuppressWarnings("unchecked")
        List<Task> migratedTasks = (List<Task>) getJbpmService().executeJbpmOperation(
                new JbpmOperation() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Serializable run(JbpmContext context)
                            throws NuxeoJbpmException {

                        List<TaskInstance> tis = getJbpmService().getCurrentTaskInstances(
                                (NuxeoPrincipal) coreSession.getPrincipal(),
                                null);
                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(
                                tis, context, coreSession);
                        try {
                            return (Serializable) migrationRunner.migrate();
                        } catch (ClientException e) {
                            log.error("Unable to migrate task", e);
                            return new ArrayList<Task>();
                        }
                    }
                });
        return migratedTasks;
    }

    @Override
    public List<Task> getCurrentTaskInstances(final List<String> actors,
            final CoreSession coreSession) throws ClientException {
        @SuppressWarnings("unchecked")
        List<Task> migratedTasks = (List<Task>) getJbpmService().executeJbpmOperation(
                new JbpmOperation() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Serializable run(JbpmContext context)
                            throws NuxeoJbpmException {

                        List<TaskInstance> tis = getJbpmService().getCurrentTaskInstances(
                                actors, null);

                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(
                                tis, context, coreSession);
                        try {
                            return (Serializable) migrationRunner.migrate();
                        } catch (ClientException e) {
                            log.error("Unable to migrate task", e);
                            return new ArrayList<Task>();
                        }
                    }
                });
        return migratedTasks;
    }

    @Override
    public List<Task> getTaskInstances(final DocumentModel dm,
            final NuxeoPrincipal user, final CoreSession coreSession)
            throws ClientException {
        @SuppressWarnings("unchecked")
        List<Task> migratedTasks = (List<Task>) getJbpmService().executeJbpmOperation(
                new JbpmOperation() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Serializable run(final JbpmContext context)
                            throws NuxeoJbpmException {

                        final List<TaskInstance> tis = getJbpmService().getTaskInstances(
                                dm, user, null);
                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(
                                tis, context, coreSession);
                        try {
                            return (Serializable) migrationRunner.migrate();
                        } catch (ClientException e) {
                            log.error("Unable to migrate task", e);
                            return new ArrayList<Task>();
                        }
                    }
                });
        return migratedTasks;
    }

    @Override
    public List<Task> getTaskInstances(final DocumentModel dm,
            final List<String> actors, final CoreSession coreSession)
            throws ClientException {
        @SuppressWarnings("unchecked")
        List<Task> migratedTasks = (List<Task>) getJbpmService().executeJbpmOperation(
                new JbpmOperation() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Serializable run(JbpmContext context)
                            throws NuxeoJbpmException {
                        List<TaskInstance> tis = getJbpmService().getTaskInstances(
                                dm, actors, null);
                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(
                                tis, context, coreSession);
                        try {
                            return (Serializable) migrationRunner.migrate();
                        } catch (ClientException e) {
                            log.error("Unable to migrate task", e);
                            return new ArrayList<Task>();
                        }
                    }
                });
        return migratedTasks;
    }

    public String endTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment, String eventName, boolean isValidated)
            throws ClientException {

        String seamEventName = null;
        if (task != null) {

            long taskId = Long.valueOf(task.getId());
            DocumentModel taskDoc = task.getDocument();
            String transition = isValidated ? null : WORKFLOW_REJECT_TRANSITION;

            Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
            taskVariables.put(JbpmService.TaskVariableName.validated.name(),
                    String.valueOf(isValidated));

            Map<String, Serializable> transientVariables = new HashMap<String, Serializable>();
            transientVariables.put(JbpmService.VariableName.document.name(),
                    taskDoc);
            transientVariables.put(JbpmService.VariableName.principal.name(),
                    principal);

            try {
                getJbpmService().endTask(taskId, transition, taskVariables,
                        null, transientVariables, principal);
            } catch (JbpmException jbpme) {
                // Exception caught, means the task could not be ended leaving
                // the task-node with the given transition.
                // Canceling current process.
                log.info(String.format(
                        "This task cannot be ended leaving the task-node with the transition '%s' => canceling the process.",
                        transition));
                return cancelCurrentProcess(coreSession, taskDoc, principal,
                        comment);
            }

            if (comment != null && !"".equals(comment)) {
                AddCommentOperation addCommentOperation = new AddCommentOperation(
                        taskId, NuxeoPrincipal.PREFIX + principal.getName(),
                        comment);
                getJbpmService().executeJbpmOperation(addCommentOperation);
            }

            coreSession.save(); // process invalidations from handlers' sessions

            Set<String> recipients = getRecipientsFromTask(taskId);
            notifyEventListeners(eventName, comment,
                    recipients.toArray(new String[] {}), coreSession,
                    principal, taskDoc);
            seamEventName = isValidated ? TaskEventNames.WORKFLOW_TASK_COMPLETED
                    : TaskEventNames.WORKFLOW_TASK_REJECTED;
        }
        return seamEventName;
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getRecipientsFromTask(final long taskId)
            throws NuxeoJbpmException {
        GetRecipientsForTaskOperation operation = new GetRecipientsForTaskOperation(
                taskId);
        return (Set<String>) getJbpmService().executeJbpmOperation(operation);

    }

    protected void notifyEventListeners(String name, String comment,
            String[] recipients, CoreSession coreSession,
            NuxeoPrincipal principal, DocumentModel doc) throws ClientException {
        getJbpmService().notifyEventListeners(name, comment, recipients,
                coreSession, principal, doc);
    }

    protected String cancelCurrentProcess(CoreSession coreSession,
            DocumentModel doc, NuxeoPrincipal principal, String comment)
            throws ClientException {

        String seamEventName = null;
        ProcessInstance currentProcess = getCurrentProcess(doc, principal);
        if (currentProcess != null) {
            // remove wf acls
            Long pid = Long.valueOf(currentProcess.getId());
            if (doc != null) {
                AbandonProcessUnrestricted runner = new AbandonProcessUnrestricted(
                        coreSession, doc.getRef(), pid);
                runner.runUnrestricted();
            }

            // end process and tasks using unrestricted session
            List<TaskInstance> tis = getJbpmService().getTaskInstances(
                    coreSession.getDocument(doc.getRef()),
                    (NuxeoPrincipal) null, null);

            EndProcessUnrestricted endProcessRunner = new EndProcessUnrestricted(
                    coreSession, tis);
            endProcessRunner.runUnrestricted();

            getJbpmService().deleteProcessInstance(principal, pid);

            notifyEventListeners(JbpmEventNames.WORKFLOW_CANCELED, comment,
                    endProcessRunner.getRecipients().toArray(new String[] {}),
                    coreSession, principal, doc);
            seamEventName = JbpmEventNames.WORKFLOW_CANCELED;
        }
        return seamEventName;
    }

    protected ProcessInstance getCurrentProcess(DocumentModel doc,
            NuxeoPrincipal principal) throws ClientException {
        ProcessInstance currentProcess = null;
        List<ProcessInstance> processes = getJbpmService().getProcessInstances(
                doc, principal, null);
        if (processes != null && !processes.isEmpty()) {
            currentProcess = processes.get(0);
        }
        return currentProcess;
    }

    public JbpmService getJbpmService() {
        if (jbpmService == null) {
            try {
                jbpmService = Framework.getService(JbpmService.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Jbpm service is not deployed.", e);
            }
        }
        return jbpmService;
    }

    public TaskService getTaskService() {
        if (taskService == null) {
            try {
                taskService = Framework.getService(TaskService.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Task service is not deployed.", e);
            }
        }
        return taskService;
    }

    public UserManager getUserManager() {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "UserManager service is not deployed.", e);
            }
        }
        return userManager;
    }
}
