/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.client.rest.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.ecm.automation.client.AutomationException;

import com.sun.jersey.api.client.ClientResponse;

/**
 * A Rest response from Nuxeo REST API.
 * <p>
 * Wraps a {@link ClientResponse} response and provides utility methods to get
 * back the result as a {@link Map} or as a {@link JsonNode}.
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
            return objectMapper.readValue(responseAsJson, typeRef);
        } catch (IOException e) {
            throw new AutomationException(e);
        }
    }

}
