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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.io.services.enricher.ACLContentEnricher;
import org.nuxeo.ecm.automation.io.services.enricher.ContentEnricherServiceImpl;
import org.nuxeo.ecm.automation.io.services.enricher.PreviewContentEnricher;
import org.nuxeo.ecm.automation.io.services.enricher.ThumbnailContentEnricher;
import org.nuxeo.ecm.automation.io.services.enricher.UserPermissionsContentEnricher;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.restapi.jaxrs.io.documents.ACPWriter;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.DocumentModelJsonReaderLegacy;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

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
@Deploy({ "org.nuxeo.ecm.platform.ui:OSGI-INF/marshallers-contrib.xml",
        "org.nuxeo.ecm.platform.preview:OSGI-INF/marshallers-contrib.xml" })
public class DocumentBrowsingTest extends BaseTest {

    @Test
    public void iCanBrowseTheRepoByItsPath() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request
        ClientResponse response = getResponse(RequestType.GET, "path" + note.getPathAsString());

        // Then i get a document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEntityEqualsDoc(response.getEntityInputStream(), note);

    }

    @Test
    public void iCanBrowseTheRepoByItsId() throws Exception {
        // Given a document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request
        ClientResponse response = getResponse(RequestType.GET, "id/" + note.getId());

        // The i get the document as Json
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEntityEqualsDoc(response.getEntityInputStream(), note);

    }

    @Test
    public void iCanGetTheChildrenOfADoc() throws Exception {
        // Given a folder with one document
        DocumentModel folder = RestServerInit.getFolder(0, session);
        DocumentModel child = session.createDocumentModel(folder.getPathAsString(), "note", "Note");
        child = session.createDocument(child);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // When i call a GET on the children for that doc
        ClientResponse response = getResponse(RequestType.GET, "id/" + folder.getId() + "/@children");

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
        ClientResponse response = getResponse(RequestType.GET, "id/" + note.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // When i do a PUT request on the document with modified data
        JSONDocumentNode jsonDoc = new JSONDocumentNode(response.getEntityInputStream());
        jsonDoc.setPropertyValue("dc:title", "New title");
        response = getResponse(RequestType.PUT, "id/" + note.getId(), jsonDoc.asJson());

        // Then the document is updated
        fetchInvalidations();
        note = RestServerInit.getNote(0, session);
        assertEquals("New title", note.getTitle());

    }

    @Test
    public void iCanUpdateDocumentVersion() throws Exception {
        // Given a document
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(RequestType.GET, "id/" + note.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Check the current version of the live document
        assertEquals("0.0", note.getVersionLabel());

        // When i do a PUT request on the document with modified version in the header
        JSONDocumentNode jsonDoc = new JSONDocumentNode(response.getEntityInputStream());
        Map<String, String> headers = new HashMap<>();
        headers.put(RestConstants.X_VERSIONING_OPTION, VersioningOption.MAJOR.toString());
        headers.put(HEADER_PREFIX + FETCH_PROPERTIES + "." + ENTITY_TYPE, "versionLabel");
        response = getResponse(RequestType.PUT, "id/" + note.getId(), jsonDoc.asJson(), headers);

        // Check if the version of the document has been returned
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("1.0", node.get("versionLabel").getValueAsText());

        // Check if the original document is still not versioned.
        note = RestServerInit.getNote(0, session);
        assertEquals("0.0", note.getVersionLabel());
    }

    @Test
    public void itCanUpdateADocumentWithoutSpecifyingIdInJSONPayload() throws Exception {
        // Given a document
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(RequestType.GET, "path" + note.getPathAsString());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // When i do a PUT request on the document with modified data
        response = getResponse(RequestType.PUT, "id/" + note.getId(),
                "{\"entity-type\":\"document\",\"properties\":{\"dc:title\":\"Other New title\"}}");

        // Then the document is updated
        fetchInvalidations();
        note = RestServerInit.getNote(0, session);
        assertEquals("Other New title", note.getTitle());

    }

    @Test
    public void itCanSetPropertyToNull() throws Exception {
        DocumentModel note = RestServerInit.getNote(0, session);
        note.setPropertyValue("dc:format", "a value that will be set to null");
        note.setPropertyValue("dc:language", "a value that that must not be resetted");
        session.saveDocument(note);

        fetchInvalidations();

        // When i do a PUT request on the document with modified data
        getResponse(RequestType.PUT, "id/" + note.getId(),
                "{\"entity-type\":\"document\",\"properties\":{\"dc:format\":null}}");

        // Then the document is updated
        fetchInvalidations();
        note = RestServerInit.getNote(0, session);
        assertEquals(null, note.getPropertyValue("dc:format"));
        assertEquals("a value that that must not be resetted", note.getPropertyValue("dc:language"));

    }

    @Test
    public void itCanSetPropertyToNullNewModeKeepEmpty() throws Exception {
        DocumentModel note = RestServerInit.getNote(0, session);
        note.setPropertyValue("dc:format", "a value that will be set to null");
        session.saveDocument(note);

        fetchInvalidations();

        // When i do a PUT request on the document with modified data
        getResponse(RequestType.PUT, "id/" + note.getId(),
                "{\"entity-type\":\"document\",\"properties\":{\"dc:format\":\"\"}}");

        // Then the document is updated
        fetchInvalidations();
        note = RestServerInit.getNote(0, session);
        assertEquals("", note.getPropertyValue("dc:format"));
    }

    @Test
    public void itCanSetPropertyToNullLegacyModeHeader() throws Exception {
        DocumentModel note = RestServerInit.getNote(0, session);
        note.setPropertyValue("dc:format", "a value that will be set to null");
        session.saveDocument(note);

        fetchInvalidations();

        // When i do a PUT request on the document with modified data
        Map<String, String> headers = new HashMap<>();
        headers.put(DocumentModelJsonReaderLegacy.HEADER_DOCUMENT_JSON_LEGACY, Boolean.TRUE.toString());
        getResponse(RequestType.PUT, "id/" + note.getId(),
                "{\"entity-type\":\"document\",\"properties\":{\"dc:format\":\"\"}}", headers);

        // Then the document is updated
        fetchInvalidations();
        note = RestServerInit.getNote(0, session);
        assertEquals(null, note.getPropertyValue("dc:format"));
    }

    @Test
    public void iCanCreateADocument() throws Exception {

        // Given a folder and a Rest Creation request
        DocumentModel folder = RestServerInit.getFolder(0, session);

        String data = "{\"entity-type\": \"document\",\"type\": \"File\",\"name\":\"newName\",\"properties\": {\"dc:title\":\"My title\",\"dc:description\":\" \"}}";

        ClientResponse response = getResponse(RequestType.POST, "path" + folder.getPathAsString(), data);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // Then the create document is returned
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("My title", node.get("title").getValueAsText());
        assertEquals(" ", node.get("properties").get("dc:description").getTextValue());
        String id = node.get("uid").getValueAsText();
        assertTrue(StringUtils.isNotBlank(id));

        // Then a document is created in the database
        fetchInvalidations();
        DocumentModel doc = session.getDocument(new IdRef(id));
        assertEquals(folder.getPathAsString() + "/newName", doc.getPathAsString());
        assertEquals("My title", doc.getTitle());
        assertEquals("File", doc.getType());

    }

    @Test
    public void iCanDeleteADocument() throws Exception {
        // Given a document
        DocumentModel folder = RestServerInit.getFolder(1, session);
        DocumentModel doc = RestServerInit.getNote(0, session);

        // When I do a DELETE request
        ClientResponse response = getResponse(RequestType.DELETE, "path" + doc.getPathAsString());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        fetchInvalidations();
        // Then the doc is deleted
        assertTrue(!session.exists(doc.getRef()));

    }

    @Test
    public void iCanChooseAnotherRepositoryName() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString());

        // Then i get a document
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEntityEqualsDoc(response.getEntityInputStream(), note);

        // When i do a GET Request on a non existent repository
        response = getResponse(RequestType.GET, "repo/nonexistentrepo/path" + note.getPathAsString());

        // Then i receive a 404
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

    }

    @Test
    public void iCanGetTheACLsOnADocumentThroughAdapter() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString() + "/@acl");

        // Then i get a the ACL
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(ACPWriter.ENTITY_TYPE, node.get("entity-type").getValueAsText());

    }

    @Test
    public void iCanGetTheACLsOnADocumentThroughContributor() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);
        Map<String, String> headers = new HashMap<>();
        headers.put(ContentEnricherServiceImpl.NXCONTENT_CATEGORY_HEADER, ACLContentEnricher.ACLS_CONTENT_ID);

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // Then i get a the ACL
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("inherited",
                node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get("acls").get(0).get("name").getTextValue());

    }

    @Test
    public void iCanGetTheThumbnailOfADocumentThroughContributor() throws Exception {
        // TODO NXP-14793: Improve testing by adding thumbnail conversion
        // Attach a blob
        // Blob blob = new InputStreamBlob(DocumentBrowsingTest.class.getResource(
        // "/test-data/png.png").openStream(), "image/png",
        // null, "logo.png", null);
        // DocumentModel file = RestServerInit.getFile(0, session);
        // file.setPropertyValue("file:content", (Serializable) blob);
        // file = session.saveDocument(file);
        // session.save();
        // ClientResponse response = getResponse(
        // RequestType.GET,
        // "repo/" + file.getRepositoryName() + "/path"
        // + file.getPathAsString(), headers);
        // Then i get an entry for thumbnail
        // assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // JsonNode node = mapper.readTree(response.getEntityInputStream());
        // assertEquals("specificUrl", node.get(RestConstants
        // .CONTRIBUTOR_CTX_PARAMETERS).get("thumbnail").get
        // ("thumbnailUrl").getTextValue());

        Map<String, String> headers = new HashMap<>();
        headers.put(ContentEnricherServiceImpl.NXCONTENT_CATEGORY_HEADER, ThumbnailContentEnricher.THUMBNAIL_CONTENT_ID);

        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request on the note without any image
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // Then i get no result for valid thumbnail url as expected but still
        // thumbnail entry from the contributor
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(null,
                node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get("thumbnail").get("url").getTextValue());
    }

    @Test
    public void iCanGetThePermissionsOnADocumentThroughContributor() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);
        Map<String, String> headers = new HashMap<>();
        headers.put(ContentEnricherServiceImpl.NXCONTENT_CATEGORY_HEADER,
                UserPermissionsContentEnricher.PERMISSIONS_CONTENT_ID);

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // Then i get a list of permissions
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        JsonNode permissions = node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get("permissions");
        assertNotNull(permissions);
        assertTrue(permissions.isArray());
    }

    @Test
    public void iCanGetThePreviewURLThroughContributor() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);
        Map<String, String> headers = new HashMap<>();
        headers.put(ContentEnricherServiceImpl.NXCONTENT_CATEGORY_HEADER, PreviewContentEnricher.PREVIEW_CONTENT_ID);

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // Then i get a preview url
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        JsonNode preview = node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get("preview");
        assertNotNull(preview);
        StringUtils.endsWith(preview.get(PreviewContentEnricher.PREVIEW_URL_LABEL).getTextValue(), "/default/");
    }

    @Test
    public void itCanBrowseDocumentWithSpacesInPath() throws Exception {
        DocumentModel folder = RestServerInit.getFolder(0, session);
        DocumentModel note = session.createDocumentModel(folder.getPathAsString(), "doc with space", "Note");
        note = session.createDocument(note);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET, "repo/" + note.getRepositoryName() + "/path"
                + note.getPathAsString().replace(" ", "%20"));

        // Then i get a the ACL
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // When i do a GET Request on the note repository
        response = getResponse(RequestType.GET, "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString());

        // Then i get a the ACL
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

    @Test
    public void itCanModifyArrayTypes() throws Exception {
        // Given a document
        DocumentModel note = RestServerInit.getNote(0, session);
        ClientResponse response = getResponse(RequestType.GET, "id/" + note.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // When i do a PUT request on the document with modified data
        JSONDocumentNode jsonDoc = new JSONDocumentNode(response.getEntityInputStream());
        jsonDoc.setPropertyValue("dc:title", "New title");
        jsonDoc.setPropertyArray("dc:contributors", "system");
        response = getResponse(RequestType.PUT, "id/" + note.getId(), jsonDoc.asJson());

        // Then the document is updated
        fetchInvalidations();
        note = RestServerInit.getNote(0, session);
        assertEquals("New title", note.getTitle());

        List<String> contributors = Arrays.asList((String[]) note.getPropertyValue("dc:contributors"));
        assertTrue(contributors.contains("system"));
        assertTrue(contributors.contains("Administrator"));
        assertEquals(2, contributors.size());
    }

}
