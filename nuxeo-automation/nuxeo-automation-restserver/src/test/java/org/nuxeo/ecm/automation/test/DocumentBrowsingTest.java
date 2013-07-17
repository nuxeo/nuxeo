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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Iterator;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.AutomationServerComponent;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * Test the CRUD rest API
 *
 * @since 5.7.2
 */

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy("nuxeo-automation-restserver")
@LocalDeploy({ "nuxeo-automation-restserver:adapter-contrib.xml" })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentBrowsingTest {

    private static enum RequestType {
        GET, POST, DELETE, PUT
    }

    private WebResource service;

    @Inject
    CoreSession session;

    private ObjectMapper mapper;

    @BeforeClass
    public static void setupCodecs() throws Exception {
        // Fire application start on AutomationServer component forcing to load
        // correctly Document Adapter Codec in Test scope (to take into account
        // of document adapters contributed into test) -> see execution order
        // here: org.nuxeo.runtime.test.runner.RuntimeFeature.start()

        ComponentInstance componentInstance = Framework.getRuntime().getComponentInstance(
                "org.nuxeo.ecm.automation.server.AutomationServer");
        AutomationServerComponent automationServerComponent = (AutomationServerComponent) componentInstance.getInstance();
        automationServerComponent.applicationStarted(componentInstance);
    }

    @Before
    public void doBefore() {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter("Administrator",
                "Administrator"));
        service = client.resource("http://localhost:18090/api/");

        mapper = new ObjectMapper();

    }

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
        response = getResponse(RequestType.POST, "id/" + note.getId(),
                jsonDoc.asJson());

        response = service.path("id/" + note.getId()).header("Content-type",
                "application/json+nxentity").put(ClientResponse.class,
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

        ClientResponse response = service.path("path/").header("Content-type",
                "application/json+nxentity").post(ClientResponse.class, data);
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
        ClientResponse response = service.path("path/").header("Content-type",
                "application/json+nxentity").delete(ClientResponse.class);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(),
                response.getStatus());

        dispose(session);
        // Then the doc is deleted
        assertTrue(!session.exists(doc.getRef()));

    }

    private ClientResponse getResponse(RequestType requestType, String path) {
        return getResponse(requestType, path, null);
    }

    private ClientResponse getResponse(RequestType requestType, String path,
            String data) {
        Builder builder = service.path(path) //
        .accept(MediaType.APPLICATION_JSON) //
        .header("X-NXDocumentProperties", "dublincore")//
        .header("Content-type", "application/json+nxentity"); //

        switch (requestType) {
        case GET:
            return builder.get(ClientResponse.class);
        case POST:
            return builder.post(ClientResponse.class, data);
        case PUT:
            return builder.put(ClientResponse.class, data);
        case DELETE:
            return builder.delete(ClientResponse.class);
        default:
            throw new RuntimeException();
        }
    }

    protected void dispose(CoreSession session) throws Exception {
        if (Proxy.isProxyClass(session.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(session);
            if (handler instanceof TransactionalCoreSessionWrapper) {
                Field field = TransactionalCoreSessionWrapper.class.getDeclaredField("session");
                field.setAccessible(true);
                session = (CoreSession) field.get(handler);
            }
        }
        if (!(session instanceof LocalSession)) {
            throw new UnsupportedOperationException(
                    "Cannot dispose session of class " + session.getClass());
        }
        ((LocalSession) session).getSession().dispose();
    }

    private void assertNodeEqualsDoc(JsonNode node, DocumentModel note)
            throws Exception {
        assertEquals("document", node.get("entity-type").getValueAsText());
        assertEquals(note.getPathAsString(), node.get("path").getValueAsText());
        assertEquals(note.getId(), node.get("uid").getValueAsText());
        assertEquals(note.getTitle(), node.get("title").getValueAsText());
    }

    private void assertEntityEqualsDoc(InputStream in, DocumentModel doc)
            throws Exception {

        JsonNode node = mapper.readTree(in);
        assertNodeEqualsDoc(node, doc);

    }

}
