/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.junit.Test;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 11.3
 */
@Deploy("org.nuxeo.runtime.migration.tests:OSGI-INF/dummy-migration.xml")
public class TestMigrationObject extends ManagementBaseTest {

    @Test
    public void testGet() throws IOException, JSONException {
        try (CloseableClientResponse response = httpClientRule.get("/management/migration/dummy-migration")) {
            assertEquals(SC_OK, response.getStatus());
            String json = response.getEntity(String.class);
            assertJsonResponse(json, "json/testGet.json");
        }
    }

    @Test
    public void testGetList() throws IOException, JSONException {
        try (CloseableClientResponse response = httpClientRule.get("/management/migration")) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            Iterator<JsonNode> elements = node.get("entries").elements();
            Map<String, String> entries = new HashMap<>();
            elements.forEachRemaining(n -> entries.put(n.get("id").textValue(), n.toString()));
            assertJsonResponse(entries.get("dummy-migration"), "json/testGet.json");
            assertJsonResponse(entries.get("dummy-multi-migration"), "json/testGetMulti.json");
        }
    }

    @Test
    public void testProbeMigration() throws IOException, JSONException {
        try (CloseableClientResponse response = httpClientRule.post("/management/migration/dummy-migration/probe",
                null)) {
            assertEquals(SC_OK, response.getStatus());
            String json = response.getEntity(String.class);
            assertJsonResponse(json, "json/testGet.json");
        }
    }

    @Test
    public void testRunMigration() throws IOException, JSONException {
        // Run a unique available migration step
        try (CloseableClientResponse response = httpClientRule.post("/management/migration/dummy-migration/run",
                null)) {
            assertEquals(SC_ACCEPTED, response.getStatus());
        }
        try (CloseableClientResponse response = httpClientRule.get("/management/migration/dummy-migration")) {
            assertEquals(SC_OK, response.getStatus());
            String json = response.getEntity(String.class);
            assertJsonResponse(json, "json/testGetAgain.json");
        }
        // Now another migration step is the only one available
        try (CloseableClientResponse response = httpClientRule.post("/management/migration/dummy-migration/run",
                null)) {
            assertEquals(SC_ACCEPTED, response.getStatus());
        }
        try (CloseableClientResponse response = httpClientRule.get("/management/migration/dummy-migration")) {
            assertEquals(SC_OK, response.getStatus());
            String json = response.getEntity(String.class);
            assertJsonResponse(json, "json/testGetFinalStep.json");
        }
    }

    @Test
    public void testRunMigrationStep() throws IOException, JSONException {
        // Can't run without specifying the desired step as there are multiple available steps
        try (CloseableClientResponse response = httpClientRule.post("/management/migration/dummy-multi-migration/run",
                null)) {
            assertEquals(SC_BAD_REQUEST, response.getStatus());
        }
        // Run a specific migration step
        try (CloseableClientResponse response = httpClientRule.post(
                "/management/migration/dummy-multi-migration/run/before-to-reallyAfter", null)) {
            assertEquals(SC_ACCEPTED, response.getStatus());
        }
        try (CloseableClientResponse response = httpClientRule.get("/management/migration/dummy-multi-migration")) {
            assertEquals(SC_OK, response.getStatus());
            String json = response.getEntity(String.class);
            assertJsonResponse(json, "json/testGetFinalStepMulti.json");
        }
    }

}
