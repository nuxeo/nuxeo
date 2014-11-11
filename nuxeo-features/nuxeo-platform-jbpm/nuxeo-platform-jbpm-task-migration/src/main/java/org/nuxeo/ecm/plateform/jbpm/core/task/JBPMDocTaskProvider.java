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
 *    Nuxeo
 */
package org.nuxeo.ecm.plateform.jbpm.core.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class JBPMDocTaskProvider implements TaskProvider {

    private static final long serialVersionUID = 1L;

    protected final static Log log = LogFactory.getLog(JBPMDocTaskProvider.class);

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
                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(tis, context, coreSession);
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

                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(tis, context, coreSession);
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
                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(tis, context, coreSession);
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
                        TaskMigrationRunner migrationRunner = new TaskMigrationRunner(tis, context, coreSession);
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
