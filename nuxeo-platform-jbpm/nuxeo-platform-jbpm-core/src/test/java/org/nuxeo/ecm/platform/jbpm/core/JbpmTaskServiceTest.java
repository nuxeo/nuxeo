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
package org.nuxeo.ecm.platform.jbpm.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.core.service.JbpmServiceImpl;
import org.nuxeo.ecm.platform.jbpm.test.JbpmUTConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
public class JbpmTaskServiceTest extends RepositoryOSGITestCase {

    protected JbpmService service;

    protected JbpmTaskService taskService;

    protected UserManager userManager;

    protected NuxeoPrincipal administrator;

    protected NuxeoPrincipal user1;

    protected NuxeoPrincipal user2;

    protected NuxeoPrincipal user3;

    protected NuxeoPrincipal user4;

    @Override
    public void setUp() throws Exception {
        // clean up previous test.
        JbpmServiceImpl.contexts.set(null);
        super.setUp();

        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployContrib("org.nuxeo.ecm.platform.jbpm.core.test",
                "OSGI-INF/jbpmService-contrib.xml");

        deployBundle(JbpmUTConstants.CORE_BUNDLE_NAME);
        deployBundle(JbpmUTConstants.TESTING_BUNDLE_NAME);

        service = Framework.getService(JbpmService.class);
        taskService = Framework.getService(JbpmTaskService.class);

        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);

        administrator = userManager.getPrincipal(SecurityConstants.ADMINISTRATOR);
        assertNotNull(administrator);

        user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        user3 = userManager.getPrincipal("myuser3");
        assertNotNull(user3);

