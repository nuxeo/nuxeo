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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.app.WebEngineExceptionMapper;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test")
public class ExceptionRestTest extends BaseTest {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    public void testSimpleException() throws IOException {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a wrong GET Request
        try (CloseableClientResponse response = getResponse(RequestType.GET, "wrongpath" + note.getPathAsString())) {

            JsonNode node = mapper.readTree(response.getEntityInputStream());

            // Then i get an exception and parse it to check json payload
            assertEquals("exception", node.get("entity-type").textValue());
            assertEquals(404, node.get("status").intValue());
            assertEquals("Type not found: wrongpath", node.get("message").textValue());
        }
    }

    @Test
    public void testExtendedException() throws IOException {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        if (!jsonFactoryManager.isStackDisplay()) {
            jsonFactoryManager.toggleStackDisplay();
        }

        // When I do a request with a wrong document ID
        try (CloseableClientResponse response = getResponse(RequestType.GET, "path" + "/wrongID")) {

            JsonNode node = mapper.readTree(response.getEntityInputStream());

            // Then i get an exception and parse it to check json payload
            assertEquals("exception", node.get("entity-type").textValue());
            assertEquals(404, node.get("status").intValue());
            assertEquals("/wrongID", node.get("message").textValue());
            assertNotNull(node.get("stacktrace").textValue());
            assertEquals(DocumentNotFoundException.class.getCanonicalName(),
                    node.get("exception").get("className").textValue());
        }
    }

    @Test
    @LogCaptureFeature.FilterWith(ExceptionLogFilter.class)
    public void testNotFoundEndpoint() throws IOException {
        try (CloseableClientResponse r = getResponse(RequestType.GET, "/foo/notfound")) {
            assertEquals(404, r.getStatus());
            JsonNode node = mapper.readTree(r.getEntityInputStream());
            assertEquals(404, node.get("status").numberValue());
            assertEquals(
                    "com.sun.jersey.api.NotFoundException: null for uri: http://localhost:18090/api/v1/foo/notfound",
                    node.get("message").textValue());

            List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
            assertEquals(0, caughtEvents.size());
        }
    }

    @Test
    @LogCaptureFeature.FilterWith(ExceptionLogFilter.class)
    public void testEndpointWithException() throws IOException {
        try (CloseableClientResponse r = getResponse(RequestType.GET, "/foo/exception")) {
            assertEquals(500, r.getStatus());
            JsonNode node = mapper.readTree(r.getEntityInputStream());
            assertEquals(500, node.get("status").numberValue());
            assertEquals("foo", node.get("message").textValue());

            List<LoggingEvent> caughtEvents = logCaptureResult.getCaughtEvents();
            assertEquals(1, caughtEvents.size());
            LoggingEvent loggingEvent = caughtEvents.get(0);
            assertEquals("org.nuxeo.ecm.core.api.NuxeoException: foo", loggingEvent.getRenderedMessage());
        }
    }

    public static class ExceptionLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            return event.getLevel().isGreaterOrEqual(Level.ERROR)
                    && (event.getLoggerName().contains(WebEngineExceptionMapper.class.getSimpleName()));
        }
    }
}
