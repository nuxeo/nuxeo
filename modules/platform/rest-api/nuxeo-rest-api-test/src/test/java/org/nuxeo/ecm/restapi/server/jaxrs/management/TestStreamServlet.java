/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 2021.22
 */
@RunWith(FeaturesRunner.class)
@Features({ ServletContainerFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.restapi.test")
@Deploy("org.nuxeo.ecm.platform.restapi.server")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-stream-servlet-contrib.xml")
public class TestStreamServlet extends BaseTest {

    @Override
    protected String getRestApiUrl() {
        return getBaseURL();
    }

    @Test
    public void testStreamServlet() {
        String url = "/api/v1/management/stream/cat";

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("timeout", "50ms");

        try (CloseableClientResponse response = getResponse(RequestType.GET, url, queryParams)) {
            // stream param is missing
            assertEquals(SC_BAD_REQUEST, response.getStatus());
            assertContentTypeStartsWith(response, "application/json");
            assertEquals("{\"status\": 400,\"message\":\"Missing stream param\"}", getContent(response));
        }

        queryParams.putSingle("stream", "bulk/unknownStream");
        try (CloseableClientResponse response = getResponse(RequestType.GET, url, queryParams)) {
            // stream not found
            assertEquals(SC_NOT_FOUND, response.getStatus());
            assertContentTypeStartsWith(response, "application/json");
        }

        queryParams.putSingle("stream", "internal/metrics");
        queryParams.putSingle("fromGroup", "stream/introspection");
        try (CloseableClientResponse response = getResponse(RequestType.GET, url, queryParams)) {
            assertEquals(SC_OK, response.getStatus());
            assertContentTypeStartsWith(response, "text/event-stream");
            String content = getContent(response);
            assertTrue(content, content.startsWith("data: "));
        }

    }

    protected String getContent(CloseableClientResponse response) {
        return new BufferedReader(new InputStreamReader(response.getEntityInputStream(),
                StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }

    protected void assertContentTypeStartsWith(CloseableClientResponse response, String expected) {
        assertTrue(response.getHeaders().containsKey("Content-Type"));
        String contentType = response.getHeaders().getFirst("Content-Type");
        assertTrue(contentType, contentType.startsWith(expected));
    }

}
