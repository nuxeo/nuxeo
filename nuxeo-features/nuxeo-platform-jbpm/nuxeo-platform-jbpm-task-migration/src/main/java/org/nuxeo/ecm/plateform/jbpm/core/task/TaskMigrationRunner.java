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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.plateform.jbpm.core.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.automation.task.CreateTask;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.service.CreateTaskUnrestricted;
import org.nuxeo.runtime.api.Framework;

/**
 * Encapsulate migration of task from JBPM to DocumentModel
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author ataillefer (ataillefer@nuxeo.com)
 *
 */
public class TaskMigrationRunner extends UnrestrictedSessionRunner {

    protected final List<TaskInstance> tis;

    protected List<Task> tasks;

    protected final JbpmContext context;

    protected static final Log log = LogFactory.getLog(TaskMigrationRunner.class);

    protected static Boolean migrate = null;

    public static final String TASK_MIGRATION_PROPERTY = "org.nuxeo.ecm.platform.jbpm.task.migrate";

    protected boolean needToMigrate() {
        if (migrate == null) {
            String flag = Framework.getProperty(TASK_MIGRATION_PROPERTY, "true");
            migrate = Boolean.parseBoolean(flag);
        }
        return migrate;
    }

    protected TaskMigrationRunner(List<TaskInstance> tis, JbpmContext context,
            CoreSession session) {
        super(session);
        this.tis = tis;
        tasks = new ArrayList<Task>(tis.size());
        this.context = context;
    }

    public List<Task> migrate() throws ClientException {
        if (tis.size() == 0) {
            return new ArrayList<Task>();
        }
        this.runUnrestricted();
        return tasks;
    }

    @Override
    public void run() throws ClientException {
        for (TaskInstance taskInstance : tis) {
            Task migratedTask = null;
            if (needToMigrate()) {
                try {
                    migratedTask = migrateJBPMtoDocTask(taskInstance, context,
                            session);
                } catch (ClientException e) {
                    log.error("Error while migrating task", e);
                }
            }
            if (migratedTask == null) {
                migratedTask = new JBPMTaskWrapper(taskInstance);
            }
            tasks.add(migratedTask);
        }
    }

    protected Task migrateJBPMtoDocTask(TaskInstance ti, JbpmContext context,
            CoreSession coreSession) throws ClientException {
        ti = context.getTaskInstance(ti.getId());
        JBPMDocTaskProvider.eagerLoadTaskInstance(ti);
        String commentId = (String) ti.getVariable("commentId");
        String taskName = ti.getName();
        if (commentId != null) {
            return migrateCommentTask(ti, context, coreSession);
        } else if (JBPMDocTaskProvider.PUBLISHER_JBPMTASK_NAME.equals(taskName)) {
            return migratePublisherTask(ti, context, coreSession);
        } else {
            if (ti.getProcessInstance() != null) {
                // don't migrate tasks associated to a process
                return new JBPMTaskWrapper(ti);
            } else {
                return migrateGenericTask(ti, context, coreSession, null);
            }
        }
    }

    private Task migrateGenericTask(TaskInstance ti, JbpmContext context,
            CoreSession coreSession, Map<String, String> vars)
            throws ClientException {
        if (vars == null) {
            vars = new HashMap<String, String>();
        }
        // Kepp all task instance variables
        vars.putAll(ti.getVariables());
        // Task name needs to be translated in the My Tasks dashboard gadget
        vars.put(Task.TaskVariableName.needi18n.name(), "true");
        Set<PooledActor> pooledActors = ti.getPooledActors();
        List<String> actors = new ArrayList<String>();
        for (PooledActor pooledActor : pooledActors) {
            actors.add(pooledActor.getActorId());
        }
        String docId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
        DocumentModel doc = null;
        if (docId != null) {
            DocumentRef idRef = new IdRef(docId);
            doc = coreSession.getDocument(idRef);
        }
        String prefixedInitiator = (String) ti.getVariable("initiator");
        String userId = prefixedInitiator;
        if (prefixedInitiator.contains(":")) {
            userId = prefixedInitiator.split(":")[1];
        }

        TaskService taskService = Framework.getLocalService(TaskService.class);
        NuxeoPrincipal user = new UserPrincipal(userId, null, false, false);
        String parentPath = taskService.getTaskRootParentPath(coreSession);

        String directive = (String) ti.getVariable(JbpmService.TaskVariableName.directive.name());
        String comment = "";
        if (ti.getComments().size() > 0) {
            Comment jbpmComment = (Comment) ti.getComments().get(0);
            comment = jbpmComment.getMessage();
        }
        String taskName = ti.getName();
        // Migrate publisher task name
        if (JBPMDocTaskProvider.PUBLISHER_JBPMTASK_NAME.equals(taskName)) {
            taskName = JBPMDocTaskProvider.PUBLISHER_TASK_NAME;
        }
        CreateTaskUnrestricted runner = new CreateTaskUnrestricted(coreSession,
                user, doc, taskName, actors, false, directive, comment,
                ti.getDueDate(), vars, parentPath);
        runner.runUnrestricted();
        List<Task> tasks = runner.getTasks();
        ti.suspend();
        if (tasks != null && tasks.size() > 0) {
            return tasks.get(0);
        } else {
            return null;
        }
    }

    private Task migrateCommentTask(TaskInstance ti, JbpmContext context,
            CoreSession coreSession) throws ClientException {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(
                CreateTask.OperationTaskVariableName.acceptOperationChain.name(),
                CommentsConstants.ACCEPT_CHAIN_NAME);
        vars.put(
                CreateTask.OperationTaskVariableName.rejectOperationChain.name(),
                CommentsConstants.REJECT_CHAIN_NAME);
        vars.putAll(ti.getVariables());

        return migrateGenericTask(ti, context, coreSession, vars);
    }

    private Task migratePublisherTask(TaskInstance ti, JbpmContext context,
            CoreSession coreSession) throws ClientException {

        // Add taskType var, needed to filter tasks in single tasks widget
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(Task.TaskVariableName.taskType.name(),
                CoreProxyWithWorkflowFactory.PUBLISH_TASK_TYPE);

        return migrateGenericTask(ti, context, coreSession, vars);
    }

}
