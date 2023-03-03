/*
 * (C) Copyright 2014-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *     <a href="mailto:ncunha@nuxeo.com">Nuno Cunha</a>
 *
 */

package org.nuxeo.ecm.restapi.server;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.routing.core.io.DocumentRouteWriter;
import org.nuxeo.ecm.platform.routing.core.io.TaskWriter;
import org.nuxeo.ecm.platform.routing.core.io.enrichers.PendingTasksJsonEnricher;
import org.nuxeo.ecm.platform.routing.core.io.enrichers.RunnableWorkflowJsonEnricher;
import org.nuxeo.ecm.platform.routing.core.io.enrichers.RunningWorkflowJsonEnricher;
import org.nuxeo.ecm.platform.routing.core.listener.DocumentRoutingEscalationListener;
import org.nuxeo.ecm.platform.routing.core.listener.DocumentRoutingWorkflowInstancesCleanup;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter.TaskAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter.WorkflowAdapter;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, WorkflowFeature.class, AuditFeature.class, LogCaptureFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.server.routing")
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.platform.restapi.io")
@Deploy("org.nuxeo.ecm.platform.restapi.test")
@Deploy("org.nuxeo.ecm.platform.restapi.server")
@Deploy("org.nuxeo.ecm.platform.routing.default")
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.actions")
public class WorkflowEndpointTest extends RoutingRestBaseTest {

    protected static final int NB_WF = 5;

    protected static final String CANCELLED_WORKFLOWS = "SELECT ecm:uuid FROM DocumentRoute WHERE ecm:currentLifeCycleState = 'canceled'";

    @Inject
    protected WorkManager workManager;

    @Inject
    protected EventService eventService;

    @Inject
    protected LogCaptureFeature.Result logResult;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected UserManager userManager;

