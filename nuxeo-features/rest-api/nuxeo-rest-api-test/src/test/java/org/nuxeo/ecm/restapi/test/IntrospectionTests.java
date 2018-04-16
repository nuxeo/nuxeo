/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class IntrospectionTests extends BaseTest {

    @Test
    public void itCanFetchSchemas() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/config/schemas")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            Assert.assertTrue(node.size() > 0);
            boolean dcFound = false;
            for (int i = 0; i < node.size(); i++) {
                if ("dublincore".equals(node.get(i).get("name").asText())) {
                    dcFound = true;
                    break;
                }
            }
            Assert.assertTrue(dcFound);
        }
    }

    @Test
    public void itCanFetchASchema() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/config/schemas/dublincore")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            String json = IOUtils.toString(response.getEntityInputStream());
            JsonAssert jsonAssert = JsonAssert.on(json);

            jsonAssert.has("name").isEquals("dublincore");
            jsonAssert.has("@prefix").isEquals("dc");
            jsonAssert.has("fields.creator").isEquals("string");
            jsonAssert.has("fields.contributors").isEquals("string[]");
        }
    }

    @Test
    public void itCanFetchFacets() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/config/facets")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            Assert.assertTrue(node.size() > 0);

            boolean found = false;
            for (int i = 0; i < node.size(); i++) {
                if ("HasRelatedText".equals(node.get(i).get("name").asText())) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
    }

    @Test
    public void itCanFetchAFacet() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/config/facets/HasRelatedText")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            Assert.assertEquals("HasRelatedText", node.get("name").asText());
            Assert.assertEquals("relatedtext", node.get("schemas").get(0).get("name").asText());
        }
    }

    @Test
    public void itCanFetchTypes() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/config/types")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            // the export is done as a compound object rather than an array !

            Assert.assertTrue(node.has("doctypes"));
            Assert.assertTrue(node.has("schemas"));

            Assert.assertTrue(node.get("doctypes").has("File"));
            Assert.assertTrue(node.get("schemas").has("dublincore"));
        }
    }

    @Test
    public void itCanFetchAType() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/config/types/File")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            // the export is done as a compound object rather than an array !

            Assert.assertEquals("Document", node.get("parent").asText());

            boolean dcFound = false;
            JsonNode schemas = node.get("schemas");
            for (int i = 0; i < schemas.size(); i++) {
                if ("dublincore".equals(schemas.get(i).get("name").asText())) {
                    dcFound = true;
                    break;
                }
            }
            Assert.assertTrue(dcFound);
        }
    }

}
