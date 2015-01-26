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
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.jaxrs.io.directory.DirectoryEntriesWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.directory.DirectoryEntryWriter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.ClientResponse;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:test-directory-contrib.xml")
public class DirectoryTest extends BaseTest {

    @Inject
    DirectoryService ds;

    /**
    *
    */
    private static final String TESTDIRNAME = "testdir";

    Session dirSession = null;

    @Override
    @Before
    public void doBefore() throws Exception {
        super.doBefore();
        dirSession = ds.open(TESTDIRNAME);
    }

    @After
    public void doAfter() throws Exception {
        if (dirSession != null) {
            dirSession.close();
        }
    }

    @Test
    public void itCanQueryDirectoryEntry() throws Exception {
        // Given a directoryEntry
        DocumentModel docEntry = dirSession.getEntry("test1");
        // When I call the Rest endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + TESTDIRNAME + "/test1");

        assertEquals(DirectoryEntryWriter.ENTITY_TYPE, node.get("entity-type").getValueAsText());
        assertEquals(TESTDIRNAME, node.get("directoryName").getValueAsText());
        assertEquals(docEntry.getPropertyValue("vocabulary:label"),
                node.get("properties").get("label").getValueAsText());

    }

    @Test
    public void itCanQueryDirectoryEntries() throws Exception {
        // Given a directory
        DocumentModelList entries = dirSession.getEntries();

        // When i do a request on the directory endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + TESTDIRNAME);

        // Then i receive the response as json
        assertEquals(DirectoryEntriesWriter.ENTITY_TYPE, node.get("entity-type").getValueAsText());
        ArrayNode jsonEntries = (ArrayNode) node.get("entries");
        assertEquals(entries.size(), jsonEntries.size());

    }

    @Test
    public void itCanUpdateADirectoryEntry() throws Exception {
        // Given a directory modified entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        docEntry.setPropertyValue("vocabulary:label", "newlabel");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        ClientResponse response = getResponse(RequestType.PUT, "/directory/" + TESTDIRNAME + "/" + docEntry.getId(),
                jsonEntry);

        // Then the entry is updated
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        docEntry = dirSession.getEntry("test1");
        assertEquals("newlabel", docEntry.getPropertyValue("vocabulary:label"));

    }

    @Test
    public void itCanCreateADirectoryEntry() throws Exception {
        // Given a directory modified entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        docEntry.setPropertyValue("vocabulary:id", "newtest");
        docEntry.setPropertyValue("vocabulary:label", "newlabel");
        assertNull(dirSession.getEntry("newtest"));
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        ClientResponse response = getResponse(RequestType.POST, "/directory/" + TESTDIRNAME, jsonEntry);

        // Then the entry is updated
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        docEntry = dirSession.getEntry("newtest");
        assertEquals("newlabel", docEntry.getPropertyValue("vocabulary:label"));

    }

    @Test
    public void itCanDeleteADirectoryEntry() throws Exception {
        // Given an existent entry
        DocumentModel docEntry = dirSession.getEntry("test2");
        assertNotNull(docEntry);

        // When i do a DELETE request on the entry endpoint
        getResponse(RequestType.DELETE, "/directory/" + TESTDIRNAME + "/" + docEntry.getId());

        // Then the entry is deleted
        assertNull(dirSession.getEntry("test2"));

    }

    @Test
    public void itSends404OnnotExistentDirectory() throws Exception {
        ClientResponse response = getResponse(RequestType.GET, "/directory/nonexistendirectory");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void itSends404OnnotExistentDirectoryEntry() throws Exception {
        ClientResponse response = getResponse(RequestType.GET, "/directory/" + TESTDIRNAME + "/nonexistententry");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void genericUserCanNotEditDirectories() throws Exception {
        // As a generic user
        service = getServiceFor("user1", "user1");

        // Given a directory entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        ClientResponse response = getResponse(RequestType.PUT, "/directory/" + TESTDIRNAME + "/" + docEntry.getId(),
                jsonEntry);
        // Then it is unauthorized
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        // When i do an create request on the entry endpoint
        response = getResponse(RequestType.POST, "/directory/" + TESTDIRNAME, jsonEntry);
        // Then it is unauthorized
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        // When i do an delete request on the entry endpoint
        response = getResponse(RequestType.DELETE, "/directory/" + TESTDIRNAME + "/" + docEntry.getId());
        // Then it is unauthorized
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    }

    @Test
    public void userDirectoryAreNotEditable() throws Exception {

        // Given a user directory entry
        UserManager um = Framework.getLocalService(UserManager.class);
        DocumentModel model = um.getUserModel("user1");
        String userDirectoryName = um.getUserDirectoryName();
        String jsonEntry = getDirectoryEntryAsJson(userDirectoryName, model);

        // When i do an update request on it
        ClientResponse response = getResponse(RequestType.POST, "/directory/" + userDirectoryName, jsonEntry);
        // Then it is unauthorized
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    }

    @Test
    public void itShouldNotWritePasswordFieldInResponse() throws Exception {
        // Given a user directory entry
        UserManager um = Framework.getLocalService(UserManager.class);
        String userDirectoryName = um.getUserDirectoryName();

        // When i do an update request on it
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + userDirectoryName + "/user1");

        assertEquals("", node.get("properties").get("password").getValueAsText());

    }

    @Test
    public void groupDirectoryAreNotEditable() throws Exception {

        // Given a user directory entry
        UserManager um = Framework.getLocalService(UserManager.class);
        DocumentModel model = um.getGroupModel("group1");
        String groupDirectoryName = um.getGroupDirectoryName();
        String jsonEntry = getDirectoryEntryAsJson(groupDirectoryName, model);

        // When i do an create request on it
        ClientResponse response = getResponse(RequestType.POST, "/directory/" + groupDirectoryName, jsonEntry);
        // Then it is unauthorized
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    }

    private String getDirectoryEntryAsJson(DocumentModel dirEntry) throws IOException, JsonGenerationException,
            ClientException {
        return getDirectoryEntryAsJson(TESTDIRNAME, dirEntry);
    }

    private String getDirectoryEntryAsJson(String dirName, DocumentModel dirEntry) throws IOException,
            JsonGenerationException, ClientException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
        DirectoryEntryWriter dew = new DirectoryEntryWriter();
        dew.writeEntity(jg, new DirectoryEntry(dirName, dirEntry));
        return out.toString();
    }

}
