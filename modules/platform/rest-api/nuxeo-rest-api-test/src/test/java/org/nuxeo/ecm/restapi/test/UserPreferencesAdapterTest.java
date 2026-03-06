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
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.UserPreferencesAdapter;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.http.test.handler.StringHandler;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.user.preferences.directory.UserPreferencesFeature;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @since 2025.16
 */
@RunWith(FeaturesRunner.class)
@Features({ UserPreferencesFeature.class, RestServerFeature.class })
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class UserPreferencesAdapterTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected RestServerFeature restServerFeature;

    protected DocumentModel note;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder()
                                                                   .url(() -> restServerFeature.getRestApiUrl())
                                                                   .credentials("user1", "user1")
                                                                   .accept(MediaType.APPLICATION_JSON)
                                                                   .build();

    @Before
    public void setup() {
        note = RestServerInit.getNote(1, session);
        ACE ace = ACE.builder("user1", READ).creator(session.getPrincipal().getName()).isGranted(true).build();
        ACP acp = new ACPImpl();
        acp.addACE(ACL.LOCAL_ACL, ace);
        session.setACP(note.getRef(), acp, false);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testUserPreferencesAdapterCRUD() {
        // Create
        httpClient.buildPutRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME).entity("""
                {
                  "entity-type" : "userPreferences",
                  "preferences" : {
                    "foo" : "bar"
                  }
                }""").executeAndConsume(new StringHandler(SC_CREATED), s -> JSONAssert.assertEquals("""
                {
                  "entity-type" : "userPreferences",
                  "preferences" : {
                    "foo" : "bar"
                  }
                }""", s, JSONCompareMode.NON_EXTENSIBLE));

        // Get
        httpClient.buildGetRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME) //
                  .executeAndConsume(new StringHandler(SC_OK), s -> JSONAssert.assertEquals("""
                          {
                            "entity-type" : "userPreferences",
                            "preferences" : {
                              "foo" : "bar"
                            }
                          }""", s, JSONCompareMode.NON_EXTENSIBLE));

        // Update: Put more preferences
        httpClient.buildPutRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME).entity("""
                {
                  "entity-type" : "userPreferences",
                  "preferences" : {
                    "key1" : "value1"
                  }
                }""").executeAndConsume(new StringHandler(SC_OK), s -> JSONAssert.assertEquals("""
                {
                  "entity-type" : "userPreferences",
                  "preferences" : {
                    "foo" : "bar",
                    "key1" : "value1"
                  }
                }""", s, JSONCompareMode.NON_EXTENSIBLE));

        // Get a single key
        httpClient.buildGetRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME + "/foo")
                  .executeAndConsume(new StringHandler(SC_OK), s -> JSONAssert.assertEquals("""
                          {
                            "entity-type" : "userPreference",
                            "key" : "foo",
                            "value" : "bar"
                          }""", s, JSONCompareMode.NON_EXTENSIBLE));

        // Update: Replace preferences
        httpClient.buildPutRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME).entity("""
                {
                  "entity-type" : "userPreferences",
                  "preferences" : {
                    "key1" : "anotherValue1",
                    "key2" : "value2"
                  }
                }""").executeAndConsume(new StringHandler(SC_OK), s -> JSONAssert.assertEquals("""
                {
                  "entity-type" : "userPreferences",
                  "preferences" : {
                    "foo" : "bar",
                    "key1" : "anotherValue1",
                    "key2" : "value2"
                  }
                }""", s, JSONCompareMode.NON_EXTENSIBLE));
        // Update: Remove a single preference
        httpClient.buildDeleteRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME + "/key1")
                  .executeAndConsume(new StringHandler(SC_OK), s -> JSONAssert.assertEquals("""
                          {
                            "entity-type" : "userPreferences",
                            "preferences" : {
                              "foo" : "bar",
                              "key2" : "value2"
                            }
                          }""", s, JSONCompareMode.NON_EXTENSIBLE));
        // Update: Remove unknown preference key
        httpClient.buildDeleteRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME + "/unknown")
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

        // Delete
        httpClient.buildDeleteRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));
        // Check delete is idempotent
        httpClient.buildDeleteRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));
        // Check delete is effective
        httpClient.buildGetRequest("/id/" + note.getId() + "/@" + UserPreferencesAdapter.NAME) //
                  .executeAndConsume(new StringHandler(SC_OK), s -> JSONAssert.assertEquals("""
                          {
                            "entity-type" : "userPreferences",
                            "preferences" : {
                            }
                          }""", s, JSONCompareMode.NON_EXTENSIBLE));
    }
}
