/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.io.marshallers.json.types.SchemaJsonWriter;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Test class for 'docType', 'facet' and 'schema' endpoint.
 *
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, DirectoryFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-directory-contrib.xml")
public class ConfigTest extends BaseTest {

    /**
     * @since 8.10
     */
    @Test
    public void itCanRetrieveSchemaDefinitionWithoutFieldConstraints() throws IOException {
        // When I call the Rest schema endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/schema/dublincore");

        // Then I can retrieve schema fields type
        assertEquals("schema", node.get("entity-type").textValue());
        JsonNode fields = node.get("fields");
        assertTrue(fields.size() > 0);
        JsonNode creatorField = fields.get("creator");
        assertEquals("string", creatorField.textValue());
    }

    /**
     * @since 8.10
     */
    @Test
    public void itCanRetrieveSchemaDefinitionWithFieldConstraints() throws IOException {
        // When I call the Rest schema endpoint with fetch.schema=fields
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + SchemaJsonWriter.ENTITY_TYPE, SchemaJsonWriter.FETCH_FIELDS);
        JsonNode node = getResponseAsJson(RequestType.GET, "/schema/dublincore", queryParams);

        // Then I can retrieve schema fields type and constraints
        assertEquals("schema", node.get("entity-type").textValue());
        JsonNode fields = node.get("fields");
        assertTrue(fields.size() > 0);

        // Test that creator has a constraint with validation disabled
        JsonNode creatorField = fields.get("creator");
        assertEquals("string", creatorField.get("type").textValue());
        ArrayNode constraints = (ArrayNode) creatorField.get("constraints");
        assertEquals(2, constraints.size());
        JsonNode userConstraint = getConstraint(constraints, "userManagerResolver");
        assertNotNull(userConstraint);
        JsonNode userConstraintParams = userConstraint.get("parameters");
        assertEquals(3, userConstraintParams.size());
        assertEquals("true", userConstraintParams.get("includeUsers").textValue());
        assertEquals("false", userConstraintParams.get("includeGroups").textValue());
        assertEquals("false", userConstraintParams.get("validation").textValue());

        // Test that nature has a constraint checking if value exists in directory
        JsonNode natureField = fields.get("nature");
        assertEquals("string", natureField.get("type").textValue());
        constraints = (ArrayNode) natureField.get("constraints");
        assertEquals(2, constraints.size());
        JsonNode natureConstraint = getConstraint(constraints, "directoryResolver");
        assertNotNull(natureConstraint);
        JsonNode natureConstraintParams = natureConstraint.get("parameters");
        assertEquals(2, natureConstraintParams.size());
        assertEquals("nature", natureConstraintParams.get("directory").textValue());
    }

    protected JsonNode getConstraint(ArrayNode constraints, String name) {
        JsonNode natureConstraint = null;
        for (JsonNode constraint : constraints) {
            if (name.equals(constraint.get("name").textValue())) {
                natureConstraint = constraint;
                break;
            }
        }
        return natureConstraint;
    }

    /**
     * @since 8.10
     */
    @Test
    public void itCanRetrieveAllSchemas() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/schema")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertTrue(node.isArray());
        }
    }

    /**
     * @since 8.10
     */
    @Test
    public void itCanRetrieveAllFacets() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/facet")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertTrue(node.isArray());
        }
    }

    /**
     * @since 8.10
     */
    @Test
    public void itCanRetrieveAllDocTypes() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/docType")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertTrue(node.isObject());
            assertTrue(node.has("doctypes"));
        }
    }
}
