/*
 * (C) Copyright 2016-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.restapi.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @since 8.3
 */
public class RoutingRestBaseTest extends BaseTest {

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

    protected String getBodyForChooseParticipantsTaskCompletion(String taskId) {
        return "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\"," + "\"entity-type\": \"task\","
                + "\"variables\": {\"participants\": [\"Administrator\"], \"validationOrReview\": \"validation\"} }";
    }

    protected String getBodyForStartReviewTaskCompletion(String taskId) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        return getBodyForStartReviewTaskCompletion(taskId, calendar.getTime());
    }

    /**
     * @since 9.1
     */
    protected String getBodyForStartReviewTaskCompletion(String taskId, String assignee) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        return getBodyForStartReviewTaskCompletion(taskId, calendar.getTime(), assignee);
    }

    protected String getBodyForStartReviewTaskCompletion(String taskId, Date dueDate) throws IOException {
        return getBodyForStartReviewTaskCompletion(taskId, dueDate, "user:Administrator");
    }

    /**
     * @since 9.1
     */
    protected String getBodyForStartReviewTaskCompletion(String taskId, Date dueDate, String assignee)
            throws IOException {
        String jsonBody = "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\","
                + "\"entity-type\": \"task\"," + "\"variables\": {" + "\"end_date\": \""
                + DateParser.formatW3CDateTime(dueDate) + "\"," + "\"participants\": [\"" + assignee + "\"],"
                + "\"assignees\": [\"" + assignee + "\"]" + "}" + "}";
        return jsonBody;
    }

    protected String getBodyWithSecurityViolationForStartReviewTaskCompletion(String taskId) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        String jsonBody = "{" + "\"id\": \"" + taskId + "\"," + "\"comment\": \"a comment\","
                + "\"entity-type\": \"task\"," + "\"variables\": {" + "\"end_date\": \""
                + DateParser.formatW3CDateTime(calendar.getTime()) + "\","
                + "\"participants\": [\"user:Administrator\"]," + "\"review_result\": \"blabablaa\"" + "}" + "}";
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

    protected String getCurrentTaskId(final String createdWorflowInstanceId)
            throws IOException, JsonProcessingException {
        String taskId = getCurrentTask(createdWorflowInstanceId, null, null).get("id").getTextValue();
        return taskId;
    }

    /**
     * @since 8.3
     */
    protected JsonNode getCurrentTask(final String createdWorflowInstanceId, MultivaluedMap<String, String> queryParams,
            Map<String, String> headers) throws IOException, JsonProcessingException {
        JsonNode node;
        if (queryParams == null) {
            queryParams = new MultivaluedMapImpl();
        }
        queryParams.put("workflowInstanceId", Arrays.asList(new String[] { createdWorflowInstanceId }));
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/task", null, queryParams, null,
                headers)) {
            node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, node.get("entries").size());
            Iterator<JsonNode> elements = node.get("entries").getElements();
            return elements.next();
        }
    }

}
