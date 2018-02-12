/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.task.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Anahide Tchertchian
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.task.testing:OSGI-INF/test-sql-directories-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml")
public class TaskServiceTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected TaskService taskService;

    @Inject
    protected UserManager userManager;

    protected NuxeoPrincipal administrator;

    protected NuxeoPrincipal user1;

    protected NuxeoPrincipal user2;

    protected NuxeoPrincipal user3;

    protected NuxeoPrincipal user4;

    @Before
    public void setUp() {
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

    @Test
    public void testSingleTaskWithAccept() throws Exception {
        DocumentModel document = getDocument();
        assertNotNull(document);

        // create task as admin
        List<String> actors = new ArrayList<String>();
        actors.add(user1.getName());
        actors.add(SecurityConstants.MEMBERS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6);
        calendar.set(Calendar.MILLISECOND, 0); // be sure to avoid Timestamp
                                               // truncation issues.

        // create one task for all actors
        taskService.createTask(session, user3, document, "Test Task Name", "test type", "test process id", actors,
                false, "test directive", "test comment", calendar.getTime(), null, null);
        session.save();
        List<Task> tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);

        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertEquals("Test Task Name", task.getName());
        assertEquals("test type", task.getType());
        assertEquals("test process id", task.getProcessId());

        List<String> pooledActorIds = task.getActors();
        assertEquals(2, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(SecurityConstants.MEMBERS));
        assertTrue(pooledActorIds.contains(user1.getName()));

        List<TaskComment> comments = task.getComments();
        assertEquals(1, comments.size());

        TaskComment comment = comments.get(0);
        assertEquals(user3.getName(), comment.getAuthor());
        assertEquals("test comment", comment.getText());
        assertEquals(calendar.getTime(), task.getDueDate());
        // task status
        assertTrue(task.isOpened());
        assertFalse(task.isCancelled());
        assertFalse(task.hasEnded());

        assertEquals(4, task.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task.getVariable(TaskService.VariableName.documentId.name()));
        assertEquals("test directive", task.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task.getVariable(TaskService.VariableName.createdFromTaskService.name()));

        assertEquals(user3.getName(), task.getInitiator());
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
            taskService.acceptTask(session, user4, task, "ok i'm in");
            fail("Should have raised an exception: user4 cannot end the task");
        } catch (NuxeoException e) {
            assertEquals("User with id 'myuser4' cannot end this task", e.getMessage());
        }

        // accept task
        taskService.acceptTask(session, user1, task, "ok i'm in");

        session.save();

        // test task again
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        // ended tasks are filtered
        assertEquals(0, tasks.size());

        // retrieve the task another way
        String taskId = task.getId();
        task = getTask(taskId);
        assertNotNull(task);
        assertEquals("Test Task Name", task.getName());

        pooledActorIds = task.getActors();
        assertEquals(2, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(SecurityConstants.MEMBERS));
        assertTrue(pooledActorIds.contains(user1.getName()));

        comments = task.getComments();
        assertEquals(2, comments.size());

        comment = comments.get(0);
        assertEquals(user3.getName(), comment.getAuthor());
        assertEquals("test comment", comment.getText());
        assertEquals(calendar.getTime(), task.getDueDate());
        // task status
        assertFalse(task.isOpened());
        assertFalse(task.isCancelled());
        assertTrue(task.hasEnded());
        assertEquals(5, task.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task.getVariable(TaskService.VariableName.documentId.name()));
        assertEquals("test directive", task.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task.getVariable(TaskService.VariableName.createdFromTaskService.name()));
        assertEquals("true", task.getVariable(TaskService.VariableName.validated.name()));
    }

    @Test
    public void testMultipleTaskWithReject() throws Exception {
        DocumentModel document = getDocument();
        assertNotNull(document);

        // create task as admin
        List<String> actors = new ArrayList<String>();
        actors.add(user1.getName());
        actors.add(SecurityConstants.MEMBERS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6);
        calendar.set(Calendar.MILLISECOND, 0); // be sure to avoid Timestamp
                                               // truncation issues.

        // create one task per actor
        taskService.createTask(session, user3, document, "Test Task Name", actors, true, "test directive",
                "test comment", calendar.getTime(), null, null);
        session.save();
        List<Task> tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        // order is database-dependent
        if (tasks.get(0).getActors().get(0).equals(SecurityConstants.MEMBERS)) {
            Collections.reverse(tasks);
        }

        Task task1 = tasks.get(0);
        assertEquals("Test Task Name", task1.getName());

        List<String> pooledActorIds = task1.getActors();
        assertEquals(1, pooledActorIds.size());
        assertEquals(user1.getName(), pooledActorIds.get(0));

        List<TaskComment> comments = task1.getComments();
        assertEquals(1, comments.size());

        TaskComment comment = comments.get(0);
        assertEquals(user3.getName(), comment.getAuthor());
        assertEquals("test comment", comment.getText());
        assertEquals(calendar.getTime(), task1.getDueDate());
        // task status
        assertTrue(task1.isOpened());
        assertFalse(task1.isCancelled());
        assertFalse(task1.hasEnded());
        assertEquals(4, task1.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task1.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task1.getVariable(TaskService.VariableName.documentId.name()));
        assertEquals("test directive", task1.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task1.getVariable(TaskService.VariableName.createdFromTaskService.name()));
        assertEquals(user3.getName(), task1.getInitiator());

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
            taskService.rejectTask(session, user2, task1, "i don't agree");
            fail("Should have raised an exception: user2 cannot end the task");
        } catch (NuxeoException e) {
            assertEquals("User with id 'myuser2' cannot end this task", e.getMessage());
        }

        // reject task as user1
        taskService.rejectTask(session, user1, task1, "i don't agree");
        session.save();
        // test task again
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        // ended tasks are filtered
        assertEquals(1, tasks.size());

        // retrieve the task another way
        final String taskId = task1.getId();
        task1 = getTask(taskId);
        assertNotNull(task1);
        assertEquals("Test Task Name", task1.getName());

        pooledActorIds = task1.getActors();
        assertEquals(1, pooledActorIds.size());
        assertEquals(user1.getName(), pooledActorIds.get(0));

        comments = task1.getComments();
        assertEquals(2, comments.size());

        comment = comments.get(0);
        assertEquals(user3.getName(), comment.getAuthor());
        assertEquals("test comment", comment.getText());
        assertEquals(calendar.getTime(), task1.getDueDate());
        // task status
        assertFalse(task1.isOpened());
        assertFalse(task1.isCancelled());
        assertTrue(task1.hasEnded());
        assertEquals(5, task1.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task1.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task1.getVariable(TaskService.VariableName.documentId.name()));
        assertEquals("test directive", task1.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task1.getVariable(TaskService.VariableName.createdFromTaskService.name()));
        assertEquals("false", task1.getVariable(TaskService.VariableName.validated.name()));
        assertEquals(user3.getName(), task1.getInitiator());

        // check second task
        Task task2 = tasks.get(0);
        assertEquals("Test Task Name", task2.getName());

        pooledActorIds = task2.getActors();
        assertEquals(1, pooledActorIds.size());
        assertEquals(SecurityConstants.MEMBERS, pooledActorIds.get(0));

        comments = task2.getComments();
        assertEquals(1, comments.size());

        comment = comments.get(0);
        assertEquals(user3.getName(), comment.getAuthor());
        assertEquals("test comment", comment.getText());
        assertEquals(calendar.getTime(), task2.getDueDate());
        // task status
        assertTrue(task2.isOpened());
        assertFalse(task2.isCancelled());
        assertFalse(task2.hasEnded());
        assertEquals(4, task2.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task2.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task2.getVariable(TaskService.VariableName.documentId.name()));
        assertEquals("test directive", task2.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task2.getVariable(TaskService.VariableName.createdFromTaskService.name()));
        assertEquals(user3.getName(), task2.getInitiator());

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
            taskService.acceptTask(session, user4, task2, "i don't agree");
            fail("Should have raised an exception: user4 cannot end the task");
        } catch (NuxeoException e) {
            assertEquals("User with id 'myuser4' cannot end this task", e.getMessage());
        }

        // accept task as user1
        taskService.acceptTask(session, user1, task2, "i don't agree");
        session.save();
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
    }

    /**
     * Test user tasks.
     *
     * @throws Exception the exception
     */
    @Test
    public void testUserTasks() throws Exception {

        DocumentModel document = getDocument();
        assertNotNull(document);

        // ----------------------------------------------------------------------
        // Create a task assigned to user1 and check that user1 has 1 task
        // assigned
        // ----------------------------------------------------------------------
        // set task actors
        List<String> actors = new ArrayList<String>();
        actors.add(user1.getName());

        // create task
        taskService.createTask(session, administrator, document, "Task assigned to user1", actors, false, null, null,
                null, null, null);
        session.save();
        // get user1 tasks
        List<Task> tasks = taskService.getTaskInstances(document, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertEquals("Task assigned to user1", task.getName());

        List<String> pooledActorIds = task.getActors();
        assertEquals(1, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(user1.getName()));

        // ----------------------------------------------------------------------
        // Create a task assigned to user2 and check that:
        // - user2 has 1 task assigned
        // - the total number of tasks is 2
        // ----------------------------------------------------------------------
        // set task actors
        actors.clear();
        actors.add(user2.getName());

        // create task
        taskService.createTask(session, administrator, document, "Task assigned to user2", actors, false, null, null,
                null, null, null);
        session.save();
        // get user2 tasks
        tasks = taskService.getTaskInstances(document, user2, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        task = tasks.get(0);
        assertEquals("Task assigned to user2", task.getName());

        pooledActorIds = task.getActors();
        assertEquals(1, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(user2.getName()));

        // get all tasks
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // ----------------------------------------------------------------------
        // Create a task assigned to user3 and user4 (using
        // createOneTaskPerActor) and check that:
        // - user3 has 1 task assigned
        // - user4 has 1 task assigned
        // - the total number of tasks is 4
        // ----------------------------------------------------------------------
        // set task actors
        actors.clear();
        actors.add(user3.getName());
        actors.add(user4.getName());

        // create task
        taskService.createTask(session, administrator, document, "Task assigned to user3 and user4", actors, true,
                null, null, null, null, null);
        session.save();
        // get user3 tasks
        tasks = taskService.getTaskInstances(document, user3, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        task = tasks.get(0);
        assertEquals("Task assigned to user3 and user4", task.getName());

        pooledActorIds = task.getActors();
        assertEquals(1, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(user3.getName()));

        // get user4 tasks
        tasks = taskService.getTaskInstances(document, user4, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        task = tasks.get(0);
        assertEquals("Task assigned to user3 and user4", task.getName());

        pooledActorIds = task.getActors();
        assertEquals(1, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(user4.getName()));

        // get all tasks
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(4, tasks.size());

        // ----------------------------------------------------------------------
        // Create a task assigned to members and check that all users that are
        // in the members group have this task assigned (user1, user2, user3).
        // Since at this point each user has 1 task assigned,
        // these users should then have 2.
        // The total number of tasks should be 5.
        // ----------------------------------------------------------------------
        // set task actors
        actors.clear();
        actors.add(SecurityConstants.MEMBERS);

        // create task
        taskService.createTask(session, administrator, document, "Task assigned to members", actors, false, null, null,
                null, null, null);
        session.save();
        // get user1 tasks
        tasks = taskService.getTaskInstances(document, user1, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // get user2 tasks
        tasks = taskService.getTaskInstances(document, user2, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // get user3 tasks
        tasks = taskService.getTaskInstances(document, user3, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // get user4 tasks
        tasks = taskService.getTaskInstances(document, user4, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // get all tasks
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(5, tasks.size());

    }

    /**
     * Check prefixed and unprefixed names in actors list.
     * <p>
     * It should have no impact since the DocumentTaskProvider rebuilds a clean actors list with both prefixed and
     * unprefixed names of the principal and all its groups.
     *
     * @throws Exception the exception
     */
    @Test
    public void testPrefixedUnprefixedActorNames() throws Exception {

        DocumentModel document = getDocument();
        assertNotNull(document);

        // set task actors mixing user and groups, prefixed and unprefixed
        // names
        List<String> actors = new ArrayList<String>();
        actors.add(user1.getName());
        actors.add(NuxeoPrincipal.PREFIX + user2.getName());
        actors.add(SecurityConstants.ADMINISTRATORS);
        actors.add(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS);

        // create task
        taskService.createTask(session, administrator, document,
                "Task assigned to prefixed ans unprefixed users and groups", actors, true, null, null, null, null, null);
        session.save();
        // get user1 tasks: should have 2 since in members group
        List<Task> tasks = taskService.getTaskInstances(document, user1, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // get user2 tasks: should have 2 since in members group
        tasks = taskService.getTaskInstances(document, user2, session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // get user3 tasks: should have 1 since in members group
        tasks = taskService.getTaskInstances(document, user3, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // get user4 tasks: should have 0 since not in members group
        tasks = taskService.getTaskInstances(document, user4, session);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());

        // get administrator tasks: should have 1 since in administrators group
        tasks = taskService.getTaskInstances(document, administrator, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // get all tasks: should have 4 (1 per actor)
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(4, tasks.size());
    }

    /**
     * Test user tasks retrieval by non-admin session.
     */
    @Test
    public void testUserTasksAsUser() throws Exception {

        DocumentModel document = getDocument();
        assertNotNull(document);

        taskService.createTask(session, administrator, document, "Task assigned to user1",
                Arrays.asList(user1.getName()), false, null, null, null, null, null);
        session.save();
        // check as admin
        List<Task> tasks = taskService.getTaskInstances(document, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        Task task = tasks.get(0);
        assertEquals("Task assigned to user1", task.getName());
        List<String> pooledActorIds = task.getActors();
        assertEquals(1, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(user1.getName()));

        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // check as user1
        try (CloseableCoreSession session1 = coreFeature.openCoreSession(user1.getName())) {
            tasks = taskService.getTaskInstances(document, user1, session1);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            task = tasks.get(0);
            assertEquals("Task assigned to user1", task.getName());
            pooledActorIds = task.getActors();
            assertEquals(1, pooledActorIds.size());
            assertTrue(pooledActorIds.contains(user1.getName()));

            tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session1);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
        }

        // check that user2 sees them if requesting the given user / all
        try (CloseableCoreSession session2 = coreFeature.openCoreSession(user2.getName())) {
            tasks = taskService.getTaskInstances(document, user1, session2);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            task = tasks.get(0);
            assertEquals("Task assigned to user1", task.getName());
            pooledActorIds = task.getActors();
            assertEquals(1, pooledActorIds.size());
            assertTrue(pooledActorIds.contains(user1.getName()));

            tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, session2);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
        }
    }

    protected Task getTask(final String taskId) {
        DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
        if (taskDoc != null) {
            return taskDoc.getAdapter(Task.class);
        }
        return null;
    }

    protected DocumentModel getDocument() throws Exception {
        DocumentModel model = session.createDocumentModel(session.getRootDocument().getPathAsString(), "1", "File");
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);

        session.saveDocument(doc);
        session.save();
        return doc;
    }

}
