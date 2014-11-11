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
package org.nuxeo.ecm.automation.jbpm.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.Assert;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jbpm.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.automation.jbpm.GetUserTasks;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmService.TaskVariableName;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author Anahide Tchertchian "org.nuxeo.ecm.platform.jbpm.testing" })
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy( { "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.platform.jbpm.automation",
        "org.nuxeo.ecm.platform.jbpm.core",
        "org.nuxeo.ecm.platform.jbpm.testing" })
@LocalDeploy("org.nuxeo.ecm.platform.jbpm.automation:test-operations.xml")
public class JbpmAutomationTest {

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Inject
    JbpmService jbpmService;

    @Inject
    JbpmTaskService jbpmTaskService;

    protected DocumentModel document;

    @Before
    public void initRepo() throws Exception {
        document = coreSession.createDocumentModel("/", "src", "Folder");
        document.setPropertyValue("dc:title", "Source");
        document = coreSession.createDocument(document);
        coreSession.save();
        document = coreSession.getDocument(document.getRef());
    }

    @After
    public void clearRepo() throws Exception {
        coreSession.removeChildren(coreSession.getRootDocument().getRef());
    }

    @Test
    public void testCreateSingleTaskChain() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(document);

        List<TaskInstance> tasks = jbpmService.getTaskInstances(document,
                (NuxeoPrincipal) null, null);
        Assert.assertNotNull(tasks);
        assertEquals(0, tasks.size());

        automationService.run(ctx, "createSingleTaskChain");

        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        assertEquals(1, tasks.size());

        TaskInstance task = tasks.get(0);
        assertEquals("single test task", task.getName());
        Assert.assertNull(task.getActorId());

        List<String> pooledActorIds = getPooledActorIds(task);
        assertEquals(3, pooledActorIds.size());
        assertEquals(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS,
                pooledActorIds.get(0));
        assertEquals(NuxeoPrincipal.PREFIX
                + SecurityConstants.ADMINISTRATOR, pooledActorIds.get(1));
        assertEquals(NuxeoPrincipal.PREFIX + "myuser",
                pooledActorIds.get(2));

        List<Comment> comments = task.getComments();
        assertEquals(1, comments.size());

        Comment comment = comments.get(0);
        assertEquals(SecurityConstants.ADMINISTRATOR,
                comment.getActorId());
        assertEquals("test comment", comment.getMessage());

