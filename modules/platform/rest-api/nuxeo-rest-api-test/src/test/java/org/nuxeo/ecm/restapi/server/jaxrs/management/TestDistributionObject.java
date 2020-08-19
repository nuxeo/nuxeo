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
 *     Nour Al Kotob
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.restapi.jaxrs.io.management.SimplifiedServerInfoJsonWriter;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 11.3
 */
public class TestDistributionObject extends ManagementBaseTest {

    private static final String PRODUCT_NAME = "cool product name";

    private static final String PRODUCT_VERSION = "cool product version";

    private static final String DISTRIBUTION_NAME = "cool distribution name";

    private static final String DISTRIBUTION_VERSION = "cool distribution version";

    private static final String DISTRIBUTION_SERVER = "cool distribution server";

    private static final String DISTRIBUTION_DATE = "cool distribution date";

    @Test
    @WithFrameworkProperty(name = Environment.PRODUCT_NAME, value = PRODUCT_NAME)
    @WithFrameworkProperty(name = Environment.PRODUCT_VERSION, value = PRODUCT_VERSION)
    @WithFrameworkProperty(name = Environment.DISTRIBUTION_NAME, value = DISTRIBUTION_NAME)
    @WithFrameworkProperty(name = Environment.DISTRIBUTION_VERSION, value = DISTRIBUTION_VERSION)
    @WithFrameworkProperty(name = Environment.DISTRIBUTION_SERVER, value = DISTRIBUTION_SERVER)
    @WithFrameworkProperty(name = Environment.DISTRIBUTION_DATE, value = DISTRIBUTION_DATE)
    public void testDistribution() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/distribution")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertEquals(SimplifiedServerInfoJsonWriter.ENTITY_TYPE, node.get(ENTITY_FIELD_NAME).asText());
            assertEquals(PRODUCT_NAME, node.get("applicationName").asText());
            assertEquals(PRODUCT_VERSION, node.get("applicationVersion").asText());
            assertEquals(DISTRIBUTION_NAME, node.get("distributionName").asText());
            assertEquals(DISTRIBUTION_VERSION, node.get("distributionVersion").asText());
            assertEquals(DISTRIBUTION_DATE, node.get("distributionDate").asText());

            JsonNode value = node.get("warnings");
            assertNotNull(value);
            assertTrue(value.isArray());

            value = node.get("errors");
            assertNotNull(value);
            assertTrue(value.isArray());

            value = node.get("bundles");
            assertNotNull(value);
            assertTrue(value.isArray());
        }
    }
}
