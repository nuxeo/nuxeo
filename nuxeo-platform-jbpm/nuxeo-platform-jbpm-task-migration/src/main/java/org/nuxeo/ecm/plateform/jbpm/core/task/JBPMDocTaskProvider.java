package org.nuxeo.ecm.plateform.jbpm.core.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.automation.task.CreateTask;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.service.CreateTaskUnrestricted;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class JBPMDocTaskProvider implements TaskProvider {

    private static final long serialVersionUID = 1L;

    public static final String PUBLISHER_JBPMTASK_NAME = "org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory";

    public static final String PUBLISHER_TASK_NAME = "org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory";

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

    public Task migrateJBPMtoDocTask(TaskInstance ti, JbpmContext context,
            CoreSession coreSession) throws ClientException {
        ti = context.getTaskInstance(ti.getId());
        eagerLoadTaskInstance(ti);
        String commentId = (String) ti.getVariable("commentId");
        String taskName = ti.getName();
        if (commentId != null) {
            return migrateCommentTask(ti, context, coreSession).get(0);
        } else if (PUBLISHER_JBPMTASK_NAME.equals(taskName)) {
            return migratePublisherTask(ti, context, coreSession).get(0);
        } else {
            return new JBPMTaskWrapper(ti);
        }
    }

    private List<Task> migrateCommentTask(TaskInstance ti, JbpmContext context,
            CoreSession coreSession) throws ClientException {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(
                CreateTask.OperationTaskVariableName.createdFromCreateTaskOperation.name(),
                "true");
        vars.put(
                CreateTask.OperationTaskVariableName.acceptOperationChain.name(),
                CommentsConstants.ACCEPT_CHAIN_NAME);
        vars.put(
                CreateTask.OperationTaskVariableName.rejectOperationChain.name(),
                CommentsConstants.REJECT_CHAIN_NAME);
        vars.putAll(ti.getVariables());
        Set<PooledActor> pooledActors = ti.getPooledActors();
        List<String> actors = new ArrayList<String>();
        for (PooledActor pooledActor : pooledActors) {
            actors.add(pooledActor.getActorId());
        }
        String docId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
        DocumentRef idRef = new IdRef(docId);
        DocumentModel doc = coreSession.getDocument(idRef);
        String prefixedInitiator = (String) ti.getVariable("initiator");
        String userId = prefixedInitiator.split(":")[1];
        NuxeoPrincipal user = getUserManager().getPrincipal(userId);
        String parentPath = getTaskService().getTaskRootParentPath(coreSession);

        CreateTaskUnrestricted runner = new CreateTaskUnrestricted(coreSession, user, doc,
                CommentsConstants.MODERATION_DIRECTIVE_NAME, actors, false,
                null, null, ti.getDueDate(), vars, parentPath);
        runner.runUnrestricted();
        List<Task> tasks = runner.getTasks();
        ti.suspend();
        ti.getProcessInstance().suspend();
        return tasks;
    }

    private List<Task> migratePublisherTask(TaskInstance ti,
            JbpmContext context, CoreSession coreSession)
            throws ClientException {
        String docId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
        DocumentRef idRef = new IdRef(docId);
        DocumentModel doc = coreSession.getDocument(idRef);
        String userId = (String) ti.getVariable("initiator");
        NuxeoPrincipal user = getUserManager().getPrincipal(userId);
        Set<PooledActor> pooledActors = ti.getPooledActors();
        List<String> actors = new ArrayList<String>();
        for (PooledActor pooledActor : pooledActors) {
            actors.add(pooledActor.getActorId());
        }

        String parentPath = getTaskService().getTaskRootParentPath(coreSession);
        CreateTaskUnrestricted runner = new CreateTaskUnrestricted(coreSession,
                user, doc, PUBLISHER_TASK_NAME, actors, false,
                null, null, ti.getDueDate(), ti.getVariables(), parentPath);
        runner.runUnrestricted();
        List<Task> tasks = runner.getTasks();
        ti.suspend();
        return tasks;
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
                        List<Task> tasks = new ArrayList<Task>(tis.size());
                        for (TaskInstance taskInstance : tis) {
                            try {
                                tasks.add(migrateJBPMtoDocTask(taskInstance,
                                        context, coreSession));
                            } catch (ClientException e) {
                                throw new NuxeoJbpmException(e);
                            }
                        }
                        return (Serializable) tasks;
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
                        List<Task> tasks = new ArrayList<Task>(tis.size());
                        for (TaskInstance taskInstance : tis) {
                            try {
                                tasks.add(migrateJBPMtoDocTask(taskInstance,
                                        context, coreSession));
                            } catch (ClientException e) {
                                throw new NuxeoJbpmException(e);
                            }
                        }
                        return (Serializable) tasks;
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
                    public Serializable run(JbpmContext context)
                            throws NuxeoJbpmException {

                        List<TaskInstance> tis = getJbpmService().getTaskInstances(
                                dm, user, null);
                        List<Task> tasks = new ArrayList<Task>(tis.size());
                        for (TaskInstance taskInstance : tis) {
                            try {
                                tasks.add(migrateJBPMtoDocTask(taskInstance,
                                        context, coreSession));
                            } catch (ClientException e) {
                                throw new NuxeoJbpmException(e);
                            }
                        }
                        return (Serializable) tasks;
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
                        List<Task> tasks = new ArrayList<Task>(tis.size());
                        for (TaskInstance taskInstance : tis) {
                            try {
                                tasks.add(migrateJBPMtoDocTask(taskInstance,
                                        context, coreSession));
                            } catch (ClientException e) {
                                throw new NuxeoJbpmException(e);
                            }
                        }
                        return (Serializable) tasks;
                    }
                });
        return migratedTasks;
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
