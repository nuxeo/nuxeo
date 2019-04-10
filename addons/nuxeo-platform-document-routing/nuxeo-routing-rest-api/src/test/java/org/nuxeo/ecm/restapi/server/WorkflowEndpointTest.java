/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.restapi.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter.TaskAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter.WorkflowAdapter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import com.ibm.icu.util.Calendar;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, WorkflowFeature.class, AuditFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.ecm.platform.restapi.server.routing", "org.nuxeo.ecm.automation.test",
        "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.platform.restapi.io", "org.nuxeo.ecm.platform.restapi.test",
        "org.nuxeo.ecm.platform.restapi.server", "org.nuxeo.ecm.platform.routing.default",
        "org.nuxeo.ecm.platform.filemanager.api", "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.actions" })
public class WorkflowEndpointTest extends BaseTest {

    protected String assertActorIsAdministrator(ClientResponse response) throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        JsonNode element = elements.next();
        String taskId = element.get("id").getTextValue();
        JsonNode actors = element.get("actors");
        assertEquals(1, actors.size());
        String actor = actors.getElements().next().get("id").getTextValue();
        assertEquals("Administrator", actor);
        return taskId;
    }

    protected String getBodyForStartReviewTaskCompletion(String taskId) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        String jsonBody = "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\","
                + "\"entity-type\": \"task\"," + "\"variables\": {" + "\"end_date\": \""
                + DateParser.formatW3CDateTime(calendar.getTime()) + "\","
                + "\"participants\": [\"user:Administrator\"],"
                + "\"assignees\": [\"user:Administrator\"]" + "}" + "}";
        return jsonBody;
    }

    protected String getBodyWithSecurityViolationForStartReviewTaskCompletion(String taskId) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        String jsonBody = "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\","
                + "\"entity-type\": \"task\"," + "\"variables\": {" + "\"end_date\": \""
                + DateParser.formatW3CDateTime(calendar.getTime()) + "\","
                + "\"participants\": [\"user:Administrator\"]," + "\"review_result\": \"blabablaa\"" + "}"
                + "}";
        return jsonBody;
    }

    protected String getBodyForTaskCompletion(String taskId) throws IOException {
        return "{\"entity-type\": \"task\", " + "\"id\": \"" + taskId + "\"}";
    }

    protected String getCreateAndStartWorkflowBodyContent(String workflowName, List<String> docIds) throws IOException {
        String result = "{\"entity-type\": \"workflow\", " + "\"workflowModelName\": \"" + workflowName + "\"";
        if (docIds != null && !docIds.isEmpty()) {
            result += ", " + "\"attachedDocumentIds\": [";
            for (int i = 0; i < docIds.size(); i++) {
                result += "\"" + docIds.get(i) + "\"";
            }
            result += "]";
        }

        result += "}";
        return result;
    }

    protected String getCurrentTaskId(final String createdWorflowInstanceId) throws IOException,
            JsonProcessingException {
        ClientResponse response;
        JsonNode node;
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowInstanceId", Arrays.asList(new String[] { createdWorflowInstanceId }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        String taskId = elements.next().get("id").getTextValue();
        return taskId;
    }

    @Test
    public void testAdapter() throws IOException {

        DocumentModel note = RestServerInit.getNote(0, session);
        // Check POST /api/id/{documentId}/@workflow/
        ClientResponse response = getResponse(RequestType.POST, "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME,
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Check GET /api/id/{documentId}/@workflow/
        response = getResponse(RequestType.GET, "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        assertEquals(createdWorflowInstanceId, node.get("entries").getElements().next().get("id").getTextValue());

        // Check GET /api/id/{documentId}/@workflow/{workflowInstanceId}/task
        response = getResponse(RequestType.GET, "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME + "/"
                + createdWorflowInstanceId + "/task");
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        JsonNode taskNode = node.get("entries").getElements().next();
        String taskUid = taskNode.get("id").getTextValue();

        // Check GET /api/id/{documentId}/@task/
        response = getResponse(RequestType.GET, "/id/" + note.getId() + "/@" + TaskAdapter.NAME);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        taskNode = node.get("entries").getElements().next();
        assertEquals(taskUid, taskNode.get("id").getTextValue());

        // Complete task via task adapter
        response = getResponse(RequestType.PUT, "/id/" + note.getId() + "/@" + TaskAdapter.NAME + "/" + taskUid
                + "/start_review", getBodyForStartReviewTaskCompletion(taskUid).toString());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateGetAndCancelWorkflowEndpoint() throws IOException {
        // Check POST /workflow
        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Check GET /workflow/{workflowInstanceId}
        response = getResponse(RequestType.GET, "/workflow/" + createdWorflowInstanceId);
        node = mapper.readTree(response.getEntityInputStream());
        String fetchedWorflowInstanceId = node.get("id").getTextValue();
        assertEquals(createdWorflowInstanceId, fetchedWorflowInstanceId);

        // Check GET /workflow .i.e get running workflow initialized by currentUser
        response = getResponse(RequestType.GET, "/workflow");
        node = mapper.readTree(response.getEntityInputStream());
        // we expect to retrieve the one previously created
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        fetchedWorflowInstanceId = elements.next().get("id").getTextValue();
        assertEquals(createdWorflowInstanceId, fetchedWorflowInstanceId);

        // Check GET /task i.e. pending tasks for current user
        response = getResponse(RequestType.GET, "/task");
        String taskId = assertActorIsAdministrator(response);

        // Check GET /task/{taskId}
        response = getResponse(RequestType.GET, "/task/" + taskId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Check GET /task?userId=Administrator i.e. pending tasks for Administrator
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("userId", Arrays.asList(new String[] { "Administrator" }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        assertActorIsAdministrator(response);

        // Check GET /task?workflowInstanceId={workflowInstanceId} i.e. pending tasks for Administrator
        queryParams.put("workflowInstanceId", Arrays.asList(new String[] { createdWorflowInstanceId }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        assertActorIsAdministrator(response);

        // Check DELETE /workflow
        response = getResponse(RequestType.DELETE, "/workflow/" + createdWorflowInstanceId);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // Check GET /workflow
        response = getResponse(RequestType.GET, "/workflow");
        node = mapper.readTree(response.getEntityInputStream());
        // we cancel running workflow, we expect 0 running workflow
        assertEquals(0, node.get("entries").size());

        // Check we have no opened tasks
        response = getResponse(RequestType.GET, "/task");
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(0, node.get("entries").size());

    }

    @Test
    public void testWorkflowModelEndpoint() throws Exception {

        ClientResponse response = getResponse(RequestType.GET, "/workflowModel");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, node.get("entries").size());

        Iterator<JsonNode> elements = node.get("entries").getElements();

        List<String> expectedNames = Arrays.asList(new String[] { "SerialDocumentReview", "ParallelDocumentReview" });
        Collections.sort(expectedNames);
        List<String> realNames = new ArrayList<String>();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            realNames.add(element.get("name").getTextValue());
        }
        Collections.sort(realNames);
        assertEquals(expectedNames, realNames);

        response = getResponse(RequestType.GET, "/workflowModel/SerialDocumentReview");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("SerialDocumentReview", node.get("name").getTextValue());

        response = getResponse(RequestType.GET, "/workflowModel/ParallelDocumentReview");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("ParallelDocumentReview", node.get("name").getTextValue());

        // Check graph resource
        String graphUrl = node.get("graphResource").getTextValue();
        String graphModelPath = "/workflowModel/ParallelDocumentReview/graph";
        assertTrue(graphUrl.endsWith(graphModelPath));
        response = getResponse(RequestType.GET, graphModelPath);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Instantiate a workflow and check it does not appear as a model
        response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        response = getResponse(RequestType.GET, "/workflowModel");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, node.get("entries").size());
    }

    @Test
    public void testInvalidNodeAction() throws IOException {
        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    /**
     * Start and terminate ParallelDocumentReview workflow by completing all its tasks.
     */
    @Test
    public void testTerminateParallelDocumentReviewWorkflow() throws JsonProcessingException, IOException {

        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(
                RequestType.POST,
                "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview",
                        Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Complete first task
        String taskId = getCurrentTaskId(createdWorflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review", out.toString());
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Complete second task
        taskId = getCurrentTaskId(createdWorflowInstanceId);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/approve", getBodyForTaskCompletion(taskId));
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Complete third task
        taskId = getCurrentTaskId(createdWorflowInstanceId);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/validate", getBodyForTaskCompletion(taskId));
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Worfklow must be terminated now
        // Check there are no running workflow
        response = getResponse(RequestType.GET, "/workflow");
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(0, node.get("entries").size());

        // Check we have no opened tasks
        response = getResponse(RequestType.GET, "/task");
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(0, node.get("entries").size());

    }

    /**
     * Start ParallelDocumentReview workflow and try to set a global variable that you are not supposed to.
     */
    @Test
    public void testSecurityCheckOnGlobalVariable() throws JsonProcessingException, IOException {

        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(
                RequestType.POST,
                "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview",
                        Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Complete first task
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowInstanceId", Arrays.asList(new String[] { createdWorflowInstanceId }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        JsonNode task = elements.next();
        JsonNode variables = task.get("variables");

        // Check we don't see global variables we are not supposed to
        assertTrue(variables.has("end_date"));
        assertFalse(variables.has("review_result"));

        String taskId = task.get("id").getTextValue();

        String out = getBodyWithSecurityViolationForStartReviewTaskCompletion(taskId);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review", out.toString());
        // Missing required variables
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        final String responseEntityType = node.get("entity-type").getTextValue();
        final String responseMessage = node.get("message").getTextValue();
        assertEquals("exception", responseEntityType);
        assertEquals("You don't have the permission to set the workflow variable review_result", responseMessage);
    }

    @Test
    public void testFilterByWorkflowModelName() throws IOException {
        // Initiate SerialDocumentReview workflow
        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // Initiate ParallelDocumentReview workflow
        response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        // Check GET /task
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, node.get("entries").size());

        // Check GET /task?workflowModelName={workflowModelName} i.e. pending tasks for SerialDocumentReview
        queryParams.put("workflowModelName", Arrays.asList(new String[] { "SerialDocumentReview" }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        JsonNode element = elements.next();
        String serialDocumentReviewTaskId = element.get("id").getTextValue();

        // Check GET /task?workflowModelName={workflowModelName} i.e. pending tasks for ParallelDocumentReview
        queryParams.put("workflowModelName", Arrays.asList(new String[] { "ParallelDocumentReview" }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        elements = node.get("entries").getElements();
        element = elements.next();
        String parallelDocumentReviewTaskId = element.get("id").getTextValue();

        assertNotEquals(serialDocumentReviewTaskId, parallelDocumentReviewTaskId);
    }

    @Test
    public void testMultipleWorkflowInstanceCreation() throws IOException {
        // Initiate a first SerialDocumentReview workflow
        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String workflowModelName1 = node.get("workflowModelName").getTextValue();

        // Initiate a second SerialDocumentReview workflow
        response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        final String workflowModelName2 = node.get("workflowModelName").getTextValue();

        assertEquals(workflowModelName1, workflowModelName2);

        // Check we have two pending tasks
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowModelName", Arrays.asList(new String[] { "SerialDocumentReview" }));
        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, node.get("entries").size());

    }

    @Test
    public void testMultipleWorkflowInstanceCreation2() throws IOException {
        // Initiate a SerialDocumentReview workflow
        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // Initiate a ParallelDocumentReview workflow
        response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());

        // Check GET /workflow?workflowMnodelName=SerialDocumentReview
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("workflowModelName", Arrays.asList(new String[] { "SerialDocumentReview" }));
        response = getResponse(RequestType.GET, "/workflow", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());

        // Check GET /workflow?workflowMnodelName=ParallelDocumentReview
        queryParams.put("workflowModelName", Arrays.asList(new String[] { "ParallelDocumentReview" }));
        response = getResponse(RequestType.GET, "/workflow", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
    }

    @Test
    public void testDelegateTask() throws IOException {
        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(
                RequestType.POST,
                "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview",
                        Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Complete first task
        String taskId = getCurrentTaskId(createdWorflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review", out.toString());
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Delegate
        taskId = getCurrentTaskId(createdWorflowInstanceId);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("actors", Arrays.asList(new String[] { "members" }));
        queryParams.put("comment", Arrays.asList(new String[] { "A comment" }));
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/delegate", null, queryParams, null, null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

    @Test
    public void testReassignTask() throws IOException {
        // Start SerialDocumentReview on Note 0
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(
                RequestType.POST,
                "/workflow",
                getCreateAndStartWorkflowBodyContent("ParallelDocumentReview",
                        Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Complete first task
        String taskId = getCurrentTaskId(createdWorflowInstanceId);
        String out = getBodyForStartReviewTaskCompletion(taskId);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review", out.toString());
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Reassign
        response = getResponse(RequestType.GET, "/task", null, null, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        node = elements.next();
        assertActorIs("user:Administrator", node);
        taskId = node.get("id").getTextValue();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("actors", Arrays.asList(new String[] { "members" }));
        queryParams.put("comment", Arrays.asList(new String[] { "A comment" }));
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/reassign", null, queryParams, null, null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = getResponse(RequestType.GET, "/task", null, queryParams, null, null);
        node = mapper.readTree(response.getEntityInputStream());
        node = node.get("entries").getElements().next();
        assertActorIs("members", node);
    }

    protected static void assertActorIs(String expectedActor, JsonNode taskNode) {
        Iterator<JsonNode> actorNode = taskNode.get("actors").getElements();
        List<String> actors = new ArrayList<String>();
        while (actorNode.hasNext()) {
            actors.add(actorNode.next().get("id").getTextValue());
        }
        assertEquals(1, actors.size());
        assertEquals(expectedActor, actors.get(0));
    }

    @Test
    public void testTaskActionUrls() throws IOException {
        // Check POST /workflow
        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        // Check GET /workflow/{workflowInstanceId}
        response = getResponse(RequestType.GET, "/workflow/" + createdWorflowInstanceId);
        node = mapper.readTree(response.getEntityInputStream());
        String fetchedWorflowInstanceId = node.get("id").getTextValue();
        assertEquals(createdWorflowInstanceId, fetchedWorflowInstanceId);

        // Check GET /workflow .i.e get running workflow initialized by currentUser
        response = getResponse(RequestType.GET, "/workflow");
        node = mapper.readTree(response.getEntityInputStream());
        // we expect to retrieve the one previously created
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        fetchedWorflowInstanceId = elements.next().get("id").getTextValue();
        assertEquals(createdWorflowInstanceId, fetchedWorflowInstanceId);

        // Check GET /task i.e. pending tasks for current user
        response = getResponse(RequestType.GET, "/task");
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        JsonNode element = node.get("entries").getElements().next();
        assertNotNull(element);
        JsonNode taskInfo = element.get("taskInfo");
        assertNotNull(taskInfo);
        JsonNode taskActions = taskInfo.get("taskActions");
        assertEquals(2, taskActions.size());
        JsonNode taskAction = taskActions.getElements().next();
        assertNotNull(taskAction);
        assertEquals(String.format("http://localhost:18090/api/v1/task/%s/cancel", element.get("id").getTextValue()),
                taskAction.get("url").getTextValue());
    }

}
