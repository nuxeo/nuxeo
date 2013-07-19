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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Test the CRUD rest API
 *
 * @since 5.7.2
 */

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentBrowsingTest extends BaseTest {

    @Test
    public void iCanBrowseTheRepoByItsPath() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request
        ClientResponse response = getResponse(RequestType.GET,
                "path" + note.getPathAsString());

        // Then i get a document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEntityEqualsDoc(response.getEntityInputStream(), note);

    }

    @Test
    public void iCanBrowseTheRepoByItsId() throws Exception {
        // Given a document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request
        ClientResponse response = getResponse(RequestType.GET,
                "id/" + note.getId());

        // The i get the document as Json
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEntityEqualsDoc(response.getEntityInputStream(), note);

    }

    @Test
    public void iCanGetTheChildrenOfADoc() throws Exception {
        // Given a folder with one document
        DocumentModel folder = RestServerInit.getFolder(0, session);
        DocumentModel child = session.createDocumentModel(
                folder.getPathAsString(), "note", "Note");
        child = session.createDocument(child);
        session.save();

        // When i call a GET on the children for that doc
        ClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@children");

        // Then i get the only document of the folder
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        Iterator<JsonNode> elements = node.get("entries").getElements();
        node = elements.next();

        assertNodeEqualsDoc(node, child);

    }

    @Test
    public void iCanUpdateADocument() throws Exception {

        // Given a document
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(RequestType.GET,
                "id/" + note.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // When i do a PUT request on the document with modified data
        JSONDocumentNode jsonDoc = new JSONDocumentNode(
                response.getEntityInputStream());
        jsonDoc.setPropertyValue("dc:title", "New title");
        response = getResponse(RequestType.PUT, "id/" + note.getId(),
                jsonDoc.asJson());

        // Then the document is updated
        dispose(session);
        note = RestServerInit.getNote(0, session);
        assertEquals("New title", note.getTitle());

    }

    @Test
    public void iCanCreateADocument() throws Exception {

        // Given a Rest Creation request
        String data = "{\"entity-type\": \"document\",\"type\": \"File\",\"properties\": {\"dc:title\":\"My title\"}}";

        ClientResponse response = getResponse(RequestType.POST, "path/", data);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Then the create document is returned
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("My title", node.get("title").getValueAsText());
        String id = node.get("uid").getValueAsText();
        assertTrue(StringUtils.isNotBlank(id));

        // Then a document is created in the database
        dispose(session);
        DocumentModel doc = session.getDocument(new IdRef(id));
        assertEquals("My title", doc.getTitle());
        assertEquals("File", doc.getType());

    }

    @Test
    public void iCanDeleteADocument() throws Exception {
        // Given a document
        DocumentModel doc = RestServerInit.getNote(0, session);

        // When I do a DELETE request
        ClientResponse response = getResponse(RequestType.DELETE, "path" + doc.getPathAsString());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(),
                response.getStatus());

        dispose(session);
        // Then the doc is deleted
        assertTrue(!session.exists(doc.getRef()));

    }

}
