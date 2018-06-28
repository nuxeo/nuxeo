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

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 8.3
 */
public class RoutingRestBaseTest extends BaseTest {

    protected String assertActorIsAdministrator(ClientResponse response) throws IOException {
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get("entries").size());
        Iterator<JsonNode> elements = node.get("entries").elements();
        JsonNode element = elements.next();
        String taskId = element.get("id").textValue();
        JsonNode actors = element.get("actors");
        assertEquals(1, actors.size());
        String actor = actors.elements().next().get("id").textValue();
        assertEquals("Administrator", actor);
        return taskId;
    }

    protected String getBodyForStartReviewTaskCompletion(String taskId) {
        return getBodyForStartReviewTaskCompletion(taskId, "user:Administrator");
    }

    /**
     * @since 9.1
     */
    protected String getBodyForStartReviewTaskCompletion(String taskId, String assignee) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        return getBodyForStartReviewTaskCompletion(taskId, calendar.getTime(), assignee);
    }

    protected String getBodyForStartReviewTaskCompletion(String taskId, Date dueDate) {
        return getBodyForStartReviewTaskCompletion(taskId, dueDate, "user:Administrator");
    }

    /**
     * @since 9.1
     */
    protected String getBodyForStartReviewTaskCompletion(String taskId, Date dueDate, String assignee) {
        return "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\"," + "\"entity-type\": \"task\","
                + "\"variables\": {" + "\"end_date\": \"" + DateParser.formatW3CDateTime(dueDate) + "\","
                + "\"participants\": [\"" + assignee + "\"]," + "\"assignees\": [\"" + assignee + "\"]" + "}" + "}";
    }

    protected String getBodyWithSecurityViolationForStartReviewTaskCompletion(String taskId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        return "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\"," + "\"entity-type\": \"task\","
                + "\"variables\": {" + "\"end_date\": \"" + DateParser.formatW3CDateTime(calendar.getTime()) + "\","
                + "\"participants\": [\"user:Administrator\"]," + "\"review_result\": \"blabablaa\"" + "}" + "}";
    }

    protected String getBodyForTaskCompletion(String taskId) {
        return "{\"entity-type\": \"task\", \"id\": \"" + taskId + "\"}";
    }

    protected String getCreateAndStartWorkflowBodyContent(String workflowName) {
        return getCreateAndStartWorkflowBodyContent(workflowName, null);
    }

    protected String getCreateAndStartWorkflowBodyContent(String workflowName, List<String> docIds) {
        StringBuilder result = new StringBuilder();
        result.append("{\"entity-type\": \"workflow\", " + "\"workflowModelName\": \"").append(workflowName).append(
                "\"");
        if (docIds != null && !docIds.isEmpty()) {
            result.append(", \"attachedDocumentIds\": [");
            for (String docId : docIds) {
                result.append("\"").append(docId).append("\"");
            }
            result.append("]");
        }

        result.append("}");
        return result.toString();
    }

    protected String getCurrentTaskId(String createdWorkflowInstanceId) throws IOException {
        return getCurrentTask(createdWorkflowInstanceId).get("id").textValue();
    }

    /**
     * @since 10.2
     */
    protected JsonNode getCurrentTask(String createdWorkflowInstanceId) throws IOException {
        return getCurrentTask(createdWorkflowInstanceId, null);
    }

    /**
     * @since 10.2
     */
    protected JsonNode getCurrentTask(String createdWorkflowInstanceId, MultivaluedMap<String, String> queryParams)
            throws IOException {
        return getCurrentTask(createdWorkflowInstanceId, queryParams, null);
    }

    /**
     * @since 8.3
     */
    protected JsonNode getCurrentTask(String createdWorkflowInstanceId, MultivaluedMap<String, String> queryParams,
            Map<String, String> headers) throws IOException {
        JsonNode node;
        if (queryParams == null) {
            queryParams = new MultivaluedMapImpl();
        }
        queryParams.put("workflowInstanceId", singletonList(createdWorkflowInstanceId));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null,
                headers)) {
            node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").elements();
            return elements.next();
        }
    }

}
