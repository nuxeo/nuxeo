/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *      bdelbosc
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2021.34
 */
public class TestPageProvidersObject extends ManagementBaseTest {

    @Test
    public void testPageProvidersEndpoint() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/page-providers")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            JsonNode ppList = mapper.readTree(response.getEntityInputStream());
            assertTrue(ppList.isArray());
            assertFalse(ppList.isEmpty());
            JsonNode pp = ppList.get(0);
            assertFalse(pp.get("name").asText().isEmpty());
            assertFalse(pp.get("class").asText().isEmpty());
        }
    }
}