        Calendar calendar = Calendar.getInstance();
        calendar.set(2006, 6, 6, 17, 10, 15);
        // FIXME: check why dates are not exactly equal
        // Assert.assertEquals(calendar.getTime(), task.getDueDate());
        Assert.assertNull(task.getProcessInstance());
        // task status
        assertTrue(task.isOpen());
        assertFalse(task.isCancelled());
        assertFalse(task.hasEnded());
        assertEquals(7, task.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(SecurityConstants.ADMINISTRATOR,
                task.getVariable(JbpmService.VariableName.initiator.name()));
        assertEquals("test directive",
                task.getVariable(TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals(
                "true",
                task.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        // accept task
        jbpmTaskService.acceptTask(coreSession,
                (NuxeoPrincipal) coreSession.getPrincipal(), task, "ok i'm in");

        // test task again
        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        // ended tasks are filtered
        assertEquals(0, tasks.size());

        // check document metadata
        Assert.assertNull(document.getPropertyValue("dc:description"));
    }

    @Test
    public void testGetUserTasks() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(document);
       automationService.run(ctx, "createSingleTaskChain");
       ctx.clear();
       OperationChain chain = new OperationChain("test");
       chain.add(GetUserTasks.ID);
       Blob blob = (Blob)automationService.run(ctx, chain);
       JSONArray rows = JSONArray.fromObject(blob.getString());
       assertEquals(1, rows.size());
       JSONObject obj = rows.getJSONObject(0);
       assertNotNull(obj.get("id")); // can be 1 or 2 depending
       assertEquals(obj.get("docref"), document.getRef().toString());
       assertEquals(obj.get("name"), "single test task");
       assertEquals(obj.get("directive"), "test directive");
       assertEquals(obj.get("comment"), "test comment");
       assertNotNull(obj.get("startDate"));
       assertNotNull(obj.get("dueDate"));
       assertTrue((Boolean)obj.get("expired"));
    }

    @Test
    public void testCreateSingleTaskChainWithoutActors() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(document);

        List<TaskInstance> tasks = jbpmService.getTaskInstances(document,
                (NuxeoPrincipal) null, null);
        Assert.assertNotNull(tasks);
        assertEquals(0, tasks.size());

        automationService.run(ctx, "createSingleTaskChainWithoutActors");

        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        assertEquals(0, tasks.size());
    }

    @Test
    public void testCreateSeveralTasksChain() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(document);

        List<TaskInstance> tasks = jbpmService.getTaskInstances(document,
                (NuxeoPrincipal) null, null);
        Assert.assertNotNull(tasks);
        assertEquals(0, tasks.size());

        automationService.run(ctx, "createSeveralTasksChain");

        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        Collections.sort(tasks, new TaskInstanceComparator());
        assertEquals(3, tasks.size());

        TaskInstance task1 = tasks.get(0);
        assertEquals("several test tasks", task1.getName());
        assertNull(task1.getActorId());

        List<String> pooledActorIds = getPooledActorIds(task1);
        assertEquals(1, pooledActorIds.size());
        assertEquals(NuxeoPrincipal.PREFIX + "myuser", pooledActorIds.get(0));

        List<Comment> comments = task1.getComments();
        assertEquals(0, comments.size());
        Assert.assertNull(task1.getProcessInstance());
        // task status
        assertTrue(task1.isOpen());
        assertFalse(task1.isCancelled());
        assertFalse(task1.hasEnded());
        assertEquals(7, task1.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task1.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task1.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(SecurityConstants.ADMINISTRATOR,
                task1.getVariable(JbpmService.VariableName.initiator.name()));
        Assert.assertNull(task1.getVariable(TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task1.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals(
                "true",
                task1.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        // accept task
        jbpmTaskService.acceptTask(coreSession,
                (NuxeoPrincipal) coreSession.getPrincipal(), task1, "ok i'm in");

        // test task again
        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        // ended tasks are filtered
        assertEquals(2, tasks.size());

        Collections.sort(tasks, new TaskInstanceComparator());

        // check other tasks
        TaskInstance task2 = tasks.get(0);
        assertEquals("several test tasks", task2.getName());
        assertNull(task2.getActorId());

        pooledActorIds = getPooledActorIds(task2);
        assertEquals(1, pooledActorIds.size());
        assertEquals(NuxeoPrincipal.PREFIX + SecurityConstants.ADMINISTRATOR,
                pooledActorIds.get(0));

        comments = task2.getComments();
        assertEquals(0, comments.size());
        Assert.assertNull(task2.getProcessInstance());
        // task status
        assertTrue(task2.isOpen());
        assertFalse(task2.isCancelled());
        assertFalse(task2.hasEnded());
        assertEquals(7, task2.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task2.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task2.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(SecurityConstants.ADMINISTRATOR,
                task2.getVariable(JbpmService.VariableName.initiator.name()));
        Assert.assertNull(task2.getVariable(TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task2.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals(
                "true",
                task2.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        TaskInstance task3 = tasks.get(1);
        assertEquals("several test tasks", task3.getName());
        Assert.assertNull(task3.getActorId());

        pooledActorIds = getPooledActorIds(task3);
        assertEquals(1, pooledActorIds.size());
        assertEquals(NuxeoGroup.PREFIX + SecurityConstants.MEMBERS,
                pooledActorIds.get(0));

        comments = task3.getComments();
        assertEquals(0, comments.size());
        Assert.assertNull(task3.getProcessInstance());
        // task status
        assertTrue(task3.isOpen());
        assertFalse(task3.isCancelled());
        assertFalse(task3.hasEnded());
        assertEquals(7, task3.getVariables().size());
        assertEquals(
                document.getRepositoryName(),
                task3.getVariable(JbpmService.VariableName.documentRepositoryName.name()));
        assertEquals(document.getId(),
                task3.getVariable(JbpmService.VariableName.documentId.name()));
        assertEquals(SecurityConstants.ADMINISTRATOR,
                task3.getVariable(JbpmService.VariableName.initiator.name()));
        Assert.assertNull(task3.getVariable(TaskVariableName.directive.name()));
        assertEquals(
                "true",
                task3.getVariable(OperationTaskVariableName.createdFromCreateTaskOperation.name()));
        assertEquals(
                "true",
                task3.getVariable(JbpmTaskService.TaskVariableName.createdFromTaskService.name()));

        // check document metadata
        Assert.assertNull(document.getPropertyValue("dc:description"));
    }

    @Test
    public void testCreateSingleTaskAndRunOperationChain() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(document);

        List<TaskInstance> tasks = jbpmService.getTaskInstances(document,
                (NuxeoPrincipal) null, null);
        Assert.assertNotNull(tasks);
        assertEquals(0, tasks.size());

        automationService.run(ctx, "createSingleTaskAndRunOperationChain");

        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        assertEquals(1, tasks.size());

        TaskInstance task = tasks.get(0);

        // accept task
        jbpmTaskService.acceptTask(coreSession,
                (NuxeoPrincipal) coreSession.getPrincipal(), task, "ok i'm in");

        // test task again
        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        // ended tasks are filtered
        assertEquals(0, tasks.size());

        // check document metadata, refetching doc from core
        document = coreSession.getDocument(document.getRef());
        assertEquals("This document has been accepted",
                document.getPropertyValue("dc:description"));

        // run another time, and this time reject
        automationService.run(ctx, "createSingleTaskAndRunOperationChain");
        tasks = jbpmService.getTaskInstances(document, (NuxeoPrincipal) null,
                null);
        assertEquals(1, tasks.size());
        jbpmTaskService.rejectTask(coreSession,
                (NuxeoPrincipal) coreSession.getPrincipal(), tasks.get(0),
                "i don't agree with what you're saying");
        document = coreSession.getDocument(document.getRef());
        assertEquals("This document has been rejected !!!",
                document.getPropertyValue("dc:description"));

    }

    @SuppressWarnings("unchecked")
    protected List<String> getPooledActorIds(TaskInstance task) {
        List<PooledActor> pooledActors = new ArrayList<PooledActor>(
                task.getPooledActors());
        Assert.assertNotNull(pooledActors);
        List<String> pooledActorIds = new ArrayList<String>(pooledActors.size());
        for (PooledActor actor : pooledActors) {
            pooledActorIds.add(actor.getActorId());
        }
        Collections.sort(pooledActorIds);
        return pooledActorIds;
    }

    class TaskInstanceComparator implements Comparator<TaskInstance> {
        @Override
        public int compare(TaskInstance o1, TaskInstance o2) {
            String actor1 = ((PooledActor) o1.getPooledActors().toArray()[0]).getActorId();
            String actor2 = ((PooledActor) o2.getPooledActors().toArray()[0]).getActorId();

            return actor2.compareTo(actor1);
        }
    }

}
