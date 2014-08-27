/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        ClientResponse response = getResponse(RequestType.GET,
                "wrongpath" + note.getPathAsString());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // Then i get an exception and parse it to check json payload
        assertEquals("exception", node.get("entity-type").getTextValue());
        assertEquals(NotFoundException.class.getCanonicalName(),
                node.get("code").getTextValue());
        assertEquals(500, node.get("status").getIntValue());
        assertEquals("null for uri: " +
                        "http://localhost:18090/api/v1/wrongpath/folder_1" +
                        "/note_0",
                node.get("message").getTextValue());
    }

    @Test
    public void testExtendedException() throws IOException {
        JsonFactoryManager jsonFactoryManager = Framework.getLocalService
                (JsonFactoryManager.class);
        if (!jsonFactoryManager.isStackDisplay()) {
            jsonFactoryManager.toggleStackDisplay();
        }

        // When I do a request with a wrong document ID
        ClientResponse response = getResponse(RequestType.GET,
                "path" + "/wrongID");

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        // Then i get an exception and parse it to check json payload
        assertEquals("exception", node.get("entity-type").getTextValue());
        assertEquals(NoSuchDocumentException.class.getCanonicalName(),
                node.get("code").getTextValue());
        assertEquals(404, node.get("status").getIntValue());
        assertEquals("No such document: No such document: /wrongID",
                node.get("message").getTextValue());
        assertNotNull(node.get("stacktrace").getTextValue());
        assertEquals(NoSuchDocumentException.class.getCanonicalName(),
                node.get("exception").get
                ("className").getTextValue());
    }
}
