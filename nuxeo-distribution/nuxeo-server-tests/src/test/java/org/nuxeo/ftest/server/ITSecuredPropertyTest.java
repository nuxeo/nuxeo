/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ftest.server;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;
import static org.nuxeo.functionaltests.AbstractTest.TEST_PASSWORD;
import static org.nuxeo.functionaltests.AbstractTest.TEST_USERNAME;

import java.io.IOException;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.HttpClientTestRule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the secured property handled by {@link PropertyCharacteristicHandler} and contributed by
 * {@code CoreExtension.xml}.
 *
 * @since 11.1
 */
public class ITSecuredPropertyTest {

    public static final String REST_API_URL = NUXEO_URL + "/api/v1";

    public static final String APPLICATION_JSON = "application/json";

    public static final String CONTENT_TYPE = "Content-Type";

    @Rule
    public final HttpClientTestRule adminHttpClientRule = new HttpClientTestRule.Builder().url(REST_API_URL)
                                                                                          .adminCredentials()
                                                                                          .header(CONTENT_TYPE,
                                                                                                  APPLICATION_JSON)
                                                                                          .build();

    @Rule
    public final HttpClientTestRule testHttpClientRule = new HttpClientTestRule.Builder().url(REST_API_URL)
                                                                                         .credentials(TEST_USERNAME,
                                                                                                 TEST_PASSWORD)
                                                                                         .header(CONTENT_TYPE,
                                                                                                 APPLICATION_JSON)
                                                                                         .build();

    protected final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void beforeClass() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
    }

    @AfterClass
    public static void afterClass() {
        RestHelper.cleanupUsers();
    }

    @Test
    public void testAdministratorCanEdit() {
        try (CloseableClientResponse response = adminHttpClientRule.post("path/", instantiateDocumentBody())) {
            assertEquals(SC_CREATED, response.getStatus());
        }
    }

    @Test
    public void testUserCanNotEdit() throws IOException {
        try (CloseableClientResponse response = testHttpClientRule.post("path/", instantiateDocumentBody())) {
            assertEquals(SC_BAD_REQUEST, response.getStatus());
            JsonNode root = mapper.readTree(response.getEntityInputStream());
            assertTrue(root.hasNonNull("message"));
            JsonNode message = root.get("message");
            assertTrue(message.isTextual());
            assertEquals("Cannot set the value of property: dc:creator since it is readonly", message.asText());
        }
    }

    @NotNull
    protected String instantiateDocumentBody() {
        try {
            return mapper.writeValueAsString( //
                    Map.of("entity-type", "document", //
                            "name", "file", //
                            "type", "File", //
                            "properties", Map.of("dc:creator", "john") //
                    ));
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to serialize document body", e);
        }
    }

}
