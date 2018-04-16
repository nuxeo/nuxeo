/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.io.marshallers.json.types.SchemaJsonWriter;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class SchemaTest extends BaseTest {

    @Test
    public void testFieldsWithConstraintsFetch() throws IOException {
        // Given the dublincore

        // When I call the schema Rest endpoint
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + SchemaJsonWriter.ENTITY_TYPE, SchemaJsonWriter.FETCH_FIELDS);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/schema/dublincore", queryParams)) {

            // Then it returns the dublincore schema Json with constraints
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode fields = node.get("fields");
            assertNotNull(fields);

            JsonNode creator = fields.get("creator");
            assertNotNull(creator);

            JsonNode type = creator.get("type");
            assertNotNull(type);
            assertEquals("string", type.textValue());

            JsonNode constraints = creator.get("constraints");
            assertNotNull(constraints);
            assertTrue(constraints.isArray());
            assertTrue(constraints.size() > 0);

            JsonNode contributors = fields.get("contributors");
            assertNotNull(contributors);

            type = contributors.get("type");
            assertNotNull(type);
            assertEquals("string[]", type.textValue());

            constraints = contributors.get("constraints");
            assertNotNull(constraints);
            assertTrue(constraints.isArray());

            JsonNode itemConstraints = contributors.get("itemConstraints");
            assertNotNull(itemConstraints);
            assertTrue(itemConstraints.isArray());
            assertTrue(itemConstraints.size() > 0);
        }
    }
}
