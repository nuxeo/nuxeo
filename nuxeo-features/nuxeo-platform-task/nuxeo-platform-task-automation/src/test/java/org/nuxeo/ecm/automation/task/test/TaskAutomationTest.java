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
package org.nuxeo.ecm.automation.task.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.automation.task.GetUserTasks;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.task.automation")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.task.testing")
@Deploy("org.nuxeo.ecm.platform.task.automation:test-operations.xml")
public class TaskAutomationTest {

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Inject
    TaskService taskService;

    protected DocumentModel document;

    @Before
    public void initRepo() throws Exception {
        document = coreSession.createDocumentModel("/", "task-root", "TaskRoot");
        document.setPropertyValue("dc:title", "Task");
        document = coreSession.createDocument(document);

        document = coreSession.createDocumentModel("/", "src", "Folder");
        document.setPropertyValue("dc:title", "Source");
        document = coreSession.createDocument(document);

        coreSession.save();
        document = coreSession.getDocument(document.getRef());

        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void testCreateSingleTaskChain() throws Exception {

        List<Task> tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(document);
            automationService.run(ctx, "createSingleTaskChain");
        }

        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertEquals("single test task", task.getName());

        List<String> pooledActorIds = task.getActors();
        assertEquals(3, pooledActorIds.size());
        assertTrue(pooledActorIds.contains(SecurityConstants.MEMBERS));
        assertTrue(pooledActorIds.contains("myuser"));
        assertTrue(pooledActorIds.contains(SecurityConstants.ADMINISTRATOR));

        List<TaskComment> comments = task.getComments();
        assertEquals(1, comments.size());

        TaskComment comment = comments.get(0);
        assertEquals(SecurityConstants.ADMINISTRATOR, comment.getAuthor());
        assertEquals("test comment", comment.getText());

        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6, 15, 10, 15);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(calendar.getTime(), task.getDueDate());
        // task status
        assertTrue(task.isOpened());
        assertFalse(task.isCancelled());
        assertFalse(task.hasEnded());
        assertEquals(6, task.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task.getVariable(TaskService.VariableName.documentId.name()));
        assertEquals("test directive", task.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals("true", task.getVariable(TaskService.VariableName.createdFromTaskService.name()));
        assertEquals(SecurityConstants.ADMINISTRATOR, task.getInitiator());

        // accept task
        taskService.acceptTask(coreSession, coreSession.getPrincipal(), task, "ok i'm in");
        coreSession.save();
        // test task again
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        // ended tasks are filtered
        assertEquals(0, tasks.size());

        // check document metadata
        assertNull(document.getPropertyValue("dc:description"));
    }

