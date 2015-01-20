/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter.TaskAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter.WorkflowAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.model.WorkflowRequest;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.model.TaskCompletionRequest;
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
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class, init = RestServerInit.class)
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.ecm.platform.restapi.server.routing", "org.nuxeo.ecm.automation.test",
        "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.platform.restapi.io", "org.nuxeo.ecm.platform.restapi.test",
        "org.nuxeo.ecm.platform.restapi.server", "org.nuxeo.ecm.platform.routing.default",
        "org.nuxeo.ecm.platform.filemanager.api", "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.mimetype.api", "org.nuxeo.ecm.platform.mimetype.core", "org.nuxeo.ecm.actions" })
public class WorkflowEndpointTest extends BaseTest {

    @Inject
    ObjectCodecService objectCodecService;

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

    protected ByteArrayOutputStream getBodyForStartReviewTaskCompletion() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TaskCompletionRequest taskCompletionRequest = new TaskCompletionRequest();
        final Map<String, String> variables = new HashMap<String, String>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        variables.put("end_date", DateParser.formatW3CDateTime(calendar.getTime()));
        variables.put("participants", "[\"user:Administrator\"]");
        taskCompletionRequest.setNodeVariables(variables);
        taskCompletionRequest.setWorkflowVariables(variables);
        taskCompletionRequest.setComment("a comment");
        objectCodecService.write(out, taskCompletionRequest);
        return out;
    }

    protected String getCreateAndStartWorkflowBodyContent(String workflowName, List<String> docIds) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WorkflowRequest routingRequest = new WorkflowRequest();
        routingRequest.setWorkflowModelName(workflowName);
        if (docIds != null) {
            routingRequest.setDocumentIds(docIds);
        }
        objectCodecService.write(out, routingRequest);
        return out.toString();
    }

    protected String getCurrentTask(final String createdWorflowInstanceId) throws IOException, JsonProcessingException {
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WorkflowRequest routingRequest = new WorkflowRequest();
        routingRequest.setWorkflowModelName("SerialDocumentReview");
        objectCodecService.write(out, routingRequest);
        ClientResponse response = getResponse(RequestType.POST, "/id/" + note.getId() + "/@" + WorkflowAdapter.NAME,
                out.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("uid").getTextValue();

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
                + "/start_review", getBodyForStartReviewTaskCompletion().toString());
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

        List<String> expectedNames = Arrays.asList(new String[] { "SerialDocumentReview",
                "ParallelDocumentReview" });
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

        // Check path resource
        String graphUrl = node.get("graphResource").getTextValue();
        String graphModelPath = "/workflowModel/ParallelDocumentReview/graph";
        assertTrue(graphUrl.endsWith(graphModelPath));
        response = getResponse(RequestType.GET, graphModelPath);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInvalidNodeAction() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WorkflowRequest routingRequest = new WorkflowRequest();
        routingRequest.setWorkflowModelName("SerialDocumentReview");
        objectCodecService.write(out, routingRequest);
        ClientResponse response = getResponse(RequestType.POST, "/workflow", out.toString());
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
        String taskId = getCurrentTask(createdWorflowInstanceId);
        TaskCompletionRequest taskCompletionRequest;
        ByteArrayOutputStream out = getBodyForStartReviewTaskCompletion();
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/start_review", out.toString());
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Complete second task
        taskId = getCurrentTask(createdWorflowInstanceId);
        out = new ByteArrayOutputStream();
        taskCompletionRequest = new TaskCompletionRequest();
        objectCodecService.write(out, taskCompletionRequest);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/approve", out.toString());
        // Missing required variables
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Complete third task
        taskId = getCurrentTask(createdWorflowInstanceId);
        out = new ByteArrayOutputStream();
        taskCompletionRequest = new TaskCompletionRequest();
        objectCodecService.write(out, taskCompletionRequest);
        response = getResponse(RequestType.PUT, "/task/" + taskId + "/validate", out.toString());
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
}
