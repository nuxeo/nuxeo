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

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.model.TypeNotFoundException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.ClientResponse;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class ExceptionRestTest extends BaseTest {

    @Test
    public void testSimpleException() throws IOException {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a wrong GET Request
        ClientResponse response = getResponse(RequestType.GET, "wrongpath" + note.getPathAsString());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // Then i get an exception and parse it to check json payload
        assertEquals("exception", node.get("entity-type").getTextValue());
        assertEquals(TypeNotFoundException.class.getCanonicalName(), node.get("code").getTextValue());
        assertEquals(500, node.get("status").getIntValue());
        assertEquals("Type not found: wrongpath",
                node.get("message").getTextValue());
    }

    @Test
    public void testExtendedException() throws IOException {
        JsonFactoryManager jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
        if (!jsonFactoryManager.isStackDisplay()) {
            jsonFactoryManager.toggleStackDisplay();
        }

        // When I do a request with a wrong document ID
        ClientResponse response = getResponse(RequestType.GET, "path" + "/wrongID");

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // Then i get an exception and parse it to check json payload
        assertEquals("exception", node.get("entity-type").getTextValue());
        assertEquals(DocumentNotFoundException.class.getCanonicalName(), node.get("code").getTextValue());
        assertEquals(404, node.get("status").getIntValue());
        assertEquals("/wrongID", node.get("message").getTextValue());
        assertNotNull(node.get("stacktrace").getTextValue());
        assertEquals(DocumentNotFoundException.class.getCanonicalName(),
                node.get("exception").get("className").getTextValue());
    }
}
