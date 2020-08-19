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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 11.3
 */
@Deploy("org.nuxeo.ecm.core.management")
public class TestProbesObject extends ManagementBaseTest {

    @Test
    public void testAllProbes() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/probes")) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonAssert jAssert = JsonAssert.on(node.toString());
            jAssert.get("entity-type").isEquals("probes");
            JsonAssert jProbeArray = jAssert.get("entries");

            JsonAssert jProbe = jProbeArray.get(0);
            testProbeInfo(jProbe);
        }
    }

    @Test
    public void testProbe() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/probes/administrativeStatus")) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonAssert jAssert = JsonAssert.on(node.toString());
            testProbeInfo(jAssert);
        }
    }

    @Test
    public void testLaunchProbe() throws IOException {
        try (CloseableClientResponse response = httpClientRule.get("/management/probes/administrativeStatus")) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, node.get("counts").get("run").asInt());
        }
        try (CloseableClientResponse response = httpClientRule.post("/management/probes/administrativeStatus", null)) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonAssert jAssert = JsonAssert.on(node.toString());
            testProbeInfo(jAssert);
            assertEquals(1, node.get("counts").get("run").asInt());
        }
    }

    @Test
    public void testGetWrongProbe() {
        try (CloseableClientResponse response = httpClientRule.get("/management/probes/fake")) {
            assertEquals(SC_NOT_FOUND, response.getStatus());
        }
    }

    @Test
    public void testLaunchWrongProbe() {
        try (CloseableClientResponse response = httpClientRule.post("/management/probes/fake", null)) {
            assertEquals(SC_NOT_FOUND, response.getStatus());
        }
    }

    protected void testProbeInfo(JsonAssert jProbe) throws IOException {
        jProbe.get("name").notNull();
        jProbe.get("status").get("success").notNull();
        jProbe.get("status").get("infos").notNull();

        JsonAssert jHistory = jProbe.get("history");
        jHistory.has("lastRun");
        jHistory.has("lastSuccess");
        jHistory.has("lastFail");

        JsonAssert jCounts = jProbe.get("counts");
        jCounts.has("run");
        jCounts.has("success");
        jCounts.has("failure");
        jProbe.get("time").notNull();
    }
}
