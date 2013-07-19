/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.*;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.rest.jaxrs.adapters.BOAdapter;
import org.nuxeo.ecm.automation.test.adapters.BusinessBeanAdapter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.ClientResponse;

/**
 *
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@LocalDeploy({ "nuxeo-automation-restserver:operation-contrib.xml" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AdapterBindingTest extends BaseTest {


    @Test
    public void iCanGetAnAdapter() throws Exception {

        // Given a note
        DocumentModel note = RestServerInit.getNote(1, session);

        // When i browse the adapter
        ClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + BOAdapter.NAME
                        + "/BusinessBeanAdapter");

        // Then i receive a formatted response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("BusinessBeanAdapter",
                node.get("entity-type").getValueAsText());
        assertEquals(note.getPropertyValue("note:note"),
                node.get("value").get("note").getValueAsText());

    }

    @Test
    public void iCanSaveAnAdapter() throws Exception {
        // Given a note and a modified business object representation
        DocumentModel note = RestServerInit.getNote(1, session);
        String ba = String.format(
                "{\"entity-type\":\"BusinessBeanAdapter\",\"value\":{\"type\""
                        + ":\"Note\",\"id\":\"%s\","
                        + "\"note\":\"Note 1\",\"title\":\"Note 1\",\"description\":\"description\"}}",
                note.getId());
        assertTrue(StringUtils.isBlank((String) note.getPropertyValue("dc:description")));

        // When i do a put request on it
        ClientResponse response = getResponse(RequestType.PUT,
                "/id/" + note.getId() + "/@" + BOAdapter.NAME
                        + "/BusinessBeanAdapter", ba);

        // Then it modifies the description
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        dispose(session);
        note = session.getDocument(note.getRef());
        assertEquals("description",
                note.getAdapter(BusinessBeanAdapter.class).getDescription());

    }

    @Test
    public void iCanCreateAnAdapter() throws Exception {
        // Given a note and a modified business object representation
        DocumentModel folder = RestServerInit.getFolder(0, session);
        String ba = String.format(
                "{\"entity-type\":\"BusinessBeanAdapter\",\"value\":{\"type\""
                        + ":\"Note\","
                        + "\"note\":\"Note 1\",\"title\":\"Note 1\",\"description\":\"description\"}}",
                folder.getId());
        assertTrue(session.getChildren(folder.getRef()).isEmpty());

        // When i do a put request on it
        ClientResponse response = getResponse(RequestType.POST,
                "/id/" + folder.getId() + "/@" + BOAdapter.NAME
                        + "/BusinessBeanAdapter/note2", ba);

        // Then it modifies the description
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        dispose(session);
        assertFalse(session.getChildren(folder.getRef()).isEmpty());
    }
}
