/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.HEADER_PREFIX;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.collections.core.io.CollectionsJsonEnricher;
import org.nuxeo.ecm.collections.core.io.FavoritesJsonEnricher;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.io.marshallers.json.document.ACPJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.BasePermissionsJsonEnricher;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.permissions.ACLJsonEnricher;
import org.nuxeo.ecm.platform.preview.io.PreviewJsonEnricher;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.tag.io.TagsJsonEnricher;
import org.nuxeo.ecm.platform.thumbnail.io.ThumbnailJsonEnricher;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.DocumentModelJsonReaderLegacy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Test the CRUD rest API
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail:OSGI-INF/marshallers-contrib.xml",
        "org.nuxeo.ecm.platform.preview:OSGI-INF/marshallers-contrib.xml",
        "org.nuxeo.ecm.permissions:OSGI-INF/marshallers-contrib.xml",
        "org.nuxeo.ecm.platform.collections.core", "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.userworkspace.types"})
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
    public void iCanUpdateAFileDocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        Blob blob = new StringBlob("test");
        blob.setFilename("test.txt");
        doc.setProperty("file", "content", blob);
        doc.setPropertyValue("dc:title", "my Title");
        doc = session.createDocument(doc);
        fetchInvalidations();

        ClientResponse response = getResponse(RequestType.GET, "id/" + doc.getId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String payload = "{  " +
                "         \"entity-type\": \"document\"," +
                "         \"name\": \"myFile\"," +
                "         \"type\": \"File\"," +
                "         \"state\": \"project\"," +
                "         \"title\": \"New title\"," +
                "         \"properties\": {" +
                "             \"dc:description\":\"blabla\"," +
                "             \"dc:title\":\"New title\"" +
                "         }" +
                "     }";



        response = getResponse(RequestType.PUT, "id/" + doc.getId(), payload);

        // Then the document is updated
        fetchInvalidations();

        doc = session.getDocument(new IdRef(doc.getId()));
        assertEquals("New title", doc.getTitle());
        Blob value = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(value);
        assertEquals("test.txt", value.getFilename());

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
        Serializable value = note.getPropertyValue("dc:format");
        if (!"".equals(value)) {
            // will be NULL for Oracle, where empty string and NULL are the same thing
            assertNull(value);
        }
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
        DocumentModel doc = RestServerInit.getNote(0, session);

        // When I do a DELETE request
        ClientResponse response = getResponse(RequestType.DELETE, "path" + doc.getPathAsString());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        fetchInvalidations();
        // Then the doc is deleted
        assertTrue(!session.exists(doc.getRef()));

    }

    @Test
    public void iCanDeleteADocumentWithoutHeaders() throws Exception {
        // Given a document
        DocumentModel doc = RestServerInit.getNote(0, session);

        // When I do a DELETE request
        WebResource wr = service.path("path" + doc.getPathAsString());
        ClientResponse response = wr.delete(ClientResponse.class);
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
        assertEquals(ACPJsonWriter.ENTITY_TYPE, node.get("entity-type").getValueAsText());

    }

    @Test
    public void iCanGetTheACLsOnADocumentThroughContributor() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);
        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", ACLJsonEnricher.NAME);

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
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", ThumbnailJsonEnricher.NAME);

        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);

        // When i do a GET Request on the note without any image
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // Then i get no result for valid thumbnail url as expected but still
        // thumbnail entry from the contributor
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertNotNull(node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get("thumbnail").get("url").getTextValue());
    }

    /**
     * @since 8.1
     */
    @Test
    public void iCanGetIsADocumentFavorite() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", FavoritesJsonEnricher.NAME);

        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // The above GET will force the creation of the user workspace if it did not exist yet.
        // Force to refresh current transaction context.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertFalse(node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(FavoritesJsonEnricher.NAME).get(FavoritesJsonEnricher.IS_FAVORITE).getBooleanValue());

        FavoritesManager favoritesManager = Framework.getService(FavoritesManager.class);
        favoritesManager.addToFavorites(note, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertTrue(node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(FavoritesJsonEnricher.NAME).get(FavoritesJsonEnricher.IS_FAVORITE).getBooleanValue());
    }

    /**
     * @since 8.3
     */
    @Test
    public void iCanGetDocumentTags() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", TagsJsonEnricher.NAME);

        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // The above GET will force the creation of the user workspace if it did not exist yet.
        // Force to refresh current transaction context.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(0, node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(TagsJsonEnricher.NAME).size());

        TagService tagService = Framework.getService(TagService.class);
        tagService.tag(session, note.getId(), "pouet", session.getPrincipal().getName());

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        response = getResponse(RequestType.GET, "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(),
                headers);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(TagsJsonEnricher.NAME).size());
        assertEquals("pouet", node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(TagsJsonEnricher.NAME).get(0).get(
                "label").getTextValue());
    }

    /**
     * @since 8.3
     */
    @Test
    public void iCanGetTheCollectionsOfADocument() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", CollectionsJsonEnricher.NAME);

        DocumentModel note = RestServerInit.getNote(0, session);

        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // The above GET will force the creation of the user workspace if it did not exist yet.
        // Force to refresh current transaction context.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(0, ((ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(CollectionsJsonEnricher.NAME)).size());
        CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        collectionManager.addToNewCollection("dummyCollection", null, note, session);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        ArrayNode collections = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get(CollectionsJsonEnricher.NAME);
        assertEquals(1, collections.size());
        assertEquals("dummyCollection", collections.get(0).get("title").getTextValue());
    }

    @Test
    public void iCanGetThePermissionsOnADocumentThroughContributor() throws Exception {
        // Given an existing document
        DocumentModel note = RestServerInit.getNote(0, session);
        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", BasePermissionsJsonEnricher.NAME);

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
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", PreviewJsonEnricher.NAME);

        // When i do a GET Request on the note repository
        ClientResponse response = getResponse(RequestType.GET,
                "repo/" + note.getRepositoryName() + "/path" + note.getPathAsString(), headers);

        // Then i get a preview url
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        JsonNode preview = node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS).get("preview");
        assertNotNull(preview);
        StringUtils.endsWith(preview.get("url").getTextValue(), "/default/");
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
