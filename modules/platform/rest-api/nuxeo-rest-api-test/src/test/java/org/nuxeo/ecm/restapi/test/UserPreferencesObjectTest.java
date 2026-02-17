/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.user.preferences.directory.UserPreferencesFeature;

/**
 * @since 2025.16
 */
@RunWith(FeaturesRunner.class)
@Features({ UserPreferencesFeature.class, RestServerFeature.class })
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class UserPreferencesObjectTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected RestServerFeature restServerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultJsonClient(
            () -> restServerFeature.getRestApiUrl());

    @Test
    public void testUserPreferenceCRUD() {
        // Create
        httpClient.buildPutRequest("/me/preferences/foo")
                  .entity("bar")
                  .executeAndConsume(new JsonNodeHandler(SC_CREATED),
                          node -> assertEquals("bar", node.get("value").asText()));
        // Create other
        httpClient.buildPutRequest("/me/preferences/otherKey")
                  .entity("otherValue")
                  .executeAndConsume(new JsonNodeHandler(SC_CREATED),
                          node -> assertEquals("otherValue", node.get("value").asText()));

        // Get one
        httpClient.buildGetRequest("/me/preferences/foo") //
                  .executeAndConsume(new JsonNodeHandler(SC_OK),
                          node -> assertEquals("bar", node.get("value").asText()));

        // Get all
        httpClient.buildGetRequest("/me/preferences") //
                  .executeAndConsume(new JsonNodeHandler(SC_OK), node -> {
                      assertTrue(node.has("preferences"));
                      assertEquals(2, node.get("preferences").size());
                  });

        // Update
        httpClient.buildPutRequest("/me/preferences/foo")
                  .entity("otherValue")
                  .executeAndConsume(new JsonNodeHandler(SC_OK),
                          node -> assertEquals("otherValue", node.get("value").asText()));

        // Delete
        httpClient.buildDeleteRequest("/me/preferences/foo")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));
        // Check delete is idempotent
        httpClient.buildDeleteRequest("/me/preferences/foo")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        // Check delete is effective
        httpClient.buildGetRequest("/me/preferences/foo")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));

        // Get all
        httpClient.buildGetRequest("/me/preferences") //
                  .executeAndConsume(new JsonNodeHandler(SC_OK), node -> {
                      assertTrue(node.has("preferences"));
                      assertEquals(1, node.get("preferences").size());
                  });
    }

}
