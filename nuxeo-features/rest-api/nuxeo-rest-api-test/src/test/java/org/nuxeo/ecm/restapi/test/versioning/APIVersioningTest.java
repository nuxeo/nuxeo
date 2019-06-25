/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.restapi.test.versioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.JerseyClientHelper;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-marshallers-contrib.xml")
public class APIVersioningTest {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected Client client;

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port;
    }

    protected WebResource getRESTAPIResource(APIVersion apiVersion) {
        return getRESTAPIResource(apiVersion.toInt());
    }

    protected WebResource getRESTAPIResource(Integer apiVersion) {
        String apiPath = String.format("%s/api/v%s/", getBaseURL(), apiVersion);
        return client.resource(apiPath);
    }

    @Before
    public void doBefore() {
        client = JerseyClientHelper.clientBuilder().setCredentials("Administrator", "Administrator").build();
    }

    @After
    public void doAfter() {
        if (client != null) {
            client.destroy();
        }
    }

    @Test
    public void testInvalidAPIVersion() throws IOException {
        ClientResponse response = client.resource(String.format("%s/api/v/", getBaseURL()))
                                        .path("foo")
                                        .path("path1")
                                        .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(404, r.getStatus());
        }

        response = getRESTAPIResource((Integer) null).path("foo").path("path1").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(404, r.getStatus());
        }

        response = getRESTAPIResource(-1).path("foo").path("path1").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(400, r.getStatus());
            assertInvalidAPIVersionException(r.getEntityInputStream(), -1);
        }

        response = getRESTAPIResource(0).path("foo").path("path1").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(400, r.getStatus());
            assertInvalidAPIVersionException(r.getEntityInputStream(), 0);
        }

        response = getRESTAPIResource(1000).path("foo").path("path1").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(400, r.getStatus());
            assertInvalidAPIVersionException(r.getEntityInputStream(), 1000);
        }
    }

    protected void assertInvalidAPIVersionException(InputStream is, Object apiVersion) throws IOException {
        JsonNode jsonNode = MAPPER.readTree(is);
        assertEquals("exception", jsonNode.get("entity-type").asText());
        assertEquals(400, jsonNode.get("status").intValue());
        String expectedMessage = String.format("%s is not part of the valid versions: %s", apiVersion,
                APIVersion.VALID_VERSIONS.keySet());
        assertEquals(expectedMessage, jsonNode.get("message").asText());
    }

    @Test
    public void testUpdatedEndpoint() {
        ClientResponse response = getRESTAPIResource(APIVersion.V1.toInt()).path("foo")
                                                                           .path("path1")
                                                                           .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            String body = r.getEntity(String.class);
            assertEquals("foo", body);
        }

        response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo").path("path1").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            String body = r.getEntity(String.class);
            assertEquals("bar", body);
        }
    }

    @Test
    public void testNewEndpoint() {
        ClientResponse response = getRESTAPIResource(APIVersion.V1.toInt()).path("foo")
                                                                           .path("path2")
                                                                           .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(404, r.getStatus());
        }

        response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo").path("path2").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            String body = r.getEntity(String.class);
            assertEquals("bar", body);
        }
    }

    @Test
    public void testUpdatedWriter() throws IOException {
        ClientResponse response = getRESTAPIResource(APIVersion.V1.toInt()).path("foo")
                                                                           .path("dummy")
                                                                           .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            assertEquals("dummy", jsonNode.get("entity-type").asText());
            assertEquals("foo", jsonNode.get("message").asText());
        }

        response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo").path("dummy").get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            assertEquals("dummy", jsonNode.get("entity-type").asText());
            assertEquals("bar", jsonNode.get("message").asText());
        }
    }

    @Test
    @ConsoleLogLevelThreshold("FATAL")
    public void testNewWriter() throws IOException {
        try (CloseableClientResponse r = CloseableClientResponse.of(
                getRESTAPIResource(APIVersion.V1.toInt()).path("foo").path("dummy2").get(ClientResponse.class))) {
            assertEquals(500, r.getStatus());
        }

        ClientResponse response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo")
                                                                            .path("dummy2")
                                                                            .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            assertEquals("dummy2", jsonNode.get("entity-type").asText());
        }
    }

    @Test
    public void testUpdatedReader() {
        String json = "{\"entity-type\": \"dummy\", \"fieldV1\": \"foo\", \"fieldV2\": \"bar\"}";
        ClientResponse response = getRESTAPIResource(1).path("foo").path("dummy").post(ClientResponse.class, json);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            assertEquals("foo - null", r.getEntity(String.class));
        }

        response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo")
                                                             .path("dummy")
                                                             .post(ClientResponse.class, json);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            assertEquals("foo - bar", r.getEntity(String.class));
        }
    }

    @Test
    public void testUpdatedEnricher() throws IOException {
        ClientResponse response = getRESTAPIResource(APIVersion.V1.toInt()).path("foo")
                                                                           .path("dummy")
                                                                           .header("enrichers.dummy", "dummy")
                                                                           .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            JsonNode dummyNode = jsonNode.get("contextParameters").get("dummy");
            assertEquals("foo", dummyNode.get("message").asText());
        }

        response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo")
                                                             .path("dummy")
                                                             .header("enrichers.dummy", "dummy")
                                                             .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            JsonNode dummyNode = jsonNode.get("contextParameters").get("dummy");
            assertEquals("bar", dummyNode.get("message").asText());
        }
    }

    @Test
    public void testNewEnricher() throws IOException {
        ClientResponse response = getRESTAPIResource(APIVersion.V1.toInt()).path("foo")
                                                                           .path("dummy")
                                                                           .header("enrichers.dummy", "dummy2")
                                                                           .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            assertNull(jsonNode.get("contextParameters"));
        }

        response = getRESTAPIResource(APIVersion.V11.toInt()).path("foo")
                                                             .path("dummy")
                                                             .header("enrichers.dummy", "dummy2")
                                                             .get(ClientResponse.class);
        try (CloseableClientResponse r = CloseableClientResponse.of(response)) {
            assertEquals(200, r.getStatus());
            JsonNode jsonNode = MAPPER.readTree(r.getEntityInputStream());
            JsonNode dummyNode = jsonNode.get("contextParameters").get("dummy2");
            assertEquals("v2", dummyNode.get("message").asText());
        }
    }

}
