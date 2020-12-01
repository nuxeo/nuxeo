/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.Environment.DISTRIBUTION_HOTFIX;
import static org.nuxeo.common.Environment.DISTRIBUTION_NAME;
import static org.nuxeo.common.Environment.DISTRIBUTION_SERVER;
import static org.nuxeo.common.Environment.DISTRIBUTION_VERSION;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.restapi.jaxrs.io.capabilities.ServerInfoJsonWriter;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.HttpClientTestRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-cluster.xml")
public class TestServerInfoObject {

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected HttpClientTestRule httpClientRule;

    protected ObjectMapper mapper = new ObjectMapper();

    protected HttpClientTestRule getRule() {
        String url = String.format("http://localhost:%d/api/v%d", servletContainerFeature.getPort(),
                APIVersion.latest().toInt());
        return new HttpClientTestRule.Builder().url(url).adminCredentials().accept(WILDCARD).build();
    }

    @Before
    public void before() {
        httpClientRule = getRule();
        httpClientRule.starting();
    }

    @After
    public void after() {
        httpClientRule.finished();
    }

    @Test
    @WithFrameworkProperty(name = DISTRIBUTION_NAME, value = DISTRIBUTION_NAME)
    @WithFrameworkProperty(name = DISTRIBUTION_VERSION, value = DISTRIBUTION_VERSION)
    @WithFrameworkProperty(name = DISTRIBUTION_SERVER, value = DISTRIBUTION_SERVER)
    public void testGet() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/server")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertEquals(ServerInfoJsonWriter.ENTITY_TYPE, node.get(ENTITY_FIELD_NAME).asText());
            assertEquals(DISTRIBUTION_NAME, node.get("distributionName").asText());
            assertEquals(DISTRIBUTION_VERSION, node.get("distributionVersion").asText());
            assertEquals(DISTRIBUTION_SERVER, node.get("distributionServer").asText());
            assertNull(node.get("hotfixVersion"));
            assertTrue(node.get("clusterEnabled").asBoolean());
            assertEquals("123", node.get("clusterNodeId").asText());
        }
    }

    @Test
    @WithFrameworkProperty(name = DISTRIBUTION_NAME, value = DISTRIBUTION_NAME)
    @WithFrameworkProperty(name = DISTRIBUTION_VERSION, value = DISTRIBUTION_VERSION)
    @WithFrameworkProperty(name = DISTRIBUTION_SERVER, value = DISTRIBUTION_SERVER)
    @WithFrameworkProperty(name = DISTRIBUTION_HOTFIX, value = DISTRIBUTION_HOTFIX)
    public void testGetWithHotfixVersion() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/server")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertEquals(ServerInfoJsonWriter.ENTITY_TYPE, node.get(ENTITY_FIELD_NAME).asText());
            assertEquals(DISTRIBUTION_NAME, node.get("distributionName").asText());
            assertEquals(DISTRIBUTION_VERSION, node.get("distributionVersion").asText());
            assertEquals(DISTRIBUTION_SERVER, node.get("distributionServer").asText());
            assertEquals(DISTRIBUTION_HOTFIX, node.get("hotfixVersion").asText());
            assertTrue(node.get("clusterEnabled").asBoolean());
            assertEquals("123", node.get("clusterNodeId").asText());
        }
    }
}
