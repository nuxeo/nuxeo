/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.core.io.FavoritesJsonEnricher;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.BasePermissionsJsonEnricher;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.BreadcrumbJsonEnricher;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.FirstAccessibleAncestorJsonEnricher;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.preview.io.PreviewJsonEnricher;
import org.nuxeo.ecm.platform.rendition.io.PublicationJsonEnricher;
import org.nuxeo.ecm.platform.rendition.io.RenditionJsonEnricher;
import org.nuxeo.ecm.platform.tag.io.TagsJsonEnricher;
import org.nuxeo.ecm.platform.types.SubtypesJsonEnricher;
import org.nuxeo.ecm.restapi.server.jaxrs.enrichers.AuditJsonEnricher;
import org.nuxeo.ecm.restapi.server.jaxrs.enrichers.HasContentJsonEnricher;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.http.test.handler.VoidHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.user.preferences.directory.UserPreferencesFeature;
import org.nuxeo.user.preferences.io.DocumentUserPreferencesJsonEnricher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, LogFeature.class, LogCaptureFeature.class, CollectionFeature.class,
        UserPreferencesFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-defaultvalue-docTypes.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-dummy-listener-contrib.xml")
public class EmptyDocumentTest {

    protected static final Map<String, String> HEADERS = Collections.singletonMap("properties", "*");

    protected static final Map<String, String> ENRICHERS_HEADERS = Collections.singletonMap("enrichers-document",
            String.join(",", Arrays.asList(HasContentJsonEnricher.NAME, FirstAccessibleAncestorJsonEnricher.NAME,
                    BasePermissionsJsonEnricher.NAME, BreadcrumbJsonEnricher.NAME, PublicationJsonEnricher.NAME,
                    TagsJsonEnricher.NAME, PreviewJsonEnricher.NAME, FavoritesJsonEnricher.NAME, AuditJsonEnricher.NAME,
                    SubtypesJsonEnricher.NAME, RenditionJsonEnricher.NAME, DocumentUserPreferencesJsonEnricher.NAME)));

    @Inject
    protected CoreSession session;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected RestServerFeature restServerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultClient(
            () -> restServerFeature.getRestApiUrl());

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testEmptyDocumentEnrichers() {
        DocumentModel folder = RestServerInit.getFolder(0, session);

        httpClient.buildGetRequest("/id/" + folder.getId() + "/@emptyWithDefault")
                  .addQueryParameter("type", "DocDefaultValue")
                  .addHeaders(ENRICHERS_HEADERS)
                  .execute(new VoidHandler());
        List<LogEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(0, events.size());
    }

    @Test
    public void testEmptyDocumentCreationWithParent() {
        DocumentModel folder = RestServerInit.getFolder(0, session);

        httpClient.buildGetRequest("/id/" + folder.getId() + "/@emptyWithDefault")
                  .executeAndConsume(new JsonNodeHandler(SC_BAD_REQUEST), this::assertError);

        httpClient.buildGetRequest("/id/" + folder.getId() + "/@emptyWithDefault")
                  .addQueryParameter("type", "DocDefaultValue")
                  .addHeaders(HEADERS)
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> assertEmptyDocument(folder.getPathAsString() + "/null", node));

        httpClient.buildGetRequest("/id/" + folder.getId() + "/@emptyWithDefault")
                  .addQueryParameter("type", "DocDefaultValue")
                  .addQueryParameter("name", "foo")
                  .addHeaders(HEADERS)
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> assertEmptyDocument(folder.getPathAsString() + "/foo", node));
    }

    protected void assertError(JsonNode node) {
        assertNotNull(node);
        assertEquals("exception", node.get("entity-type").textValue());
        assertEquals("Missing type parameter", node.get("message").textValue());
    }

    @Test
    public void testEmptyDocumentCreationWithoutParent() {
        httpClient.buildGetRequest("/@emptyWithDefault")
                  .executeAndConsume(new JsonNodeHandler(SC_BAD_REQUEST), this::assertError);

        httpClient.buildGetRequest("/@emptyWithDefault")
                  .addQueryParameter("type", "DocDefaultValue")
                  .addHeaders(HEADERS)
                  .executeAndConsume(new JsonNodeHandler(), node -> assertEmptyDocument(null, node));

        httpClient.buildGetRequest("/@emptyWithDefault")
                  .addQueryParameter("type", "DocDefaultValue")
                  .addQueryParameter("name", "foo")
                  .addHeaders(HEADERS)
                  .executeAndConsume(new JsonNodeHandler(), node -> assertEmptyDocument("foo", node));
    }

    protected void assertEmptyDocument(String expectedPath, JsonNode node) {
        assertNotNull(node);
        assertEquals("document", node.get("entity-type").textValue());
        assertEquals("DocDefaultValue", node.get("type").textValue());
        assertEquals(expectedPath, node.get("path").textValue());

        JsonNode properties = node.get("properties");
        assertNull(properties.get("dv:simpleWithoutDefault").textValue());
        assertEquals("value", properties.get("dv:simpleWithDefault").textValue());
        JsonNode multiWithDefault = properties.get("dv:multiWithDefault");
        assertEquals(0, properties.get("dv:multiWithoutDefault").size());
        assertEquals("value1", multiWithDefault.get(0).textValue());
        assertEquals("value2", multiWithDefault.get(1).textValue());
        assertEquals("dummy source", properties.get("dc:source").textValue());

        JsonNode complexWithoutDefault = properties.get("dv:complexWithoutDefault");
        assertTrue(complexWithoutDefault.get("foo").isNull());
        assertTrue(complexWithoutDefault.get("bar").isNull());

        JsonNode complexWithDefault = properties.get("dv:complexWithDefault");
        assertTrue(complexWithDefault.get("foo").isNull());
        assertEquals("value", complexWithDefault.get("bar").textValue());
    }

    @Test
    public void testOverrideEmptyDocumentListenerValues() {
        DocumentModel folder = RestServerInit.getFolder(0, session);
        String data = """
                {
                  "entity-type": "document",
                  "type": "DocDefaultValue",
                  "name": "foo",
                  "properties": {
                    "dc:source": null,
                    "dc:title": null
                  }
                }
                """;
        httpClient.buildPostRequest("/path" + folder.getPathAsString())
                  .entity(data)
                  .contentType(MediaType.APPLICATION_JSON)
                  .addHeader("X-NXDocumentProperties", "dublincore")
                  .executeAndConsume(new JsonNodeHandler(SC_CREATED), jsonNode -> {
                      // dc:source is set by DummyEmptyDocumentListener and overridden to be null
                      assertNull(jsonNode.get("properties").get("dc:source").textValue());
                      // dc:subjects is set by DummyEmptyDocumentListener and not overridden
                      JsonNode subjects = jsonNode.get("properties").get("dc:subjects");
                      assertTrue(subjects.isArray());
                      assertEquals("dummy subject", subjects.get(0).textValue());
                  });
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-operation-getdocumentparent-contrib.xml")
    public void testEmptyDocumentCreationAndCallGetDocumentParentOperation() {
        DocumentModel folder = RestServerInit.getFolder(0, session);
        httpClient.buildGetRequest("/id/" + folder.getId() + "/@emptyWithDefault")
                  .addQueryParameter("type", "File")
                  .addQueryParameter("name", "foo")
                  .addHeaders(HEADERS)
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> assertEquals("/folder_0", node.get("parentRef").textValue()));
    }

}
