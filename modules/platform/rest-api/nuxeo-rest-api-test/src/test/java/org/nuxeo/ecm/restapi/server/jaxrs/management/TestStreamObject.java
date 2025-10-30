/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionD2Writer.PARAMETER_EXCLUDE_FILTER;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionD2Writer.PARAMETER_EXCLUDE_INACTIVE;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_D2;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_PLANT_UML;
import static org.nuxeo.ecm.restapi.server.jaxrs.management.StreamObject.D2_FORMAT;
import static org.nuxeo.ecm.restapi.server.jaxrs.management.StreamObject.PUML_FORMAT;

import org.junit.Test;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.http.test.handler.StringHandler;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2021.35
 */
@WithFrameworkProperty(name = StreamObject.ENABLED_OPTION, value = "true")
public class TestStreamObject extends ManagementBaseTest {

    @Test
    public void testListStreams() {
        httpClient.buildGetRequest("/management/stream/streams").executeAndConsume(new JsonNodeHandler(), result -> {
            assertTrue(result.isArray());
            assertFalse(result.isEmpty());
            assertEquals("avro", result.get(0).get("codec").asText());
        });
    }

    @Test
    public void testListConsumers() {
        // without stream parameter
        httpClient.buildGetRequest("/management/stream/consumers").executeAndConsume(new JsonNodeHandler(), result -> {
            assertTrue(result.isArray());
            assertTrue(result.isEmpty());
        });

        // with stream parameter
        httpClient.buildGetRequest("/management/stream/consumers")
                  .addQueryParameter("stream", "internal/processors")
                  .executeAndConsume(new JsonNodeHandler(), result -> {
                      assertEquals(1, result.size());
                      var consumer = result.get(0);
                      assertEquals("internal/processors", consumer.get("stream").asText());
                      assertEquals("stream/introspection", consumer.get("consumer").asText());
                  });
    }

    @Test
    public void testStreamIntrospection() {
        httpClient.buildGetRequest("/management/stream")
                  .executeAndConsume(new JsonNodeHandler(), result -> assertTrue(result.isObject()));
    }

    @Test
    public void testStreamIntrospectionPuml() {
        // with header
        httpClient.buildGetRequest("/management/stream")
                  .accept(TEXT_PLANT_UML)
                  .executeAndConsume(new StringHandler(), puml -> {
                      assertTrue(puml.contains("@startuml"));
                      assertTrue(puml.contains("@enduml"));
                  });
        // with query parameter
        httpClient.buildGetRequest("/management/stream")
                  .addQueryParameter("format", PUML_FORMAT)
                  .executeAndConsume(new StringHandler(), puml -> {
                      assertTrue(puml.contains("@startuml"));
                      assertTrue(puml.contains("@enduml"));
                  });
    }

    @Test
    public void testStreamIntrospectionD2() {
        // with header
        httpClient.buildGetRequest("/management/stream")
                  .accept(TEXT_D2)
                  .executeAndConsume(new StringHandler(), d2 -> assertTrue(d2.contains("# Stream Introspection")));
        // with query parameter
        httpClient.buildGetRequest("/management/stream")
                  .addQueryParameter("format", D2_FORMAT)
                  .executeAndConsume(new StringHandler(), d2 -> assertTrue(d2.contains("# Stream Introspection")));
    }

    @Test
    public void testStreamIntrospectionD2WithFiltering() {
        // Test with pattern filtering to exclude "bulk/" patterns
        httpClient.buildGetRequest("/management/stream")
                  .accept(TEXT_D2)
                  .addQueryParameter(PARAMETER_EXCLUDE_FILTER, "bulk/")
                  .addQueryParameter(PARAMETER_EXCLUDE_INACTIVE, "")
                  .executeAndConsume(new StringHandler(), d2 -> {
                      assertFalse(d2.contains("stream_bulk_command"));
                      assertFalse(d2.contains("group_bulk_scroller"));
                  });

        // Test with inactive filtering (should exclude both inactive computations and empty streams)
        httpClient.buildGetRequest("/management/stream")
                  .accept(TEXT_D2)
                  .addQueryParameter(PARAMETER_EXCLUDE_FILTER, "")
                  .addQueryParameter(PARAMETER_EXCLUDE_INACTIVE, "true")
                  .executeAndConsume(new StringHandler(), d2 -> assertFalse(d2.contains("bulk_")));

        // Test with both filters
        httpClient.buildGetRequest("/management/stream")
                  .accept(TEXT_D2)
                  .addQueryParameter(PARAMETER_EXCLUDE_FILTER, "bulk/")
                  .addQueryParameter(PARAMETER_EXCLUDE_INACTIVE, "true")
                  .executeAndConsume(new StringHandler(), d2 -> assertFalse(d2.contains("bulk_")));
    }

    @Test
    public void testScale() {
        httpClient.buildGetRequest("/management/stream/scale").executeAndConsume(new JsonNodeHandler(), result -> {
            assertTrue(result.isObject());
            assertTrue(result.at("/scale").isObject());
        });
    }

}
