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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.marshallers.json.types.SchemaJsonWriter;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Test class for 'docType', 'facet' and 'schema' endpoint.
 *
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class ConfigTest extends BaseTest {

    /**
     * @since 8.10
     */
    @Test
    public void itCanRetrieveSchemaDefinitionWithoutFieldConstraints() throws IOException {
        // When I call the Rest schema endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/schema/dublincore");

        // Then I can retrieve schema fields type
        assertEquals("schema", node.get("entity-type").getTextValue());
        JsonNode fields = node.get("fields");
        assertTrue(fields.size() > 0);
        JsonNode creatorField = fields.get("creator");
        assertEquals("string", creatorField.getTextValue());
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
        assertEquals("schema", node.get("entity-type").getTextValue());
        JsonNode fields = node.get("fields");
        assertTrue(fields.size() > 0);
        JsonNode creatorField = fields.get("creator");
        assertEquals("string", creatorField.get("type").getTextValue());
        ArrayNode constraints = (ArrayNode) creatorField.get("constraints");
        assertEquals(2, constraints.size());
        JsonNode userConstraint = constraints.get(0);
        assertEquals("userManagerResolver", userConstraint.get("name").getTextValue());
        JsonNode userConstraintParams = userConstraint.get("parameters");
        assertEquals(2, userConstraintParams.size());
    }
}