    @Test
    public void testAdapter() throws IOException {

        final String createdWorkflowInstanceId;
        DocumentModel note = RestServerInit.getNote(0, session);
        // Check POST /api/id/{documentId}/@workflow/
        try (CloseableClientResponse response = getResponse(RequestType.POST,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME,
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Check GET /api/id/{documentId}/@workflow/
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            assertEquals(createdWorkflowInstanceId, node.get("entries").elements().next().get("id").textValue());
        }

        // Check GET /api/id/{documentId}/@workflow/{workflowInstanceId}/task
        String taskUid;
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME + "/" + createdWorkflowInstanceId + "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            JsonNode taskNode = node.get("entries").elements().next();
            taskUid = taskNode.get("id").textValue();
        }

        // Check GET /api/id/{documentId}/@task/
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + TaskAdapter.NAME)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            JsonNode taskNode = node.get("entries").elements().next();
            assertEquals(taskUid, taskNode.get("id").textValue());
        }

        // Complete task via task adapter
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "/id/" + note.getId() + "/@" + TaskAdapter.NAME + "/" + taskUid + "/start_review",
                getBodyForStartReviewTaskCompletion(taskUid))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testCreateGetAndCancelWorkflowEndpoint() throws IOException {
        final String createdWorkflowInstanceId;
        // Check POST /workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Check GET /workflow/{workflowInstanceId}
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/workflow/" + createdWorkflowInstanceId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            String fetchedWorkflowInstanceId = node.get("id").textValue();
            assertEquals(createdWorkflowInstanceId, fetchedWorkflowInstanceId);
        }

        // Check GET /workflow .i.e get running workflow initialized by currentUser
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // we expect to retrieve the one previously created
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").elements();
            String fetchedWorkflowInstanceId = elements.next().get("id").textValue();
            assertEquals(createdWorkflowInstanceId, fetchedWorkflowInstanceId);
        }

        String taskId;
        // Check GET /task i.e. pending tasks for current user
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task")) {
            taskId = assertActorIsAdministrator(response);
        }

        // Check GET /task/{taskId}
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task/" + taskId)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Check GET /task?userId=Administrator i.e. pending tasks for Administrator
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("userId", singletonList("Administrator"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            assertActorIsAdministrator(response);
        }

        // Check GET /task?workflowInstanceId={workflowInstanceId} i.e. pending tasks for Administrator
        queryParams.put("workflowInstanceId", singletonList(createdWorkflowInstanceId));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            assertActorIsAdministrator(response);
        }

        // Check DELETE /workflow
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/workflow/" + createdWorkflowInstanceId)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Check GET /workflow
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // we cancel running workflow, we expect 0 running workflow
            assertEquals(0, node.get("entries").size());
        }

        // Check we have no opened tasks
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, node.get("entries").size());
        }
    }

    @Test
    public void testTasksPaginationOffset() throws IOException {
        // Create two dummy tasks
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("offset", "1");
        queryParams.add("isPaginated", "true");
        // Check we get only one task due to offset parameter
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
        }
    }

    @Test
    public void testWorkflowModelEndpoint() throws Exception {

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflowModel")) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, node.get("entries").size());

            Iterator<JsonNode> elements = node.get("entries").elements();

            List<String> expectedNames = Arrays.asList("SerialDocumentReview", "ParallelDocumentReview");
            Collections.sort(expectedNames);
            List<String> realNames = new ArrayList<>();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                realNames.add(element.get("name").textValue());
            }
            Collections.sort(realNames);
            assertEquals(expectedNames, realNames);
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflowModel/SerialDocumentReview")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("SerialDocumentReview", node.get("name").textValue());
        }

        String graphModelPath;
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflowModel/ParallelDocumentReview")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("ParallelDocumentReview", node.get("name").textValue());

            // Check graph resource
            String graphUrl = node.get("graphResource").textValue();
            graphModelPath = "/workflowModel/ParallelDocumentReview/graph";
            assertTrue(graphUrl.endsWith(graphModelPath));
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, graphModelPath)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Instantiate a workflow and check it does not appear as a model
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflowModel")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, node.get("entries").size());
        }
    }

    @Test
    public void testInvalidNodeAction() throws JsonProcessingException {
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
    }

    /**
     * Start and terminate ParallelDocumentReview workflow by completing all its tasks.
     */
    @Test
    public void testTerminateParallelDocumentReviewWorkflow() throws IOException {

        final String createdWorkflowInstanceId;
        DocumentModel note = RestServerInit.getNote(0, session);

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", RunnableWorkflowJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/id/" + note.getId(), headers)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode runnableWorkflowModels = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                               .get(RunnableWorkflowJsonEnricher.NAME);
            // We can start both default workflow on the note
            assertEquals(2, runnableWorkflowModels.size());
        }

        // Start SerialDocumentReview on Note 0
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Complete first task
        String taskId = getCurrentTaskId(createdWorkflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Try to complete first task again
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }

        // Complete second task
        taskId = getCurrentTaskId(createdWorkflowInstanceId);
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/approve",
                getBodyForTaskCompletion(taskId))) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Complete third task
        taskId = getCurrentTaskId(createdWorkflowInstanceId);
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/validate",
                getBodyForTaskCompletion(taskId))) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Workflow must be terminated now
        // Check there are no running workflow
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, node.get("entries").size());
        }

        // Check we have no opened tasks
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, node.get("entries").size());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/id/" + note.getId(), headers)) {

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode runnableWorkflowModels = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                               .get(RunnableWorkflowJsonEnricher.NAME);
            // Cannot start default wf because of current lifecycle state of the note
            assertEquals(0, runnableWorkflowModels.size());
        }
    }

    @Test
    public void testTerminateTaskPermissions() throws IOException {
        final String createdWorkflowInstanceId;
        DocumentModel note = RestServerInit.getNote(0, session);

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", RunnableWorkflowJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/id/" + note.getId(), headers)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode runnableWorkflowModels = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                               .get(RunnableWorkflowJsonEnricher.NAME);
            // We can start both default workflow on the note
            assertEquals(2, runnableWorkflowModels.size());
        }

        // Start SerialDocumentReview on Note 0
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        String taskId = getCurrentTaskId(createdWorkflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);

        // Try to complete task without permissions
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        service = getServiceFor("Administrator", "Administrator");
        // Complete task
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Try to complete first task again
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    /**
     * Start ParallelDocumentReview workflow and try to set a global variable that you are not supposed to.
     */
    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testSecurityCheckOnGlobalVariable() throws IOException {

        final String createdWorkflowInstanceId;

        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Complete first task
        String taskId;
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowInstanceId", singletonList(createdWorkflowInstanceId));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").elements();
            JsonNode task = elements.next();
            JsonNode variables = task.get("variables");

            // Check we don't see global variables we are not supposed to
            assertTrue(variables.has("end_date"));
            assertFalse(variables.has("review_result"));

            taskId = task.get("id").textValue();
        }

        String out = getBodyWithSecurityViolationForStartReviewTaskCompletion(taskId);
        List<LogEvent> events = logResult.getCaughtEvents();
        assertTrue(events.isEmpty());
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Security violation returns OK
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
        // but a warn was logged
        events = logResult.getCaughtEvents();
        assertEquals(1, events.size());

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/workflow/" + createdWorkflowInstanceId)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode workflowInstance = mapper.readTree(response.getEntityInputStream());
            JsonNode variables = workflowInstance.get("variables");
            // and the global variable has not been modified
            assertTrue(variables.get("review_result").isNull());
        }
    }

    @Test
    public void testFilterByWorkflowModelName() throws IOException {
        // Initiate SerialDocumentReview workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // Initiate ParallelDocumentReview workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        // Check GET /task
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, node.get("entries").size());
        }

        // Check GET /task?workflowModelName={workflowModelName} i.e. pending tasks for SerialDocumentReview
        String serialDocumentReviewTaskId;
        queryParams.put("workflowModelName", singletonList("SerialDocumentReview"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").elements();
            JsonNode element = elements.next();
            serialDocumentReviewTaskId = element.get("id").textValue();
        }

        // Check GET /task?workflowModelName={workflowModelName} i.e. pending tasks for ParallelDocumentReview
        queryParams.put("workflowModelName", singletonList("ParallelDocumentReview"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").elements();
            JsonNode element = elements.next();
            String parallelDocumentReviewTaskId = element.get("id").textValue();

            assertNotEquals(serialDocumentReviewTaskId, parallelDocumentReviewTaskId);
        }
    }

    @Test
    public void testMultipleWorkflowInstanceCreation() throws IOException {
        final String workflowModelName1;
        // Initiate a first SerialDocumentReview workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            workflowModelName1 = node.get("workflowModelName").textValue();
        }

        // Initiate a second SerialDocumentReview workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            final String workflowModelName2 = node.get("workflowModelName").textValue();

            assertEquals(workflowModelName1, workflowModelName2);
        }

        // Check we have two pending tasks
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowModelName", singletonList("SerialDocumentReview"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, node.get("entries").size());
        }
    }

    @Test
    public void testMultipleWorkflowInstanceCreation2() throws IOException {
        // Initiate a SerialDocumentReview workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // Initiate a ParallelDocumentReview workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // Check GET /workflow?workflowMnodelName=SerialDocumentReview
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowModelName", singletonList("SerialDocumentReview"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow", null, queryParams, null,
                null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
        }

        // Check GET /workflow?workflowMnodelName=ParallelDocumentReview
        queryParams.put("workflowModelName", singletonList("ParallelDocumentReview"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow", null, queryParams, null,
                null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
        }
    }

    @Test
    public void testDelegateTask() throws IOException {
        final String createdWorkflowInstanceId;
        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Complete first task
        String taskId = getCurrentTaskId(createdWorkflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Delegate
        taskId = getCurrentTaskId(createdWorkflowInstanceId);
        List<String> delegatedActors = Arrays.asList("members", "Administrator");
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("delegatedActors", delegatedActors);
        queryParams.put("comment", singletonList("A comment"));
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/delegate", null,
                queryParams, null, null)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task/" + taskId, null, queryParams, null,
                null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode delegatedActorsNode = node.get("delegatedActors");
            assertNotNull(delegatedActorsNode);
            assertTrue(delegatedActorsNode.isArray());
            assertEquals(2, delegatedActorsNode.size());
            assertThatContainsActors(delegatedActors, delegatedActorsNode);
        }
    }

    /**
     * @since 9.1
     */
    @Test
    public void testTaskWithGroupAssignee() throws IOException {
        final String createdWorkflowInstanceId;
        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Complete first task
        String taskId = getCurrentTaskId(createdWorkflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId, "group:administrators");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Check GET /task i.e. pending tasks for current user
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("userId", singletonList("Administrator"));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
        }
    }

    @Test
    public void testReassignTask() throws IOException {
        final String createdWorkflowInstanceId;
        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Complete first task
        String taskId = getCurrentTaskId(createdWorkflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Reassign
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, null, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            Iterator<JsonNode> elements = node.get("entries").elements();
            node = elements.next();
            assertThatContainsActors(singletonList("user:Administrator"), node.get("actors"));
            taskId = node.get("id").textValue();
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("actors", singletonList("members"));
        queryParams.put("comment", singletonList("A comment"));
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/reassign", null,
                queryParams, null, null)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null, null)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            node = node.get("entries").elements().next();
            assertThatContainsActors(singletonList("members"), node.get("actors"));
        }
    }

    /**
     * @deprecated Since 10.3. Use {@link #assertThatContainsActors} instead.
     */
    @Deprecated
    protected static void assertActorIs(String expectedActor, JsonNode taskNode) {
        assertThatContainsActors(Collections.singletonList(expectedActor), taskNode.get("actors"));
    }

    protected static void assertThatContainsActors(List<String> expectedActors, JsonNode actorsNode) {
        Iterator<JsonNode> actorNode = actorsNode.elements();
        List<String> actors = new ArrayList<>();
        while (actorNode.hasNext()) {
            actors.add(actorNode.next().get("id").textValue());
        }
        assertEquals(expectedActors.size(), actors.size());
        assertTrue(actors.containsAll(expectedActors));
    }

    @Test
    public void testTaskActionUrls() throws IOException {
        final String createdWorkflowInstanceId;
        // Check POST /workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Check GET /workflow/{workflowInstanceId}
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/workflow/" + createdWorkflowInstanceId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            String fetchedWorkflowInstanceId = node.get("id").textValue();
            assertEquals(createdWorkflowInstanceId, fetchedWorkflowInstanceId);
        }

        // Check GET /workflow .i.e get running workflow initialized by currentUser
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // we expect to retrieve the one previously created
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").elements();
            String fetchedWorkflowInstanceId = elements.next().get("id").textValue();
            assertEquals(createdWorkflowInstanceId, fetchedWorkflowInstanceId);
        }

        // Check GET /task i.e. pending tasks for current user
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            JsonNode element = node.get("entries").elements().next();
            assertNotNull(element);
            JsonNode taskInfo = element.get("taskInfo");
            assertNotNull(taskInfo);
            JsonNode taskActions = taskInfo.get("taskActions");
            assertEquals(2, taskActions.size());
            JsonNode taskAction = taskActions.elements().next();
            assertNotNull(taskAction);
            assertEquals(String.format(getRestApiUrl() + "task/%s/cancel", element.get("id").textValue()),
                    taskAction.get("url").textValue());
        }
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFetchWfInitiator() throws IOException {

        final String createdWorkflowInstanceId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + DocumentRouteWriter.ENTITY_TYPE, DocumentRouteWriter.FETCH_INITATIOR);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow/" + createdWorkflowInstanceId,
                queryParams)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode initiatorNode = node.get("initiator");
            assertEquals("Administrator", initiatorNode.get("id").textValue());
            JsonNode initiatorProps = initiatorNode.get("properties");
            assertEquals(1, initiatorProps.get("groups").size());
            assertEquals("administrators", initiatorProps.get("groups").get(0).textValue());
            // For the sake of security
            assertNull(initiatorNode.get("properties").get("password"));
        }
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFetchTaskActors() throws IOException {

        final String createdWorkflowInstanceId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + TaskWriter.ENTITY_TYPE, TaskWriter.FETCH_ACTORS);

        JsonNode task = getCurrentTask(createdWorkflowInstanceId, queryParams);

        ArrayNode taskActors = (ArrayNode) task.get("actors");
        assertEquals(1, taskActors.size());
        assertEquals("Administrator", taskActors.get(0).get("id").textValue());
        // For the sake of security
        assertNull(taskActors.get(0).get("properties").get("password"));
    }

    /**
     * @since 8.3
     */
    @Test
    public void testTasksEnricher() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            mapper.readTree(response.getEntityInputStream());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", PendingTasksJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/id/" + note.getId(), headers)) {

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode tasksNode = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                  .get(PendingTasksJsonEnricher.NAME);
            assertEquals(1, tasksNode.size());
            ArrayNode targetDocumentIdsNode = (ArrayNode) tasksNode.get(0).get(TaskWriter.TARGET_DOCUMENT_IDS);
            assertEquals(1, targetDocumentIdsNode.size());
            assertEquals(note.getId(), targetDocumentIdsNode.get(0).get("id").textValue());
        }
    }

    /**
     * @since 8.3
     */
    @Test
    public void testRunningWorkflowEnricher() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            mapper.readTree(response.getEntityInputStream());
        }

        // Grant READ to user1 on the note and login as user1
        ACP acp = note.getACP();
        ACE ace = ACE.builder("user1", SecurityConstants.READ).build();
        acp.addACE(ACL.LOCAL_ACL, ace);

        note.setACP(acp, true);
        note = session.saveDocument(note);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        this.service = getServiceFor("user1", "user1");

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", RunningWorkflowJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/id/" + note.getId(), headers)) {

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode workflowsNode = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                      .get(RunningWorkflowJsonEnricher.NAME);
            assertEquals(1, workflowsNode.size());
            ArrayNode attachedDocumentIdsNode = (ArrayNode) workflowsNode.get(
                    0).get(DocumentRouteWriter.ATTACHED_DOCUMENT_IDS);
            assertEquals(1, attachedDocumentIdsNode.size());
            assertEquals(note.getId(), attachedDocumentIdsNode.get(0).get("id").textValue());
        }
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFetchTaskTargetDocuments() throws IOException {
        final String createdWorkflowInstanceId;
        DocumentModel note = RestServerInit.getNote(0, session);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + TaskWriter.ENTITY_TYPE, TaskWriter.FETCH_TARGET_DOCUMENT);

        JsonNode task = getCurrentTask(createdWorkflowInstanceId, queryParams);

        ArrayNode taskTargetDocuments = (ArrayNode) task.get(TaskWriter.TARGET_DOCUMENT_IDS);
        assertEquals(1, taskTargetDocuments.size());
        assertEquals(note.getId(), taskTargetDocuments.get(0).get("uid").textValue());

        // Don't fetch the target documents and check that "targetDocumentIds" contains a list of document ids
        // instead of document objects
        task = getCurrentTask(createdWorkflowInstanceId);

        taskTargetDocuments = (ArrayNode) task.get(TaskWriter.TARGET_DOCUMENT_IDS);
        assertEquals(1, taskTargetDocuments.size());
        assertEquals(note.getId(), taskTargetDocuments.get(0).get("id").textValue());
    }

    /**
     * Same as {@link #testFetchTaskTargetDocuments()} with the {@code DeleteTaskForDeletedDocumentListener} disabled to
     * check the behavior when a task targeting a deleted document remains.
     *
     * @since 9.3
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.server.routing:test-disable-task-deletion-listener.xml")
    public void testFetchTaskTargetDocumentsDeleted() throws IOException {
        final String createdWorkflowInstanceId;
        DocumentModel note = RestServerInit.getNote(0, session);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Remove the task's target document
        session.removeDocument(note.getRef());
        txFeature.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + TaskWriter.ENTITY_TYPE, TaskWriter.FETCH_TARGET_DOCUMENT);

        JsonNode task = getCurrentTask(createdWorkflowInstanceId, queryParams);

        ArrayNode taskTargetDocuments = (ArrayNode) task.get(TaskWriter.TARGET_DOCUMENT_IDS);
        assertEquals(0, taskTargetDocuments.size());

        // Don't fetch the target documents and check that "targetDocumentIds" still contains an empty list
        task = getCurrentTask(createdWorkflowInstanceId);

        taskTargetDocuments = (ArrayNode) task.get(TaskWriter.TARGET_DOCUMENT_IDS);
        assertEquals(0, taskTargetDocuments.size());
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFetchWorkflowAttachedDocuments() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + DocumentRouteWriter.ENTITY_TYPE, DocumentRouteWriter.FETCH_ATTACHED_DOCUMENTS);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", singletonList(note.getId())), queryParams,
                null, null)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());

            ArrayNode wfAttachedDocuments = (ArrayNode) node.get(DocumentRouteWriter.ATTACHED_DOCUMENT_IDS);
            assertEquals(1, wfAttachedDocuments.size());
            assertEquals(note.getId(), wfAttachedDocuments.get(0).get("uid").textValue());
        }
    }

    /**
     * Trigger the escalation rule that resumes a ParallelDocumentReview workflow instance of which all attached
     * documents have been deleted.
     * <p>
     * The expected behaviour is that workflow instance is cancelled.
     *
     * @since 8.4
     */
    @Test
    public void testResumeWorkflowWithDeletedAttachedDoc() throws IOException, InterruptedException {
        final String createdWorkflowInstanceId;
        DocumentModel note = RestServerInit.getNote(0, session);

        // Start SerialDocumentReview on Note 0
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Complete first task
        String taskId = getCurrentTaskId(createdWorkflowInstanceId);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        String out = getBodyForStartReviewTaskCompletion(taskId, calendar.getTime());
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review",
                out)) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Let's remove the attached document.
        session.removeDocument(note.getRef());

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        EventContext eventContext = new EventContextImpl();
        eventContext.setProperty("category", "escalation");
        Event event = new EventImpl(DocumentRoutingEscalationListener.EXECUTE_ESCALATION_RULE_EVENT, eventContext);
        eventService.fireEvent(event);

        awaitEscalationWorks();

        // Check GET /workflow/{workflowInstanceId}
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/workflow")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // we expect that the workflow has been canceled because of deleted documents
            assertEquals(0, node.get("entries").size());
        }
    }

    /**
     * @since 9.1
     */
    @Test
    public void testWorkflowCleanUp() throws Exception {
        createWorkflowsThenWaitForCleanup();
        DocumentModelList cancelled = session.query(CANCELLED_WORKFLOWS);
        assertTrue(cancelled.isEmpty());
    }

    /**
     * @since 10.2
     */
    @Test
    public void testWorkflowCleanUpDisabling() throws Exception {
        Framework.getProperties()
                 .put(DocumentRoutingWorkflowInstancesCleanup.CLEANUP_WORKFLOW_INSTANCES_PROPERTY, "true");
        try {
            createWorkflowsThenWaitForCleanup();
            DocumentModelList cancelled = session.query(CANCELLED_WORKFLOWS);
            assertEquals(NB_WF, cancelled.size());
        } finally {
            Framework.getProperties()
                     .remove(DocumentRoutingWorkflowInstancesCleanup.CLEANUP_WORKFLOW_INSTANCES_PROPERTY);
        }
    }

    protected void createWorkflowsThenWaitForCleanup() throws Exception {
        DocumentModel note = RestServerInit.getNote(0, session);
        for (int i = 0; i < NB_WF; i++) {
            final String createdWorkflowInstanceId;
            try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                    getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId())))) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
                JsonNode node = mapper.readTree(response.getEntityInputStream());
                createdWorkflowInstanceId = node.get("id").textValue();
            }
            // Cancel the workflow
            try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                    "/workflow/" + createdWorkflowInstanceId)) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
        }

        // Starts a new transaction for visibility on db that use repeatable read isolation (mysql, mariadb)
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModelList cancelled = session.query(CANCELLED_WORKFLOWS);
        assertEquals(NB_WF, cancelled.size());

        EventContext eventContext = new EventContextImpl();
        eventContext.setProperty("category", "workflowInstancesCleanup");
        Event event = new EventImpl(DocumentRoutingWorkflowInstancesCleanup.CLEANUP_WORKFLOW_EVENT_NAME, eventContext);
        eventService.fireEvent(event);

        txFeature.nextTransaction();
    }

    /**
     * @since 9.3
     */
    @Test
    public void testTaskWithoutWorkflowInstance() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        // Create a task not related to a workflow instance
        List<Task> tasks = Framework.getService(TaskService.class)
                                    .createTask(session, session.getPrincipal(), note, "testNoWorkflowTask",
                                            singletonList("user:Administrator"), false, null, null, null,
                                            Collections.emptyMap(), null);
        assertEquals(1, tasks.size());
        Task task = tasks.get(0);
        txFeature.nextTransaction();

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", PendingTasksJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/id/" + note.getId(), headers)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode tasksNode = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                  .get(PendingTasksJsonEnricher.NAME);
            assertEquals(1, tasksNode.size());

            JsonNode taskNode = tasksNode.get(0);
            assertEquals(task.getId(), taskNode.get("id").textValue());
            assertEquals("testNoWorkflowTask", taskNode.get("name").textValue());
            assertTrue(taskNode.get("workflowInstanceId").isNull());
            ArrayNode targetDocumentIdsNode = (ArrayNode) taskNode.get(TaskWriter.TARGET_DOCUMENT_IDS);
            assertEquals(1, targetDocumentIdsNode.size());
            assertEquals(note.getId(), targetDocumentIdsNode.get(0).get("id").textValue());
            assertThatContainsActors(singletonList("user:Administrator"), taskNode.get("actors"));
            assertEquals(0, taskNode.get("variables").size());
        }
    }

    /**
     * @since 10.1
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.server.routing:test-fetchReferences-false.xml")
    public void testTaskWorkflowInfo() throws IOException {
        final String createdWorkflowInstanceId;
        // Create a workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        // Fetch the user's tasks and check the workflow related info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            JsonNode taskNode = node.get("entries").elements().next();
            assertEquals(createdWorkflowInstanceId, taskNode.get("workflowInstanceId").textValue());
            assertTrue(taskNode.has("taskInfo"));
            assertTrue(taskNode.get("taskInfo").has("allowTaskReassignment"));
            assertFalse(taskNode.get("taskInfo").get("allowTaskReassignment").booleanValue());

            JsonNode actorsNode = taskNode.get("actors");
            assertNotNull(actorsNode);
            assertTrue(actorsNode.isArray());
            assertEquals(1, actorsNode.size());
            assertThatContainsActors(singletonList("Administrator"), actorsNode);

            JsonNode delegatedActorsNode = taskNode.get("delegatedActors");
            assertNotNull(delegatedActorsNode);
            assertTrue(delegatedActorsNode.isArray());
            assertEquals(0, delegatedActorsNode.size());

            assertEquals("SerialDocumentReview", taskNode.get("workflowModelName").textValue());
            assertEquals("Administrator", taskNode.get("workflowInitiator").textValue());
            assertEquals("wf.serialDocumentReview.SerialDocumentReview", taskNode.get("workflowTitle").textValue());
            assertEquals("running", taskNode.get("workflowLifeCycleState").textValue());
            assertEquals(String.format(getRestApiUrl() + "workflow/%s/graph", createdWorkflowInstanceId),
                    taskNode.get("graphResource").textValue());
        }

        // Check the workflowInitiator task fetch property
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + TaskWriter.ENTITY_TYPE, TaskWriter.FETCH_WORKFLOW_INITATIOR);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task/", queryParams)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode taskNode = node.get("entries").elements().next();
            JsonNode initiatorNode = taskNode.get("workflowInitiator");
            assertEquals("user", initiatorNode.get("entity-type").textValue());
            assertEquals("Administrator", initiatorNode.get("id").textValue());
            JsonNode properties = initiatorNode.get("properties");
            ArrayNode groups = (ArrayNode) properties.get("groups");
            JsonNode isPartial = initiatorNode.get("isPartial");
            if (isPartial != null && isPartial.booleanValue()) {
                assertFalse(initiatorNode.get("isAdministrator").booleanValue());
                assertEquals(groups.size(), 0);
            } else {
                assertTrue(initiatorNode.get("isAdministrator").booleanValue());
                assertEquals(1, groups.size());
                assertEquals("administrators", groups.get(0).textValue());

            }
        }
    }

    /**
     * @since 10.3
     */
    @Test
    public void testReassignableTaskWorkflowBasicInfo() throws IOException {
        final String createdWorkflowInstanceId;
        // Create a workflow
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview"))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdWorkflowInstanceId = node.get("id").textValue();
        }

        final String taskId;

        // Fetch the user's tasks and check the workflow related info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            JsonNode taskNode = node.get("entries").elements().next();
            assertEquals(createdWorkflowInstanceId, taskNode.get("workflowInstanceId").textValue());
            assertNotNull(taskNode.get("id").textValue());

            taskId = taskNode.get("id").textValue();
        }

        // Complete Task
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                String.format("/task/%s/start_review", taskId), getBodyForChooseParticipantsTaskCompletion(taskId))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task/")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode taskNode = node.get("entries").elements().next();

            assertTrue(taskNode.has("entity-type"));
            assertEquals("task", taskNode.get("entity-type").textValue());

            assertTrue(taskNode.has("name"));
            assertEquals("wf.serialDocumentReview.DocumentValidation", taskNode.get("name").textValue());

            assertTrue(taskNode.has("workflowInstanceId"));
            assertEquals(createdWorkflowInstanceId, taskNode.get("workflowInstanceId").textValue());

            assertTrue(taskNode.has("workflowModelName"));
            assertEquals("SerialDocumentReview", taskNode.get("workflowModelName").textValue());

            assertTrue(taskNode.has("workflowInitiator"));
            assertEquals("Administrator", taskNode.get("workflowInitiator").textValue());

            assertTrue(taskNode.has("workflowTitle"));
            assertEquals("wf.serialDocumentReview.SerialDocumentReview", taskNode.get("workflowTitle").textValue());

            assertTrue(taskNode.has("workflowLifeCycleState"));
            assertEquals("running", taskNode.get("workflowLifeCycleState").textValue());

            assertTrue(taskNode.has("directive"));
            assertEquals("wf.serialDocumentReview.AcceptReject", taskNode.get("directive").textValue());

            assertTrue(taskNode.has("taskInfo"));
            assertTrue(taskNode.get("taskInfo").has("allowTaskReassignment"));
            assertTrue(taskNode.get("taskInfo").get("allowTaskReassignment").booleanValue());
        }
    }

    /**
     * NXP-25029
     *
     * @since 10.2
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.server.routing.test:test-reject-task-in-sub-workflow.xml")
    public void testRejectTaskInSubWorkflow() throws IOException {
        final String createdMainWorkflowInstanceId;
        final String createdChildWorkflowInstanceId;

        DocumentModel note = RestServerInit.getNote(0, session);
        // create main workflow as Administrator
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("MainWF", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            createdMainWorkflowInstanceId = node.get("id").textValue();
        }

        // check childWF has started GET /api/id/{documentId}/@workflow
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, node.get("entries").size());
            JsonNode firstWF = node.get("entries").get(0);
            JsonNode secondWF = node.get("entries").get(1);
            if (createdMainWorkflowInstanceId.equals(firstWF.get("id").textValue())) {
                assertEquals("MainWF", firstWF.get("workflowModelName").textValue());
                assertEquals("ChildWF", secondWF.get("workflowModelName").textValue());
                createdChildWorkflowInstanceId = secondWF.get("id").textValue();
            } else {
                assertEquals("MainWF", secondWF.get("workflowModelName").textValue());
                assertEquals("ChildWF", firstWF.get("workflowModelName").textValue());
                createdChildWorkflowInstanceId = firstWF.get("id").textValue();
            }
        }

        // get tasks for child WF
        final String taskUser1Id;
        final String taskUser2Id;
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME + "/" + createdChildWorkflowInstanceId + "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, node.get("entries").size());
            taskUser1Id = node.get("entries").get(0).get("id").textValue();
            taskUser2Id = node.get("entries").get(1).get("id").textValue();
        }

        // login as user1
        getServiceFor("user1", "user1");

        // reject task for user1
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskUser1Id + "/reject",
                getBodyForStartReviewTaskCompletion(taskUser1Id))) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // login as user2
        getServiceFor("user2", "user2");

        // reject task for user2
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskUser2Id + "/reject",
                getBodyForStartReviewTaskCompletion(taskUser2Id))) {
            // Missing required variables
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // login as Administrator
        getServiceFor("Administrator", "Administrator");

        // As both tasks have been rejected, administrator wouldn't have anything to do.
        // get tasks for child WF
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME + "/" + createdChildWorkflowInstanceId + "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, node.get("entries").size());
        }

        // get tasks for main WF
        // there should be no entries, both reject leads to no tasks for administrator
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME + "/" + createdMainWorkflowInstanceId + "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, node.get("entries").size());
        }

    }

    /*
     * NXP-29171
     * NXP-30658
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.server.routing.test:test-specific-task-request-unmarshalling.xml")
    public void testSpecificTaskRequestWithFetchedGroup() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        // create workflow as Administrator
        String workflowInstanceId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("confirm", singletonList(note.getId())))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            workflowInstanceId = node.get("id").textValue();
        }

        // get tasks for child WF
        String taskId;
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME + "/" + workflowInstanceId + "/task")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            taskId = node.get("entries").get(0).get("id").textValue();
        }

        NuxeoGroup group = userManager.getGroup("members");

        String groupJson = MarshallerHelper.objectToJson(group, RenderingContext.CtxBuilder.get());
        String body = String.format("{\"entity-type\":\"task\", \"id\":\"%s\", \"variables\":{\"assignees\":[%s]}}",
                taskId, groupJson);
        // assign it with a fetched entity
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/task/" + taskId + "/confirm", body)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());

            JsonNode nodeVariables = node.get("variables");
            assertNotNull(nodeVariables);

            JsonNode nodeAssignees = nodeVariables.get("assignees");
            assertNotNull(nodeAssignees);
            assertTrue(nodeAssignees.isArray());

            ArrayNode arrayAssignees = (ArrayNode) nodeAssignees;
            assertEquals("Number of assignees is wrong", 1, arrayAssignees.size());
            assertEquals("group:members", arrayAssignees.get(0).textValue());
        }

    }

    /**
     * NXP-29578
     */
    @Test
    public void testStartWorkflowWithVariables() throws IOException {
        Map<String, Serializable> variables = new HashMap<>();
        variables.put("initiatorComment", "workflow start");
        variables.put("validationOrReview", "review");
        DocumentModel note = RestServerInit.getNote(0, session);
        // with workflow adapter
        try (CloseableClientResponse response = getResponse(RequestType.POST,
                "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME,
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null, variables))) {
            assertWorkflowCreatedWithVariables(response, variables);
        }

        // with workflow endpoint
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", singletonList(note.getId()),
                        variables))) {
            assertWorkflowCreatedWithVariables(response, variables);
        }
    }

    protected void assertWorkflowCreatedWithVariables(CloseableClientResponse response,
            Map<String, Serializable> expectedVariables) throws IOException {
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        JsonNode variablesNode = node.get("variables");
        expectedVariables.forEach((k, v) -> assertEquals(v, variablesNode.get(k).asText()));
    }

    protected void awaitEscalationWorks() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        workManager.awaitCompletion("escalation", 10, TimeUnit.SECONDS);
    }

}
