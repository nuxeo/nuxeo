/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Test doc resolved doc field re-posted.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, DirectoryFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-directory-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-multi-resolved-fields-docTypes.xml")
public class DocWithMultiResolvedFieldTest extends BaseTest {

    private static String createDocumentJSON(String properties) {
        String doc = "{";
        doc += "\"entity-type\":\"document\" ,";
        doc += "\"name\":\"doc1\" ,";
        doc += "\"type\":\"MultiResolved\"";
        if (properties != null) {
            doc += ", \"properties\": {";
            doc += properties;
            doc += "}";
        }
        doc += "}";
        return doc;
    }

    @Test
    public void testRePostResolvedXVocabularyEntry() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("fetch-document", "properties");
        headers.put("properties", "*");
        headers.put("fetch-directoryEntry", "parent");
        JsonNode node = null;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/",
                createDocumentJSON("\"mr:countries\": [\"Albania\"]"), headers)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            node = mapper.readTree(response.getEntityInputStream());
            assertNotNull(node);
            JsonNode props = node.get("properties");
            assertNotNull(props);
            assertNotNull(props.has("mr:countries"));
            ArrayNode countries = (ArrayNode) props.get("mr:countries");
            assertEquals(1, countries.size());
            JsonNode firstCountry = countries.get(0);
            assertTrue(firstCountry.isObject());
            assertTrue(firstCountry.has("properties"));
            assertTrue(firstCountry.get("properties").has("parent"));
            assertTrue(firstCountry.get("properties").get("parent").isObject());
        }
        // Re-Post identical
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "path/doc1", node.toString())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

    }

}
