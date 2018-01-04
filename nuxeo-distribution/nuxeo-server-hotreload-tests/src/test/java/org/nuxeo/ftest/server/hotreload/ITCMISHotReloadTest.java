/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
package org.nuxeo.ftest.server.hotreload;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.HttpClientTestRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Tests hot reload from CMIS.
 *
 * @since 10.1
 */
public class ITCMISHotReloadTest {

    @Rule
    public final HotReloadTestRule hotReloadRule = new HotReloadTestRule();

    @Rule
    public final HttpClientTestRule httpClientRule = new HttpClientTestRule.Builder().url(NUXEO_URL + "/json/cmis")
                                                                                     .adminCredentials()
                                                                                     .build();

    public final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testHotReloadDocumentType() throws Exception {
        // get root id
        String rootId;
        try (CloseableClientResponse response = httpClientRule.get("")) {
            JsonNode root = mapper.readTree(response.getEntityInputStream());
            rootId = root.get("default").get("rootFolderId").asText();
        }

        // test create a document
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("cmisaction", "createDocument");
        formData.add("propertyId[0]", "cmis:objectTypeId");
        formData.add("propertyValue[0]", "HotReload");
        formData.add("propertyId[1]", "cmis:name");
        formData.add("propertyValue[1]", "hot reload");
        formData.add("propertyId[2]", "hr:content");
        formData.add("propertyValue[2]", "some content");
        formData.add("succinct", "true");
        try (CloseableClientResponse response = httpClientRule.post("default/root?objectId=" + rootId, formData)) {
            assertEquals(201, response.getStatus());
        }
    }

}
