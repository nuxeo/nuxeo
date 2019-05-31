/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.Blobs.createBlob;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test blobs update through the REST API.
 *
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:multiblob-doctype.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentUpdateBlobsTest extends BaseTest {

    protected static final String FILE_CONTENT_PROP = "file:content";

    protected static final String FILES_FILES_PROP = "files:files";

    protected static final String MULTIBLOB_BLOBS_PROP = "mb:blobs";

    protected static final Map<String, String> HEADERS = Map.of("X-NXDocumentProperties", "*");

    @Inject
    protected TransactionalFeature transactionalFeature;

    protected String file1Id;

    protected String file2Id;

    protected String file3Id;

    protected String file4Id;

    /**
     * Creates several documents holding blobs referenced on generic schemas, such as {@code file} / {@code files}, or
     * often seen schema, such as {@code multiblobs}.
     * <ul>
     * <li>file: simple blob type</li>
     * <li>files: list of simple blob type</li>
     * <li>multiblobs: list of complex having a simple blob type + a scalar type</li>
     * </ul>
     */
    @Before
    public void setup() {
        DocumentModel doc = createDocument(1, "foo", true, false, false);
        file1Id = doc.getId();
        doc = createDocument(2, "bar", false, false, false);
        file2Id = doc.getId();
        doc = createDocument(3, "foobar", true, true, false);
        file3Id = doc.getId();
        doc = createDocument(4, "barfoo", false, false, true);
        file4Id = doc.getId();
        transactionalFeature.nextTransaction();
    }

    protected DocumentModel createDocument(int index, String blobContent, boolean addPermission, boolean addAttachments,
            boolean addMultiBlobs) {
        DocumentModel doc = session.createDocumentModel("/folder_2", "file" + index, "MultiBlobDoc");
        doc.setPropertyValue("dc:title", "MultiBlobDoc" + index);
        Blob blob = createBlob(blobContent);
        doc.setPropertyValue(FILE_CONTENT_PROP, (Serializable) blob);

        if (addAttachments) {
            List<Map<String, Serializable>> attachments = Stream.of(createBlob("one"), createBlob("two"),
                    createBlob("three")).map(b -> Map.of("file", (Serializable) b)).collect(Collectors.toList());
            doc.setPropertyValue(FILES_FILES_PROP, (Serializable) attachments);
        }

        if (addMultiBlobs) {
            List<Map<String, Serializable>> multiBlobs = Stream.of("four", "five", "six")
                                                               .map(c -> Map.of("content", (Serializable) createBlob(c),
                                                                       "filename", c + ".txt"))
                                                               .collect(Collectors.toList());
            doc.setPropertyValue(MULTIBLOB_BLOBS_PROP, (Serializable) multiBlobs);
        }

        doc = session.createDocument(doc);

        if (addPermission) {
            ACP acp = doc.getACP();
            ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
            acl.add(new ACE("user1", SecurityConstants.READ_WRITE, true));
            doc.setACP(acp, true);
        }

        return doc;
    }

    @Test
    public void shouldKeepTheSameBlob() throws IOException {
        JSONDocumentNode jsonDoc = getJSONDocumentNode(file1Id);
        assertNotNull(jsonDoc.getPropertyAsJsonNode(FILE_CONTENT_PROP));
        // PUT the same JSON document, with a file:content property
        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonDoc)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            assertBlobContent(file1Id, "foo");
        }
    }

    @Test
    public void shouldNotRemoveTheBlob() throws IOException {
        JSONDocumentNode jsonDoc = getJSONDocumentNode(file1Id);
        assertNotNull(jsonDoc.getPropertyAsJsonNode(FILE_CONTENT_PROP));
        // PUT the same JSON document, without a file:content property
        jsonDoc.removePropertyValue(FILE_CONTENT_PROP);
        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonDoc)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            assertBlobContent(file1Id, "foo");
        }
    }

    @Test
    public void shouldRemoveTheBlob() throws IOException {
        JSONDocumentNode jsonDoc = getJSONDocumentNode(file1Id);
        assertNotNull(jsonDoc.getPropertyAsJsonNode(FILE_CONTENT_PROP));
        // PUT the same JSON document, with a null file:content property
        jsonDoc.setPropertyValue(FILE_CONTENT_PROP, NullNode.instance);
        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonDoc)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure blob has been removed
            transactionalFeature.nextTransaction();
            assertBlobNull(file1Id);
        }
    }

    @Test
    public void shouldUpdateBlobWithAnotherDocumentBlob() throws IOException {
        assertBlobContent(file1Id, "foo");
        assertBlobContent(file2Id, "bar");

        JSONDocumentNode jsonFile1 = getJSONDocumentNode(file1Id);
        JSONDocumentNode jsonFile2 = getJSONDocumentNode(file2Id);

        // replace file1 'file:content' with file2 'file:content'
        jsonFile1.setPropertyValue(FILE_CONTENT_PROP, jsonFile2.getPropertyAsJsonNode(FILE_CONTENT_PROP));

        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonFile1)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure file1 blob has changed
            transactionalFeature.nextTransaction();
            assertBlobContent(file1Id, "bar");
            assertBlobContent(file2Id, "bar");
        }
    }

    @Test
    public void shouldFailWhenDocumentDoesNotExist() throws IOException {
        assertBlobContent(file1Id, "foo");
        assertBlobContent(file2Id, "bar");

        JSONDocumentNode jsonFile1 = getJSONDocumentNode(file1Id);
        JSONDocumentNode jsonFile2 = getJSONDocumentNode(file2Id);

        // replace file1 'file:content' with file2 'file:content'
        jsonFile1.setPropertyValue(FILE_CONTENT_PROP, jsonFile2.getPropertyAsJsonNode(FILE_CONTENT_PROP));

        // switch to user1 who does not have access to file2
        service = getServiceFor("user1", "user1");

        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonFile1)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            assertBlobContent(file1Id, "foo");
            assertBlobContent(file2Id, "bar");
        }
    }

    @Test
    public void shouldFailWhenBlobDoesNotExist() throws IOException {
        assertBlobContent(file1Id, "foo");
        assertBlobContent(file2Id, "bar");

        JSONDocumentNode jsonFile1 = getJSONDocumentNode(file1Id);
        JSONDocumentNode jsonFile2 = getJSONDocumentNode(file2Id);

        // remove file2 blob
        removeBlob(file2Id);

        // reference the blob 'file:content' from file2 that does not exist anymore
        jsonFile1.setPropertyValue(FILE_CONTENT_PROP, jsonFile2.getPropertyAsJsonNode(FILE_CONTENT_PROP));

        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonFile1)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            // ensure file1 blob has not changed
            transactionalFeature.nextTransaction();
            assertBlobContent(file1Id, "foo");
            assertBlobNull(file2Id);
        }
    }

    @Test
    public void shouldUpdateBlobList() throws IOException {
        List<Map<String, Serializable>> attachments = getAttachments(file3Id);
        assertEquals(3, attachments.size());
        assertAttachmentContent(attachments, 2, "three");

        JSONDocumentNode jsonDoc = getJSONDocumentNode(file3Id);

        // keep the last attachment
        jsonDoc = removeAttachment(jsonDoc, 0);
        jsonDoc = removeAttachment(jsonDoc, 0);

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "id/" + file3Id, jsonDoc.asJson())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure file3 has only the last attachment
            transactionalFeature.nextTransaction();
            attachments = getAttachments(file3Id);
            assertEquals(1, attachments.size());
            assertAttachmentContent(attachments, 0, "three");
        }
    }

    @Test
    public void shouldUpdateBlobListWithBlobFromAnotherDocument() throws IOException {
        List<Map<String, Serializable>> attachments = getAttachments(file3Id);
        assertEquals(3, attachments.size());

        JSONDocumentNode jsonFile1 = getJSONDocumentNode(file1Id);
        JSONDocumentNode jsonFile3 = getJSONDocumentNode(file3Id);

        // make the first attachment references file1 'file:content'
        jsonFile3 = replaceAttachment(jsonFile3, 0, jsonFile1.getPropertyAsJsonNode(FILE_CONTENT_PROP));
        // remove the last one
        jsonFile3 = removeAttachment(jsonFile3, 2);

        try (CloseableClientResponse response = putJSONDocument(file3Id, jsonFile3)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            transactionalFeature.nextTransaction();
            // ensure file3 attachments has changed
            attachments = getAttachments(file3Id);
            assertEquals(2, attachments.size());
            assertAttachmentContent(attachments, 0, "foo");
            assertAttachmentContent(attachments, 1, "two");
        }
    }

    @Test
    public void shouldFailUpdatingBlobListWhenDocumentDoesNotExist() throws IOException {
        List<Map<String, Serializable>> attachments = getAttachments(file3Id);
        assertEquals(3, attachments.size());

        JSONDocumentNode jsonFile2 = getJSONDocumentNode(file2Id);
        JSONDocumentNode jsonFile3 = getJSONDocumentNode(file3Id);

        // make the first attachment references file2 'file:content'
        jsonFile3 = replaceAttachment(jsonFile3, 1, jsonFile2.getPropertyAsJsonNode(FILE_CONTENT_PROP));

        // switch to user1 who does not have access to file2
        service = getServiceFor("user1", "user1");

        try (CloseableClientResponse response = putJSONDocument(file3Id, jsonFile3)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            transactionalFeature.nextTransaction();
            // ensure nothing has changed
            attachments = getAttachments(file3Id);
            assertEquals(3, attachments.size());
            assertAttachmentContent(attachments, 0, "one");
            assertAttachmentContent(attachments, 1, "two");
            assertAttachmentContent(attachments, 2, "three");
        }
    }

    @Test
    public void shouldFailUpdatingBlobListWhenBlobDoesNotExist() throws IOException {
        List<Map<String, Serializable>> attachments = getAttachments(file3Id);
        assertEquals(3, attachments.size());

        JSONDocumentNode jsonFile1 = getJSONDocumentNode(file1Id);
        JSONDocumentNode jsonFile3 = getJSONDocumentNode(file3Id);

        // remove file1 blob
        removeBlob(file1Id);

        // make the first attachment references the blob 'file:content' from file1 that does not exist anymore
        jsonFile3 = replaceAttachment(jsonFile3, 2, jsonFile1.getPropertyAsJsonNode(FILE_CONTENT_PROP));

        try (CloseableClientResponse response = putJSONDocument(file3Id, jsonFile3)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            transactionalFeature.nextTransaction();
            // ensure nothing has changed
            attachments = getAttachments(file3Id);
            assertEquals(3, attachments.size());
            assertAttachmentContent(attachments, 0, "one");
            assertAttachmentContent(attachments, 1, "two");
            assertAttachmentContent(attachments, 2, "three");
        }
    }

    @Test
    public void shouldKeepTheSameBlobWhenUpdatingSiblingInComplex() throws IOException {
        List<Map<String, Serializable>> multiBlobs = getMultiBlobs(file4Id);
        assertEquals(3, multiBlobs.size());
        assertMultiBlob(multiBlobs, 0, "four", "four.txt");

        JSONDocumentNode jsonFile4 = getJSONDocumentNode(file4Id);

        // update the sibling
        jsonFile4 = replaceMultiBlobs(jsonFile4, 0, on -> on.put("filename", "four-bis.txt"));

        try (CloseableClientResponse response = putJSONDocument(file4Id, jsonFile4)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            multiBlobs = getMultiBlobs(file4Id);
            assertEquals(3, multiBlobs.size());
            assertMultiBlob(multiBlobs, 0, "four", "four-bis.txt");
        }
    }

    @Test
    public void shouldRemoveTheBlobWhenAbsentAndUpdatingSiblingInComplex() throws IOException {
        List<Map<String, Serializable>> multiBlobs = getMultiBlobs(file4Id);
        assertEquals(3, multiBlobs.size());
        assertMultiBlob(multiBlobs, 0, "four", "four.txt");

        JSONDocumentNode jsonFile4 = getJSONDocumentNode(file4Id);

        // remove mb:blobs/0/blob property / update the sibling
        jsonFile4 = replaceMultiBlobs(jsonFile4, 0, on -> on.put("filename", "four-bis.txt").remove("content"));

        try (CloseableClientResponse response = putJSONDocument(file4Id, jsonFile4)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            multiBlobs = getMultiBlobs(file4Id);
            assertEquals(3, multiBlobs.size());
            assertMultiBlobNull(multiBlobs, 0, "four-bis.txt");
        }
    }

    @Test
    public void shouldRemoveTheBlobWhenRemovingItInComplex() throws IOException {
        List<Map<String, Serializable>> multiBlobs = getMultiBlobs(file4Id);
        assertEquals(3, multiBlobs.size());
        assertMultiBlob(multiBlobs, 0, "four", "four.txt");

        JSONDocumentNode jsonFile4 = getJSONDocumentNode(file4Id);

        // set a null value for mb:blobs/0/blob property
        jsonFile4 = replaceMultiBlobs(jsonFile4, 0, on -> on.replace("content", NullNode.instance));

        try (CloseableClientResponse response = putJSONDocument(file4Id, jsonFile4)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            multiBlobs = getMultiBlobs(file4Id);
            assertEquals(3, multiBlobs.size());
            assertMultiBlobNull(multiBlobs, 0, "four.txt");
        }
    }

    @Test
    public void shouldRemoveTheBlobWhenRemovingItAndUpdatingSiblingInComplex() throws IOException {
        List<Map<String, Serializable>> multiBlobs = getMultiBlobs(file4Id);
        assertEquals(3, multiBlobs.size());
        assertMultiBlob(multiBlobs, 0, "four", "four.txt");

        JSONDocumentNode jsonFile4 = getJSONDocumentNode(file4Id);

        // set a null value for mb:blobs/0/blob property
        jsonFile4 = replaceMultiBlobs(jsonFile4, 0,
                on -> on.put("filename", "four-bis.txt").replace("content", NullNode.instance));

        try (CloseableClientResponse response = putJSONDocument(file4Id, jsonFile4)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            multiBlobs = getMultiBlobs(file4Id);
            assertEquals(3, multiBlobs.size());
            assertMultiBlobNull(multiBlobs, 0, "four-bis.txt");
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test:test-download-permissions-contrib.xml")
    public void shouldThrowAnErrorWhenUserCannotDownloadBlob() throws IOException {
        assertBlobContent(file1Id, "foo");
        assertBlobContent(file2Id, "bar");

        JSONDocumentNode jsonFile1 = getJSONDocumentNode(file1Id);
        JSONDocumentNode jsonFile2 = getJSONDocumentNode(file2Id);

        // replace file1 'file:content' with file2 'file:content'
        jsonFile1.setPropertyValue(FILE_CONTENT_PROP, jsonFile2.getPropertyAsJsonNode(FILE_CONTENT_PROP));

        // Administrator has no permission to download any blob
        try (CloseableClientResponse response = putJSONDocument(file1Id, jsonFile1)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

            // ensure nothing has changed
            transactionalFeature.nextTransaction();
            assertBlobContent(file1Id, "foo");
            assertBlobContent(file2Id, "bar");
        }
    }

    protected JSONDocumentNode getJSONDocumentNode(String docId) throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "id/" + docId, HEADERS)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return new JSONDocumentNode((response.getEntityInputStream()));
        }
    }

    protected CloseableClientResponse putJSONDocument(String docId, JSONDocumentNode jsonDoc) throws IOException {
        return getResponse(RequestType.PUT, "id/" + docId, jsonDoc.asJson());
    }

    protected void assertBlobContent(String docId, String expectedContent) throws IOException {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        Blob blob = (Blob) doc.getPropertyValue(FILE_CONTENT_PROP);
        assertEquals(expectedContent, blob.getString());
    }

    protected void assertBlobNull(String docId) {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        Blob blob = (Blob) doc.getPropertyValue(FILE_CONTENT_PROP);
        assertNull(blob);
    }

    protected void removeBlob(String docId) {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        doc.setPropertyValue(FILE_CONTENT_PROP, null);
        session.saveDocument(doc);
        transactionalFeature.nextTransaction();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Serializable>> getAttachments(String docId) {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        return (List<Map<String, Serializable>>) doc.getPropertyValue(FILES_FILES_PROP);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Serializable>> getMultiBlobs(String docId) {
        DocumentModel doc = session.getDocument(new IdRef(docId));
        return (List<Map<String, Serializable>>) doc.getPropertyValue(MULTIBLOB_BLOBS_PROP);
    }

    protected JSONDocumentNode removeAttachment(JSONDocumentNode jsonDoc, int index) {
        JsonNode jsonNode = jsonDoc.getPropertyAsJsonNode(FILES_FILES_PROP);
        jsonNode = removeAttachment(jsonNode, index);
        jsonDoc.setPropertyValue(FILES_FILES_PROP, jsonNode);
        return jsonDoc;
    }

    protected JsonNode removeAttachment(JsonNode attachmentsNode, int index) {
        assertTrue(attachmentsNode.isArray());
        ArrayNode arrayNode = (ArrayNode) attachmentsNode;
        arrayNode.remove(index);
        return arrayNode;
    }

    protected JSONDocumentNode replaceAttachment(JSONDocumentNode jsonDoc, int index, JsonNode newAttachment) {
        JsonNode jsonNode = jsonDoc.getPropertyAsJsonNode(FILES_FILES_PROP);
        assertTrue(jsonNode.isArray());
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        JsonNode fileNode = arrayNode.get(index);
        assertTrue(fileNode.isObject());
        ((ObjectNode) fileNode).replace("file", newAttachment);
        jsonDoc.setPropertyValue(FILES_FILES_PROP, arrayNode);
        return jsonDoc;
    }

    protected JSONDocumentNode replaceMultiBlobs(JSONDocumentNode jsonDoc, int index, Consumer<ObjectNode> consumer) {
        JsonNode jsonNode = jsonDoc.getPropertyAsJsonNode(MULTIBLOB_BLOBS_PROP);
        assertTrue(jsonNode.isArray());
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        assertTrue(arrayNode.get(index).isObject());
        ObjectNode blobNode = (ObjectNode) arrayNode.get(index);
        consumer.accept(blobNode);
        jsonDoc.setPropertyValue(FILES_FILES_PROP, arrayNode);
        return jsonDoc;
    }

    protected void assertAttachmentContent(List<Map<String, Serializable>> attachments, int index,
            String expectedContent) throws IOException {
        assertEquals(expectedContent, ((Blob) attachments.get(index).get("file")).getString());
    }

    protected void assertMultiBlob(List<Map<String, Serializable>> multiBlobs, int index, String expectedContent,
            String expectedFilename) throws IOException {
        assertEquals(expectedContent, ((Blob) multiBlobs.get(index).get("content")).getString());
        assertEquals(expectedFilename, multiBlobs.get(index).get("filename"));
    }

    protected void assertMultiBlobNull(List<Map<String, Serializable>> multiBlobs, int index, String expectedFilename) {
        assertNull(multiBlobs.get(index).get("content"));
        assertEquals(expectedFilename, multiBlobs.get(index).get("filename"));
    }

}
