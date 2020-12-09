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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.Environment.DISTRIBUTION_HOTFIX;
import static org.nuxeo.common.Environment.DISTRIBUTION_NAME;
import static org.nuxeo.common.Environment.DISTRIBUTION_SERVER;
import static org.nuxeo.common.Environment.DISTRIBUTION_VERSION;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;
import static org.nuxeo.runtime.capabilities.CapabilitiesServiceImpl.CAPABILITY_SERVER;
import static org.nuxeo.runtime.cluster.ClusterServiceImpl.CAPABILITY_CLUSTER;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.restapi.jaxrs.io.capabilities.CapabilitiesJsonWriter;
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
public class TestCapabilitiesObject {

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected HttpClientTestRule httpClientRule;

    protected ObjectMapper mapper = new ObjectMapper();

    protected HttpClientTestRule getRule() {
        String url = String.format("http://localhost:%d/api/v1", servletContainerFeature.getPort());
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
        try (CloseableClientResponse response = httpClientRule.get("/capabilities")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertEquals(CapabilitiesJsonWriter.ENTITY_TYPE, node.get(ENTITY_FIELD_NAME).asText());

            JsonNode serverNode = node.get(CAPABILITY_SERVER);
            assertNotNull(serverNode);
            assertEquals(DISTRIBUTION_NAME, serverNode.get("distributionName").asText());
            assertEquals(DISTRIBUTION_VERSION, serverNode.get("distributionVersion").asText());
            assertEquals(DISTRIBUTION_SERVER, serverNode.get("distributionServer").asText());
            assertNull(serverNode.get("hotfixVersion"));

            JsonNode clusterNode = node.get(CAPABILITY_CLUSTER);
            assertNotNull(clusterNode);
            assertTrue(clusterNode.get("enabled").asBoolean());
            assertEquals("123", clusterNode.get("nodeId").asText());
        }
    }

    @Test
    @WithFrameworkProperty(name = DISTRIBUTION_NAME, value = DISTRIBUTION_NAME)
    @WithFrameworkProperty(name = DISTRIBUTION_VERSION, value = DISTRIBUTION_VERSION)
    @WithFrameworkProperty(name = DISTRIBUTION_SERVER, value = DISTRIBUTION_SERVER)
    @WithFrameworkProperty(name = DISTRIBUTION_HOTFIX, value = DISTRIBUTION_HOTFIX)
    public void testGetWithHotfixVersion() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/capabilities")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertEquals(CapabilitiesJsonWriter.ENTITY_TYPE, node.get(ENTITY_FIELD_NAME).asText());

            JsonNode serverNode = node.get(CAPABILITY_SERVER);
            assertNotNull(serverNode);
            assertEquals(DISTRIBUTION_NAME, serverNode.get("distributionName").asText());
            assertEquals(DISTRIBUTION_VERSION, serverNode.get("distributionVersion").asText());
            assertEquals(DISTRIBUTION_SERVER, serverNode.get("distributionServer").asText());
            assertEquals(DISTRIBUTION_HOTFIX, serverNode.get("hotfixVersion").asText());

            JsonNode clusterNode = node.get(CAPABILITY_CLUSTER);
            assertNotNull(clusterNode);
            assertTrue(clusterNode.get("enabled").asBoolean());
            assertEquals("123", clusterNode.get("nodeId").asText());
        }
    }
}
