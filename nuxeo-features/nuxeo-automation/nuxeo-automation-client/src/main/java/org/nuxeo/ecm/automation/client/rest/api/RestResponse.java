/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.client.rest.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.AutomationException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;

/**
 * A Rest response from Nuxeo REST API.
 * <p>
 * Wraps a {@link ClientResponse} response and provides utility methods to get back the result as a {@link Map} or as a
 * {@link JsonNode}.
 *
 * @since 5.8
 */
public class RestResponse {

    protected final ClientResponse clientResponse;

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected JsonNode responseAsJson;

    public RestResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    public int getStatus() {
        return clientResponse.getStatus();
    }

    public JsonNode asJson() {
        computeResponseAsJson();
        return responseAsJson;
    }

    protected void computeResponseAsJson() {
        if (responseAsJson == null) {
            try {
                responseAsJson = objectMapper.readTree(clientResponse.getEntityInputStream());
            } catch (IOException e) {
                throw new AutomationException(e);
            }
        }
    }

    public Map<String, Object> asMap() {
        computeResponseAsJson();
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        try {
            return objectMapper.readValue(objectMapper.treeAsTokens(responseAsJson), typeRef);
        } catch (IOException e) {
            throw new AutomationException(e);
        }
    }

}