        user4 = userManager.getPrincipal("myuser4");
        assertNotNull(user4);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        JbpmServiceImpl.contexts.set(null);
    }

    public void testSingleTaskWithAccept() throws Exception {
        DocumentModel document = getDocument();
        assertNotNull(document);

        // create task as admin
        List<String> actors = new ArrayList<String>();
        actors.add(NuxeoPrincipal.PREFIX + user1.getName());
        actors.add(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6);

        // create one task for all actors
        taskService.createTask(coreSession, user3, document, "Test Task Name",
                actors, false, "test directive", "test comment",
                calendar.getTime(), null);

        List<TaskInstance> tasks = service.getTaskInstances(document,
                (NuxeoPrincipal) null, null);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        TaskInstance task = tasks.get(0);
        assertEquals("Test Task Name", task.getName());
        assertEquals(null, task.getActorId());
        List<String> pooledActorIds = getPooledActorIds(task);
        assertEquals(2, pooledActorIds.size());
        assertEquals(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS,
                pooledActorIds.get(0));
        assertEquals(NuxeoPrincipal.PREFIX + user1.getName(),
                pooledActorIds.get(1));
        List<Comment> comments = task.getComments();
        assertEquals(1, comments.size());
        Comment comment = comments.get(0);
        assertEquals(user3.getName(), comment.getActorId());
        assertEquals("test comment", comment.getMessage());
        assertEquals(calendar.getTime(), task.getDueDate());
        assertNull(task.getProcessInstance());
        // task status
        assertTrue(task.isOpen());
        assertFalse(task.isCancelled());
        assertFalse(task.hasEnded());

        assertEquals(5, task.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(user3.getName(),
                task.getVariable(JbpmService.VariableName.initiator.name()));
        assertEquals("test directive",
                task.getVariable(JbpmService.TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        // test rights for each user
        // initiator or admin can end a task
        assertTrue(taskService.canEndTask(administrator, task));
        assertTrue(taskService.canEndTask(user3, task));
        // user 1 is in actors
        assertTrue(taskService.canEndTask(user1, task));
        // user 2 is in the members group
        assertTrue(taskService.canEndTask(user2, task));
        // user 4 is not in the members group
        assertFalse(taskService.canEndTask(user4, task));

        // test ending of the task

        try {
            taskService.acceptTask(coreSession, user4, task, "ok i'm in");
            fail("Should have raised an exception: user4 cannot end the task");
        } catch (NuxeoJbpmException e) {
            assertEquals("User with id 'myuser4' cannot end this task",
                    e.getMessage());
        }

        // accept task
        taskService.acceptTask(coreSession, user1, task, "ok i'm in");

        // test task again
        tasks = service.getTaskInstances(document, (NuxeoPrincipal) null, null);
        assertNotNull(tasks);
        // ended tasks are filtered
        assertEquals(0, tasks.size());

        // retrieve the task another way
        final Long taskId = task.getId();
        task = getTask(taskId);
        assertNotNull(task);

        assertEquals("Test Task Name", task.getName());
        assertEquals(null, task.getActorId());
        pooledActorIds = getPooledActorIds(task);
        assertEquals(2, pooledActorIds.size());
        assertEquals(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS,
                pooledActorIds.get(0));
        assertEquals(NuxeoPrincipal.PREFIX + user1.getName(),
                pooledActorIds.get(1));
        comments = task.getComments();
        // FIXME: cannot add end comment right now
        // assertEquals(2, comments.size());
        assertEquals(1, comments.size());
        comment = comments.get(0);
        assertEquals(user3.getName(), comment.getActorId());
        assertEquals("test comment", comment.getMessage());
        assertEquals(calendar.getTime(), task.getDueDate());
        assertNull(task.getProcessInstance());
        // task status
        assertFalse(task.isOpen());
        assertFalse(task.isCancelled());
        assertTrue(task.hasEnded());
        assertEquals(6, task.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(user3.getName(),
                task.getVariable(JbpmService.VariableName.initiator.name()));
        assertEquals("test directive",
                task.getVariable(JbpmService.TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));
        assertEquals("true",
                task.getVariable(JbpmService.TaskVariableName.validated.name()));

    }

    public void testMultipleTaskWithReject() throws Exception {
        DocumentModel document = getDocument();
        assertNotNull(document);

        // create task as admin
        List<String> actors = new ArrayList<String>();
        actors.add(NuxeoPrincipal.PREFIX + user1.getName());
        actors.add(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6);

        // create one task per actor
        taskService.createTask(coreSession, user3, document, "Test Task Name",
                actors, true, "test directive", "test comment",
                calendar.getTime(), null);

        List<TaskInstance> tasks = service.getTaskInstances(document,
                (NuxeoPrincipal) null, null);
        Collections.sort(tasks, new Comparator<TaskInstance>() {

            public int compare(TaskInstance o1, TaskInstance o2) {
                return o1.getCreate().compareTo(o2.getCreate());
            }

        });
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        TaskInstance task1 = tasks.get(0);
        assertEquals("Test Task Name", task1.getName());
        assertEquals(null, task1.getActorId());
        List<String> pooledActorIds = getPooledActorIds(task1);
        assertEquals(1, pooledActorIds.size());
        assertEquals(NuxeoPrincipal.PREFIX + user1.getName(),
                pooledActorIds.get(0));
        List<Comment> comments = task1.getComments();
        assertEquals(1, comments.size());
        Comment comment = comments.get(0);
        assertEquals(user3.getName(), comment.getActorId());
        assertEquals("test comment", comment.getMessage());
        assertEquals(calendar.getTime(), task1.getDueDate());
        assertNull(task1.getProcessInstance());
        // task status
        assertTrue(task1.isOpen());
        assertFalse(task1.isCancelled());
        assertFalse(task1.hasEnded());
        assertEquals(5, task1.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task1.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task1.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(user3.getName(),
                task1.getVariable(JbpmService.VariableName.initiator.name()));
        assertEquals(
                "test directive",
                task1.getVariable(JbpmService.TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task1.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        // test rights for each user
        // initiator or admin can end a task
        assertTrue(taskService.canEndTask(administrator, task1));
        assertTrue(taskService.canEndTask(user3, task1));
        // user 1 is in actors
        assertTrue(taskService.canEndTask(user1, task1));
        assertFalse(taskService.canEndTask(user2, task1));
        assertFalse(taskService.canEndTask(user4, task1));

        // test ending of the task
        try {
            taskService.rejectTask(coreSession, user2, task1, "i don't agree");
            fail("Should have raised an exception: user2 cannot end the task");
        } catch (NuxeoJbpmException e) {
            assertEquals("User with id 'myuser2' cannot end this task",
                    e.getMessage());
        }

        // reject task as user1
        taskService.rejectTask(coreSession, user1, task1, "i don't agree");

        // test task again
        tasks = service.getTaskInstances(document, (NuxeoPrincipal) null, null);
        assertNotNull(tasks);
        // ended tasks are filtered
        assertEquals(1, tasks.size());

        // retrieve the task another way
        final Long taskId = task1.getId();
        task1 = getTask(taskId);
        assertNotNull(task1);

        assertEquals("Test Task Name", task1.getName());
        assertEquals(null, task1.getActorId());
        pooledActorIds = getPooledActorIds(task1);
        assertEquals(1, pooledActorIds.size());
        assertEquals(NuxeoPrincipal.PREFIX + user1.getName(),
                pooledActorIds.get(0));
        comments = task1.getComments();
        // FIXME: cannot add end comment right now
        // assertEquals(2, comments.size());
        assertEquals(1, comments.size());
        comment = comments.get(0);
        assertEquals(user3.getName(), comment.getActorId());
        assertEquals("test comment", comment.getMessage());
        assertEquals(calendar.getTime(), task1.getDueDate());
        assertNull(task1.getProcessInstance());
        // task status
        assertFalse(task1.isOpen());
        assertFalse(task1.isCancelled());
        assertTrue(task1.hasEnded());
        assertEquals(6, task1.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task1.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task1.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(user3.getName(),
                task1.getVariable(JbpmService.VariableName.initiator.name()));
        assertEquals(
                "test directive",
                task1.getVariable(JbpmService.TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task1.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));
        assertEquals(
                "false",
                task1.getVariable(JbpmService.TaskVariableName.validated.name()));

        // check second task
        TaskInstance task2 = tasks.get(0);
        assertEquals("Test Task Name", task2.getName());
        assertEquals(null, task2.getActorId());
        pooledActorIds = getPooledActorIds(task2);
        assertEquals(1, pooledActorIds.size());
        assertEquals(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS,
                pooledActorIds.get(0));
        comments = task2.getComments();
        assertEquals(1, comments.size());
        comment = comments.get(0);
        assertEquals(user3.getName(), comment.getActorId());
        assertEquals("test comment", comment.getMessage());
        assertEquals(calendar.getTime(), task2.getDueDate());
        assertNull(task2.getProcessInstance());
        // task status
        assertTrue(task2.isOpen());
        assertFalse(task2.isCancelled());
        assertFalse(task2.hasEnded());
        assertEquals(5, task2.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task2.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task2.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(user3.getName(),
                task2.getVariable(JbpmService.VariableName.initiator.name()));
        assertEquals(
                "test directive",
                task2.getVariable(JbpmService.TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task2.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        // test rights for each user
        // initiator or admin can end a task
        assertTrue(taskService.canEndTask(administrator, task2));
        assertTrue(taskService.canEndTask(user3, task2));
        // user 1 is in actors
        assertTrue(taskService.canEndTask(user1, task2));
        // user 2 is in the members group
        assertTrue(taskService.canEndTask(user2, task2));
        // user 4 is not in the members group
        assertFalse(taskService.canEndTask(user4, task2));

        // test ending of the task
        try {
            taskService.acceptTask(coreSession, user4, task2, "i don't agree");
            fail("Should have raised an exception: user4 cannot end the task");
        } catch (NuxeoJbpmException e) {
            assertEquals("User with id 'myuser4' cannot end this task",
                    e.getMessage());
        }

        // accept task as user1
        taskService.acceptTask(coreSession, user1, task2, "i don't agree");

        tasks = service.getTaskInstances(document, (NuxeoPrincipal) null, null);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
    }

    @SuppressWarnings("unchecked")
    protected List<String> getPooledActorIds(TaskInstance task) {
        List<PooledActor> pooledActors = new ArrayList<PooledActor>(
                task.getPooledActors());
        assertNotNull(pooledActors);
        List<String> pooledActorIds = new ArrayList<String>(pooledActors.size());
        for (int i = 0; i < pooledActors.size(); i++) {
            pooledActorIds.add(pooledActors.get(i).getActorId());
        }
        Collections.sort(pooledActorIds);
        return pooledActorIds;
    }

    protected TaskInstance getTask(final Long taskId) throws NuxeoJbpmException {
        return (TaskInstance) service.executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            public TaskInstance run(JbpmContext context)
                    throws NuxeoJbpmException {
                TaskInstance task = context.getTaskInstance(taskId);
                eagerLoadTaskInstance(task);
                return task;
            }

            protected void eagerLoadTaskInstance(TaskInstance ti) {
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

        });
    }

    protected DocumentModel getDocument() throws Exception {
        openRepository();
        CoreSession session = getCoreSession();
        DocumentModel model = session.createDocumentModel(
                session.getRootDocument().getPathAsString(), "1", "File");
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);

        session.saveDocument(doc);
        session.save();
        return doc;
    }

}
