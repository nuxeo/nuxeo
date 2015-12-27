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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

/**
 * @since 5.7.2
 */
public class BaseTest {

    private static final Integer TIMEOUT = Integer.valueOf(1000 * 60 * 5); // 5min

    protected static enum RequestType {
        GET, POST, DELETE, PUT, POSTREQUEST, GETES
    }

    protected ObjectMapper mapper;

    protected Client client;

    protected WebResource service;

    @Before
    public void doBefore() throws Exception {
        service = getServiceFor("Administrator", "Administrator");

        mapper = new ObjectMapper();

    }

    /**
     * @param user
     * @param password
     * @return
     * @since 5.7.3
     */
    protected WebResource getServiceFor(String user, String password) {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(MultiPartWriter.class);
        client = Client.create(config);
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        client.addFilter(new HTTPBasicAuthFilter(user, password));

        return client.resource("http://localhost:18090/api/v1/");
    }

    @Inject
    public CoreSession session;

    protected ClientResponse getResponse(RequestType requestType, String path) {
        return getResponse(requestType, path, null, null, null, null);
    }

    protected ClientResponse getResponse(RequestType requestType, String path, Map<String, String> headers) {
        return getResponse(requestType, path, null, null, null, headers);
    }

    protected ClientResponse getResponse(RequestType requestType, String path, MultiPart mp) {
        return getResponse(requestType, path, null, null, mp, null);
    }

    protected ClientResponse getResponse(RequestType requestType, String path, MultiPart mp, Map<String, String> headers) {
        return getResponse(requestType, path, null, null, mp, headers);
    }

    protected ClientResponse getResponse(RequestType requestType, String path,
            MultivaluedMap<String, String> queryParams) {
        return getResponse(requestType, path, null, queryParams, null, null);
    }

    protected ClientResponse getResponse(RequestType requestType, String path, String data) {
        return getResponse(requestType, path, data, null, null, null);
    }

    protected ClientResponse getResponse(RequestType requestType, String path, String data, Map<String, String> headers) {
        return getResponse(requestType, path, data, null, null, headers);
    }

    protected ClientResponse getResponse(RequestType requestType, String path, String data,
            MultivaluedMap<String, String> queryParams, MultiPart mp, Map<String, String> headers) {
        WebResource wr = service.path(path);

        if (queryParams != null && !queryParams.isEmpty()) {
            wr = wr.queryParams(queryParams);
        }
        Builder builder;
        if (requestType == RequestType.GETES) {
            builder = wr.accept("application/json+esentity");
        } else {
            builder = wr.accept(MediaType.APPLICATION_JSON).header("X-NXDocumentProperties", "dublincore");
        }
        if (mp != null) {
            builder = wr.type(MediaType.MULTIPART_FORM_DATA_TYPE);
        }

        if (headers == null || !(headers.containsKey("Content-Type"))) {
            if (requestType == RequestType.POSTREQUEST) {
                builder.header("Content-Type", "application/json+nxrequest");
            } else {
                builder.header("Content-Type", "application/json+nxentity");
            }
        }

        // Adding some headers if needed
        if (headers != null && !headers.isEmpty()) {
            for (String headerKey : headers.keySet()) {
                builder.header(headerKey, headers.get(headerKey));
            }
        }
        switch (requestType) {
        case GET:
        case GETES:
            return builder.get(ClientResponse.class);
        case POST:
        case POSTREQUEST:
            if (mp != null) {
                return builder.post(ClientResponse.class, mp);
            } else {
                return builder.post(ClientResponse.class, data);
            }
        case PUT:
            if (mp != null) {
                return builder.put(ClientResponse.class, mp);
            } else {
                return builder.put(ClientResponse.class, data);
            }
        case DELETE:
            return builder.delete(ClientResponse.class, data);
        default:
            throw new RuntimeException();
        }
    }

    protected JsonNode getResponseAsJson(RequestType responseType, String url) throws IOException,
            JsonProcessingException {
        return getResponseAsJson(responseType, url, null);
    }

    /**
     * @param get
     * @param string
     * @param queryParamsForPage
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     * @since 5.8
     */
    protected JsonNode getResponseAsJson(RequestType responseType, String url,
            MultivaluedMap<String, String> queryParams) throws JsonProcessingException, IOException {
        ClientResponse response = getResponse(responseType, url, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return mapper.readTree(response.getEntityInputStream());
    }

    /**
     * Fetch session invalidations.
     *
     * @since 5.9.3
     */
    protected void fetchInvalidations() {
        session.save();
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected void assertNodeEqualsDoc(JsonNode node, DocumentModel note) throws Exception {
        assertEquals("document", node.get("entity-type").getValueAsText());
        assertEquals(note.getPathAsString(), node.get("path").getValueAsText());
        assertEquals(note.getId(), node.get("uid").getValueAsText());
        assertEquals(note.getTitle(), node.get("title").getValueAsText());
    }

    protected List<JsonNode> getLogEntries(JsonNode node) {
        assertEquals("documents", node.get("entity-type").getValueAsText());
        assertTrue(node.get("entries").isArray());
        List<JsonNode> result = new ArrayList<>();
        Iterator<JsonNode> elements = node.get("entries").getElements();
        while (elements.hasNext()) {
            result.add(elements.next());
        }
        return result;
    }

    /**
     * @since 7.1
     */
    protected String getErrorMessage(JsonNode node) {
        assertEquals("exception", node.get("entity-type").getValueAsText());
        assertTrue(node.get("message").isTextual());
        return node.get("message").getValueAsText();
    }

    protected void assertEntityEqualsDoc(InputStream in, DocumentModel doc) throws Exception {

        JsonNode node = mapper.readTree(in);
        assertNodeEqualsDoc(node, doc);

    }
}