    @Test
    public void testGetUserTasks() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(document);
        automationService.run(ctx, "createSingleTaskChain");
        ctx.clear();
        OperationChain chain = new OperationChain("test");
        chain.add(GetUserTasks.ID);
        Blob blob = (Blob) automationService.run(ctx, chain);
        JSONArray rows = new JSONArray(blob.getString());
        assertEquals(1, rows.length());
        JSONObject obj = rows.getJSONObject(0);
        assertNotNull(obj.get("id")); // can be 1 or 2 depending
        assertEquals(obj.get("docref"), document.getRef().toString());
        assertEquals(obj.get("name"), "single test task");
        assertEquals(obj.get("directive"), "test directive");
        assertEquals(obj.get("comment"), "test comment");
        assertNotNull(obj.get("startDate"));
        assertNotNull(obj.get("dueDate"));
        assertTrue((Boolean) obj.get("expired"));
    }

    @Test
    public void testCreateSingleTaskChainWithoutActors() throws Exception {

        List<Task> tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());

        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(document);
            automationService.run(ctx, "createSingleTaskChainWithoutActors");
        }

        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertEquals(0, tasks.size());
    }

    @Test
    public void testCreateSeveralTasksChain() throws Exception {

        List<Task> tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());

        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(document);
            automationService.run(ctx, "createSeveralTasksChain");
        }

        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        Collections.sort(tasks, new TaskInstanceComparator());
        assertEquals(3, tasks.size());

        Task task1 = tasks.get(0);
        assertEquals("several test tasks", task1.getName());

        List<String> pooledActorIds = task1.getActors();
        assertEquals(1, pooledActorIds.size());
        assertEquals(SecurityConstants.ADMINISTRATOR, pooledActorIds.get(0));

        List<TaskComment> comments = task1.getComments();
        assertEquals(0, comments.size());
        // task status
        assertTrue(task1.isOpened());
        assertFalse(task1.isCancelled());
        assertFalse(task1.hasEnded());
        assertEquals(5, task1.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task1.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task1.getVariable(TaskService.VariableName.documentId.name()));
        assertNull(task1.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task1.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals("true", task1.getVariable(TaskService.VariableName.createdFromTaskService.name()));

        assertEquals(SecurityConstants.ADMINISTRATOR, task1.getInitiator());
        // accept task
        taskService.acceptTask(coreSession, coreSession.getPrincipal(), task1, "ok i'm in");
        coreSession.save();
        // test task again
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        // ended tasks are filtered
        assertEquals(2, tasks.size());

        Collections.sort(tasks, new TaskInstanceComparator());

        // check other tasks
        Task task2 = tasks.get(0);
        assertEquals("several test tasks", task2.getName());

        pooledActorIds = task2.getActors();
        assertEquals(1, pooledActorIds.size());
        assertEquals(SecurityConstants.MEMBERS, pooledActorIds.get(0));

        comments = task2.getComments();
        assertEquals(0, comments.size());
        // task status
        assertTrue(task2.isOpened());
        assertFalse(task2.isCancelled());
        assertFalse(task2.hasEnded());
        assertEquals(5, task2.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task2.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task2.getVariable(TaskService.VariableName.documentId.name()));
        assertNull(task2.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task2.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals("true", task2.getVariable(TaskService.VariableName.createdFromTaskService.name()));

        assertEquals(SecurityConstants.ADMINISTRATOR, task2.getInitiator());
        Task task3 = tasks.get(1);
        assertEquals("several test tasks", task3.getName());

        pooledActorIds = task3.getActors();
        assertEquals(1, pooledActorIds.size());
        assertEquals("myuser", pooledActorIds.get(0));

        comments = task3.getComments();
        assertEquals(0, comments.size());
        // task status
        assertTrue(task3.isOpened());
        assertFalse(task3.isCancelled());
        assertFalse(task3.hasEnded());
        assertEquals(5, task3.getVariables().size());
        assertEquals(document.getRepositoryName(),
                task3.getVariable(TaskService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(), task3.getVariable(TaskService.VariableName.documentId.name()));
        assertNull(task3.getVariable(TaskService.VariableName.directive.name()));
        assertEquals("true", task3.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals("true", task3.getVariable(TaskService.VariableName.createdFromTaskService.name()));

        assertEquals(SecurityConstants.ADMINISTRATOR, task3.getInitiator());
        // check document metadata
        assertNull(document.getPropertyValue("dc:description"));
    }

    @Test
    public void testCreateSingleTaskAndRunOperationChain() throws Exception {

        List<Task> tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());

        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(document);
            automationService.run(ctx, "createSingleTaskAndRunOperationChain");
        }

        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);

        // accept task
        taskService.acceptTask(coreSession, coreSession.getPrincipal(), task, "ok i'm in");
        coreSession.save();
        // test task again
        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        // ended tasks are filtered
        assertEquals(0, tasks.size());

        // check document metadata, refetching doc from core
        document = coreSession.getDocument(document.getRef());
        assertEquals("This document has been accepted", document.getPropertyValue("dc:description"));

        // run another time, and this time reject
        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(document);
            automationService.run(ctx, "createSingleTaskAndRunOperationChain");
        }

        tasks = taskService.getTaskInstances(document, (NuxeoPrincipal) null, coreSession);
        assertEquals(1, tasks.size());
        taskService.rejectTask(coreSession, coreSession.getPrincipal(), tasks.get(0),
                "i don't agree with what you're saying");
        document = coreSession.getDocument(document.getRef());
        assertEquals("This document has been rejected !!!", document.getPropertyValue("dc:description"));

    }

    class TaskInstanceComparator implements Comparator<Task> {
        @Override
        public int compare(Task o1, Task o2) {
            // return o1.getCreated().compareTo(o2.getCreated());
            // stupid MySQL doesn't have subsecond resolution
            // sorting by first actor is enough for this test
            String a1 = o1.getActors().get(0);
            String a2 = o2.getActors().get(0);
            return a1.compareTo(a2);
        }
    }

}
