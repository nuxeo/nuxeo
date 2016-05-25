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
 *     "Guillaume Renard"
 */

package org.nuxeo.ecm.restapi.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.platform.routing.core.io.DocumentRouteWriter;
import org.nuxeo.ecm.platform.routing.core.io.TaskWriter;
import org.nuxeo.ecm.platform.routing.core.io.enrichers.PendingTasksJsonEnricher;
import org.nuxeo.ecm.platform.routing.core.io.enrichers.RunningWorkflowJsonEnricher;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.restapi.test.RestServerInit;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 8.3
 */
public class RoutingFetchAndEnricherTest extends RoutingRestBaseTest {

    /**
     * @since 8.3
     */
    @Test
    public void testFethWfInitiator() throws IOException {

        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();


        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + DocumentRouteWriter.ENTITY_TYPE, DocumentRouteWriter.FETCH_INITATIOR);
        response = getResponse(RequestType.GET, "/workflow/" + createdWorflowInstanceId, queryParams);
        node = mapper.readTree(response.getEntityInputStream());
        JsonNode initiatorNode = node.get("initiator");
        assertEquals("Administrator", initiatorNode.get("id").getTextValue());
        JsonNode initiatorProps = initiatorNode.get("properties");
        assertEquals(1, ((ArrayNode) initiatorProps.get("groups")).size());
        assertEquals("administrators", ((ArrayNode) initiatorProps.get("groups")).get(0).getTextValue());
        // For the sake of security
        assertTrue(StringUtils.isBlank(initiatorProps.get("password").getTextValue()));
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFethTaskActors() throws IOException {

        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + TaskWriter.ENTITY_TYPE, TaskWriter.FETCH_ACTORS);

        JsonNode task = getCurrentTask(createdWorflowInstanceId, queryParams, null);

        ArrayNode taskActors = (ArrayNode) task.get("actors");
        assertEquals(1, taskActors.size());
        assertEquals("Administrator", taskActors.get(0).get("id").getTextValue());
        // For the sake of security
        assertTrue(StringUtils.isBlank(taskActors.get(0).get("properties").get("password").getTextValue()));
    }

    /**
     * @since 8.3
     */
    @Test
    public void testTasksEnricher() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", PendingTasksJsonEnricher.NAME);
        response = getResponse(RequestType.GET,
                "/id/" + note.getId(), headers);

        node = mapper.readTree(response.getEntityInputStream());
        ArrayNode tasksNode = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(PendingTasksJsonEnricher.NAME);
        assertEquals(1, tasksNode.size());
        ArrayNode targetDocumentIdsNode = (ArrayNode) tasksNode.get(0).get(TaskWriter.TARGET_DOCUMENTS);
        assertEquals(1, targetDocumentIdsNode.size());
        assertEquals(note.getId(), targetDocumentIdsNode.get(0).get("id").getTextValue());
    }

    /**
     * @since 8.3
     */
    @Test
    public void testRunningWorkflowEnricher() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", RunningWorkflowJsonEnricher.NAME);
        response = getResponse(RequestType.GET,
                "/id/" + note.getId(), headers);

        node = mapper.readTree(response.getEntityInputStream());
        ArrayNode workflowsNode = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(RunningWorkflowJsonEnricher.NAME);
        assertEquals(1, workflowsNode.size());
        ArrayNode attachedDocumentIdsNode = (ArrayNode) workflowsNode.get(0).get(DocumentRouteWriter.ATTACHED_DOCUMENTS);
        assertEquals(1, attachedDocumentIdsNode.size());
        assertEquals(note.getId(), attachedDocumentIdsNode.get(0).get("id").getTextValue());
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFetchTaskTargetDocuments() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = getResponse(RequestType.POST, "/workflow",
                getCreateAndStartWorkflowBodyContent("SerialDocumentReview", Arrays.asList(new String[] { note.getId() })));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        final String createdWorflowInstanceId = node.get("id").getTextValue();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + TaskWriter.ENTITY_TYPE, TaskWriter.FETCH_TARGET_DOCUMENT);

        JsonNode task = getCurrentTask(createdWorflowInstanceId, queryParams, null);

        ArrayNode taskTargetDocuments = (ArrayNode) task.get(TaskWriter.TARGET_DOCUMENTS);
        assertEquals(1, taskTargetDocuments.size());
        assertEquals(note.getId(), taskTargetDocuments.get(0).get("uid").getTextValue());
    }

    /**
     * @since 8.3
     */
    @Test
    public void testFetchWorfklowAttachedDocuments() throws IOException {
        DocumentModel note = RestServerInit.getNote(0, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + DocumentRouteWriter.ENTITY_TYPE, DocumentRouteWriter.FETCH_ATTACHED_DOCUMENTS);

        ClientResponse response = getResponse(RequestType.POST, "/workflow", getCreateAndStartWorkflowBodyContent(
                "SerialDocumentReview", Arrays.asList(new String[] { note.getId() })), queryParams, null, null);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        ArrayNode wfAttachedDocuments = (ArrayNode) node.get(DocumentRouteWriter.ATTACHED_DOCUMENTS);
        assertEquals(1, wfAttachedDocuments.size());
        assertEquals(note.getId(), wfAttachedDocuments.get(0).get("uid").getTextValue());
    }

}
