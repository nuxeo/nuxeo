/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.task.Task;
import org.nuxeo.client.api.objects.task.TaskCompletionRequest;
import org.nuxeo.client.api.objects.task.Tasks;
import org.nuxeo.client.api.objects.workflow.Graph;
import org.nuxeo.client.api.objects.workflow.Workflow;
import org.nuxeo.client.api.objects.workflow.Workflows;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import java.util.HashMap;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, WorkflowFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.restapi.server.routing", "org.nuxeo.ecm.platform.routing.default",
        "org.nuxeo.ecm.platform.filemanager.api", "org.nuxeo.ecm.platform.filemanager.core" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class TestWorkflowAndTask extends TestBase {

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanFetchWorkflowInstances() {
        nuxeoClient.repository().startWorkflowInstanceWithDocPath("/", getOneWorkflowModel());
        Workflows workflowInstances = nuxeoClient.fetchCurrentUser().fetchWorkflowInstances();
        assertNotNull(workflowInstances);
        assertTrue(workflowInstances.size() != 0);
    }

    @Test
    public void itCanFetchDocWorflowInstances() {
        nuxeoClient.repository().fetchDocumentRoot().startWorkflowInstance(getOneWorkflowModel());
        Workflows workflowInstances = nuxeoClient.repository().fetchDocumentRoot().fetchWorkflowInstances();
        assertNotNull(workflowInstances);
    }

    @Test
    public void itCanFetchWorkflowGraph() {
        Graph graph = nuxeoClient.repository().fetchWorkflowModelGraph(getOneWorkflowModel().getWorkflowModelName());
        assertNotNull(graph);
    }

    @Test
    public void itCanCancelWorkflow() {
        Workflow workflow = nuxeoClient.repository().fetchDocumentRoot().startWorkflowInstance(getOneWorkflowModel());
        nuxeoClient.repository().cancelWorkflowInstance(workflow.getId());
        try {
            nuxeoClient.repository().cancelWorkflowInstance(workflow.getId());
            fail("Should fail: wf instance already cancelled");
        } catch (NuxeoClientException reason) {
            assertEquals(500, reason.getStatus());
        }
    }

    protected Workflow getOneWorkflowModel() {
        Workflows workflows = nuxeoClient.repository().fetchWorkflowModels();
        return workflows.get(0);
    }

    @Test
    public void itCanFetchAllTasksFromWFAndUser() {
        Tasks tasks = fetchAllTasks();
        assertNotNull(tasks);
        assertTrue(tasks.size() != 0);
    }

    @Test
    public void itCanFetchTask() {
        Task task = fetchAllTasks().get(0);
        String name = task.getName();
        task = nuxeoClient.getTaskManager().fetchTask(task.getId());
        assertNotNull(task);
        assertEquals(name, task.getName());
    }

    @Test
    public void itCanFetchTaskFromDoc() {
        Task task = fetchAllTasks().get(0);
        Document target = nuxeoClient.repository().fetchDocumentById(task.getTargetDocumentIds().get(0).get("id"));
        task = target.fetchTask();
        assertNotNull(task);
    }

    @Ignore("JAVACLIENT-82")
    @Test
    public void itCanCompleteATask() {
        Task task = fetchAllTasks().get(0);
        TaskCompletionRequest taskCompletionRequest = new TaskCompletionRequest();
        taskCompletionRequest.setComment("comment");
        taskCompletionRequest.setVariables(new HashMap<>());
        task = nuxeoClient.getTaskManager().complete(task.getId(),
                "start_review", taskCompletionRequest);
        assertNotNull(task);
    }

    @Ignore("JAVACLIENT-81")
    @Test
    public void itCanDelegate() {
        Task task = fetchAllTasks().get(0);
        String name = task.getName();
        task = nuxeoClient.getTaskManager().delegate(task.getId(), "Administrator", "some comment");
        assertNotNull(task);
        assertEquals(name, task.getName());
    }

    @Test
    public void itCanReAssign() {
        Task task = fetchAllTasks().get(0);
        try {
            nuxeoClient.getTaskManager().reassign(task.getId(), "Administrator", "some comment");
            fail("Should fail: not possible to reassign this task");
        } catch (NuxeoClientException reason) {
            assertEquals(500, reason.getStatus());
        }
    }

    protected Tasks fetchAllTasks() {
        nuxeoClient.repository().fetchDocumentRoot().startWorkflowInstance(getOneWorkflowModel());
        Workflows workflows = nuxeoClient.fetchCurrentUser().fetchWorkflowInstances();
        Workflow workflow = workflows.get(0);
        return nuxeoClient.getTaskManager().fetchTasks("Administrator", workflow.getId(),
                workflow.getWorkflowModelName());
    }

}