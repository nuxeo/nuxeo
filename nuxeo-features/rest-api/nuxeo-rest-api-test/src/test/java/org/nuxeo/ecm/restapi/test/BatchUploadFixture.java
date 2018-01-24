/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     ataillefer
 *     Gabriel Barata
 */
package org.nuxeo.ecm.restapi.test;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.operations.blob.CreateBlob;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerComponent;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.transientstore.test.TransientStoreFeature;

import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
@Features({ TransientStoreFeature.class, RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@LocalDeploy({ "org.nuxeo.ecm.platform.restapi.test:multiblob-doctype.xml",
        "org.nuxeo.ecm.platform.restapi.test:test-conflict-batch-handler.xml" })
public class BatchUploadFixture extends BaseTest {

    @Inject
    CoreSession session;

    /**
     * Tests the /upload endpoints.
     *
     * @since 7.4
     */
    @Test
    public void itCanUseBatchUpload() throws IOException {
        itCanUseBatchUpload(false);
    }

    /**
     * Tests the /upload endpoints with the "X-Batch-No-Drop" header set to true.
     *
     * @since 8.4
     */
    @Test
    public void itCanUseBatchUploadNoDrop() throws IOException {
        itCanUseBatchUpload(true);
    }

    private void itCanUseBatchUpload(boolean noDrop) throws IOException {

        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        // Upload a file not in multipart
        String fileName1 = URLEncoder.encode("Fichier accentué 1.txt", "UTF-8");
        String mimeType = "text/plain";
        String data1 = "Contenu accentué du premier fichier";
        String fileSize1 = String.valueOf(getUTF8Bytes(data1).length);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName1);
        headers.put("X-File-Size", fileSize1);
        headers.put("X-File-Type", mimeType);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data1,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
            // TODO NXP-18247 when the actual uploaded size is returned
            // assertEquals(fileSize1, node.get("uploadedSize").getValueAsText());
        }

        // Upload a file in multipart
        String fileName2 = "Fichier accentué 2.txt";
        String data2 = "Contenu accentué du deuxième fichier";
        String fileSize2 = String.valueOf(getUTF8Bytes(data2).length);
        headers = new HashMap<>();
        headers.put("X-File-Size", fileSize2);
        headers.put("X-File-Type", mimeType);

        BodyPart fdp = new StreamDataBodyPart(fileName2, new ByteArrayInputStream(getUTF8Bytes(data2)));
        try (FormDataMultiPart form = new FormDataMultiPart()) {
            form.bodyPart(fdp);
            try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/1", form,
                    headers)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
                String strResponse = IOUtils.toString(response.getEntityInputStream(), "UTF-8");
                assertTrue(strResponse.startsWith("<html>") && strResponse.endsWith("</html>"));
                strResponse = strResponse.substring(6, strResponse.length() - 7);
                JsonNode node = mapper.readTree(strResponse);
                assertEquals("true", node.get("uploaded").getValueAsText());
                assertEquals(batchId, node.get("batchId").getValueAsText());
                assertEquals("1", node.get("fileIdx").getValueAsText());
                assertEquals("normal", node.get("uploadType").getValueAsText());
                assertEquals(fileSize2, node.get("uploadedSize").getValueAsText());
            }
        }

        // Get batch info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
            assertEquals(2, nodes.size());
            JsonNode node = nodes.get(0);
            assertEquals("Fichier accentué 1.txt", node.get("name").getValueAsText());
            assertEquals(fileSize1, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
            node = nodes.get(1);
            assertEquals("Fichier accentué 2.txt", node.get("name").getValueAsText());
            assertEquals(fileSize2, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        // Get file infos
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/0")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Fichier accentué 1.txt", node.get("name").getValueAsText());
            assertEquals(fileSize1, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/1")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Fichier accentué 2.txt", node.get("name").getValueAsText());
            assertEquals(fileSize2, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        // Create a doc which references the uploaded blobs using the Document path endpoint
        String json = "{";
        json += "\"entity-type\":\"document\" ,";
        json += "\"name\":\"testBatchUploadDoc\" ,";
        json += "\"type\":\"MultiBlobDoc\" ,";
        json += "\"properties\" : {";
        json += "\"mb:blobs\" : [ ";
        json += "{ \"content\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"0\" } },";
        json += "{ \"content\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"1\" } }";
        json += "]}}";
        if (noDrop) {
            headers = new HashMap<>();
            headers.put("X-Batch-No-Drop", "true");
            try (CloseableClientResponse response = getResponse(RequestType.POST, "path/", json, headers)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        } else {
            try (CloseableClientResponse response = getResponse(RequestType.POST, "path/", json)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        }

        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNotNull(blob);
        assertEquals("Fichier accentué 1.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(data1, blob.getString());
        blob = (Blob) doc.getPropertyValue("mb:blobs/1/content");
        assertNotNull(blob);
        assertEquals("Fichier accentué 2.txt", blob.getFilename());
        assertEquals(mimeType, blob.getMimeType());
        assertEquals(data2, blob.getString());

        if (noDrop) {
            assertBatchExists(batchId);
        }
    }

    /**
     * tests if the X-File-Type header is obeyed on multipart file upload (NXP-22408)
     */
    @Test
    public void testObeyFileTypeHeader() throws IOException {
        Map<String, String> headers;

        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        // Upload a file in multipart, first without the X-File-Type header, the second with
        String fileName1 = "No header.txt";
        String data1 = "File without explicit file type";
        String fileSize1 = String.valueOf(getUTF8Bytes(data1).length);
        headers = new HashMap<>();
        headers.put("X-File-Size", fileSize1);

        try (FormDataMultiPart form = new FormDataMultiPart()) {
            BodyPart fdp = new StreamDataBodyPart(fileName1, new ByteArrayInputStream(getUTF8Bytes(data1)));
            form.bodyPart(fdp);
            try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", form,
                    headers)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        }

        String mimeType = "text/plain";
        String fileName2 = "With header.txt";
        String data2 = "File with explicit X-File-Type header";
        String fileSize2 = String.valueOf(getUTF8Bytes(data2).length);
        headers = new HashMap<>();
        headers.put("X-File-Size", fileSize2);
        headers.put("X-File-Type", mimeType);

        try (FormDataMultiPart form = new FormDataMultiPart()) {
            BodyPart fdp = new StreamDataBodyPart(fileName2, new ByteArrayInputStream(getUTF8Bytes(data2)));
            form.bodyPart(fdp);
            try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/1", form,
                    headers)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        }

        // Create a doc which references the uploaded blobs using the Document path endpoint
        String json = "{";
        json += "\"entity-type\":\"document\" ,";
        json += "\"name\":\"testBatchUploadDoc\" ,";
        json += "\"type\":\"MultiBlobDoc\" ,";
        json += "\"properties\" : {";
        json += "\"mb:blobs\" : [ ";
        json += "{ \"content\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"0\" } },";
        json += "{ \"content\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"1\" } }";
        json += "]}}";

        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/", json)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        // verify the created document
        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNotNull(blob);
        assertEquals("No header.txt", blob.getFilename());
        // Default multipart mime type must be set
        assertEquals("application/octet-stream", blob.getMimeType());
        assertEquals(data1, blob.getString());
        blob = (Blob) doc.getPropertyValue("mb:blobs/1/content");
        assertNotNull(blob);
        assertEquals("With header.txt", blob.getFilename());
        // X-File-Type header mime type must be set
        assertEquals(mimeType, blob.getMimeType());
        assertEquals(data2, blob.getString());
    }

    /**
     * Tests the use of /upload + /upload/{batchId}/{fileIdx}/execute.
     *
     * @since 7.4
     */
    @Test
    public void testBatchExecute() throws IOException {
        testBatchExecute(false);
    }

    /**
     * Tests the use of /upload + /upload/{batchId}/{fileIdx}/execute with the "X-Batch-No-Drop" header set to true.
     *
     * @since 8.4
     */
    @Test
    public void testBatchExecuteNoDrop() throws IOException {
        testBatchExecute(true);
    }

    private void testBatchExecute(boolean noDrop) throws IOException {

        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }

        // Upload file
        String fileName = URLEncoder.encode("Fichier accentué.txt", "UTF-8");
        String mimeType = "text/plain";
        String data = "Contenu accentué";
        String fileSize = String.valueOf(getUTF8Bytes(data).length);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data,
                headers)) {
        }

        // Create a doc and attach the uploaded blob to it using the /upload/{batchId}/{fileIdx}/execute endpoint
        DocumentModel file = session.createDocumentModel("/", "testBatchExecuteDoc", "File");
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String json = "{\"params\":{";
        json += "\"document\":\"" + file.getPathAsString() + "\"";
        json += "}}";
        if (noDrop) {
            headers = new HashMap<>();
            headers.put("X-Batch-No-Drop", "true");
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/0/execute/Blob.Attach", json, headers)) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
        } else {
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/0/execute/Blob.Attach", json)) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
        }

        DocumentModel doc = session.getDocument(new PathRef("/testBatchExecuteDoc"));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("Fichier accentué.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(data, blob.getString());

        if (noDrop) {
            assertBatchExists(batchId);
        }
    }

    /**
     * @since 8.1O
     */
    @Test
    public void testBatchExecuteAutomationServerBindings() throws IOException {

        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }

        File file = Framework.createTempFile("nx-test-blob-", ".tmp");
        try {
            service = getServiceFor("user1", "user1");
            CreateBlob.skipProtocolCheck = true;
            String json = "{\"params\":{";
            json += "\"file\":\"" + file.toURI().toURL() + "\"";
            json += "}}";
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/execute/Blob.CreateFromURL", json)) {
                assertEquals(SC_FORBIDDEN, response.getStatus());
            }

            service = getServiceFor("Administrator", "Administrator");
            // Batch has been cleaned up by the previous call
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/execute/Blob.CreateFromURL", json)) {
                assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            }
            // Create a new batch
            try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
                JsonNode node = mapper.readTree(response.getEntityInputStream());
                batchId = node.get("batchId").getValueAsText();
            }
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/execute/Blob.CreateFromURL", json)) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
        } finally {
            CreateBlob.skipProtocolCheck = false;
            file.delete();
        }
    }

    /**
     * Tests upload using file chunks.
     *
     * @since 7.4
     */
    @Test
    public void testChunkedUpload() throws IOException {

        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        // Upload chunks in desorder
        String fileName = URLEncoder.encode("Fichier accentué.txt", "UTF-8");
        String mimeType = "text/plain";
        String fileContent = "Contenu accentué composé de 3 chunks";
        String fileSize = String.valueOf(getUTF8Bytes(fileContent).length);
        String chunk1 = "Contenu accentu";
        String chunk2 = "é composé de ";
        String chunk3 = "3 chunks";

        // Chunk 1
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Index", "0");
        headers.put("X-Upload-Chunk-Count", "3");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk1,
                headers)) {
            assertEquals(308, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            // TODO NXP-18247 when the actual uploaded size is returned
            // assertEquals(String.valueOf(getUTF8Bytes(chunk1).length), node.get("uploadedSize").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(1, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        // Get file info, here just to test the GET method
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/0")) {
            assertEquals(308, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(1, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        // Chunk 3
        headers.put("X-Upload-Chunk-Index", "2");
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk3,
                headers)) {
            assertEquals(308, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            // TODO NXP-18247 when the actual uploaded size is returned
            // assertEquals(String.valueOf(getUTF8Bytes(chunk3).length), node.get("uploadedSize").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(2, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("2", chunkIds.get(1).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        // Get file info, here just to test the GET method
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/0")) {
            assertEquals(308, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(2, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("2", chunkIds.get(1).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        // Chunk 2
        headers.put("X-Upload-Chunk-Index", "1");
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk2,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            // TODO NXP-18247 when the actual uploaded size is returned
            // assertEquals(String.valueOf(getUTF8Bytes(chunk2).length), node.get("uploadedSize").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(3, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("1", chunkIds.get(1).getValueAsText());
            assertEquals("2", chunkIds.get(2).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        // Get file info, here just to test the GET method
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/0")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(3, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("1", chunkIds.get(1).getValueAsText());
            assertEquals("2", chunkIds.get(2).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        // Get batch info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
            assertEquals(1, nodes.size());
            JsonNode node = nodes.get(0);
            assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(3, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("1", chunkIds.get(1).getValueAsText());
            assertEquals("2", chunkIds.get(2).getValueAsText());
            assertEquals("3", node.get("chunkCount").getValueAsText());
        }

        BatchManager bm = Framework.getService(BatchManager.class);
        Blob blob = bm.getBlob(batchId, "0");
        assertNotNull(blob);
        assertEquals("Fichier accentué.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(Long.parseLong(fileSize), blob.getLength());
        assertEquals("Contenu accentué composé de 3 chunks", blob.getString());

        bm.clean(batchId);
    }

    /**
     * Tests the use of /upload using file chunks + /upload/{batchId}/{fileIdx}/execute.
     *
     * @since 7.4
     */
    @Test
    public void testBatchExecuteWithChunkedUpload() throws IOException {
        testBatchExecuteWithChunkedUpload(false);
    }

    /**
     * Tests the use of /upload using file chunks + /upload/{batchId}/{fileIdx}/execute with the "X-Batch-No-Drop"
     * header set to true.
     *
     * @since 8.4
     */
    @Test
    public void testBatchExecuteWithChunkedUploadNoDrop() throws IOException {
        testBatchExecuteWithChunkedUpload(true);
    }

    /**
     * Tests the use of /upload using file chunks + /upload/{batchId}/{fileIdx}/execute.
     *
     * @since 7.4
     */
    public void testBatchExecuteWithChunkedUpload(boolean noDrop) throws IOException {
        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }

        // Upload chunks in desorder
        String fileName = URLEncoder.encode("Fichier accentué.txt", "UTF-8");
        String mimeType = "text/plain";
        String fileContent = "Contenu accentué composé de 2 chunks";
        String fileSize = String.valueOf(getUTF8Bytes(fileContent).length);
        String chunk1 = "Contenu accentué compo";
        String chunk2 = "sé de 2 chunks";

        // Chunk 2
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Index", "1");
        headers.put("X-Upload-Chunk-Count", "2");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk2,
                headers)) {
            assertEquals(308, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            // TODO NXP-18247 when the actual uploaded size is returned
            // assertEquals(String.valueOf(getUTF8Bytes(chunk2).length), node.get("uploadedSize").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(1, chunkIds.size());
            assertEquals("1", chunkIds.get(0).getValueAsText());
            assertEquals("2", node.get("chunkCount").getValueAsText());
        }

        // Chunk 1
        headers.put("X-Upload-Chunk-Index", "0");
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk1,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("chunked", node.get("uploadType").getValueAsText());
            // TODO NXP-18247 when the actual uploaded size is returned
            // assertEquals(String.valueOf(getUTF8Bytes(chunk1).length), node.get("uploadedSize").getValueAsText());
            ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
            assertEquals(2, chunkIds.size());
            assertEquals("0", chunkIds.get(0).getValueAsText());
            assertEquals("1", chunkIds.get(1).getValueAsText());
            assertEquals("2", node.get("chunkCount").getValueAsText());
        }

        // Create a doc and attach the uploaded blob to it using the /batch/{batchId}/{fileIdx}/execute endpoint
        DocumentModel file = session.createDocumentModel("/", "testBatchExecuteDoc", "File");
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String json = "{\"params\":{";
        json += "\"document\":\"" + file.getPathAsString() + "\"";
        json += "}}";
        if (noDrop) {
            headers = new HashMap<>();
            headers.put("X-Batch-No-Drop", "true");
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/0/execute/Blob.Attach", json, headers)) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
        } else {
            try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                    "upload/" + batchId + "/0/execute/Blob.Attach", json)) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
        }

        DocumentModel doc = session.getDocument(new PathRef("/testBatchExecuteDoc"));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("Fichier accentué.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("Contenu accentué composé de 2 chunks", blob.getString());

        if (noDrop) {
            assertBatchExists(batchId);
        }
    }

    /**
     * We patched the Content-Type and X-File-Type header for NXP-12802 / NXP-13036
     *
     * @since 9.2
     */
    @Test
    public void testBatchUploadExecuteWithBadMimeType() throws Exception {
        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }

        // Upload file
        String fileName = URLEncoder.encode("file.pdf", "UTF-8");
        String badMimeType = "pdf";
        String data = "Empty and wrong pdf data";
        String fileSize = String.valueOf(getUTF8Bytes(data).length);
        Map<String, String> headers = new HashMap<>();
        // impossible to test a bad content-type as the client will parse it
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", badMimeType);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data,
                headers)) {
        }

        // Create a doc and attach the uploaded blob to it using the /upload/{batchId}/{fileIdx}/execute endpoint
        DocumentModel file = session.createDocumentModel("/", "testBatchExecuteDoc", "File");
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String json = "{\"params\":{";
        json += "\"document\":\"" + file.getPathAsString() + "\"";
        json += "}}";
        try (CloseableClientResponse response = getResponse(RequestType.POSTREQUEST,
                "upload/" + batchId + "/0/execute/Blob.Attach", json)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }

        DocumentModel doc = session.getDocument(new PathRef("/testBatchExecuteDoc"));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("file.pdf", blob.getFilename());
        assertEquals("application/pdf", blob.getMimeType());
        assertEquals(data, blob.getString());
    }

    /**
     * @since 7.4
     */
    @Test
    public void testCancelBatch() throws IOException {

        // Init batch
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }

        // Cancel batch
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "upload/" + batchId)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @since 7.4
     */
    @Test
    public void testEmptyResponseCases() throws IOException {

        // Upload
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/fakeBatchId/0")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // Get batch info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/fakeBatchId")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Get file info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/fakeBatchId/0")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/0")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // Cancel batch
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "upload/fakeBatchId")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @since 7.10
     */
    @Test
    public void testBadRequests() throws IOException {
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
        }

        // Bad file index
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/a")) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // Bad chunk index
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Count", "2");
        headers.put("X-Upload-Chunk-Index", "a");
        headers.put("X-File-Size", "100");
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0",
                "chunkContent", headers)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // Bad chunk count
        headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Count", "a");
        headers.put("X-Upload-Chunk-Index", "0");
        headers.put("X-File-Size", "100");
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0",
                "chunkContent", headers)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // Bad file size
        headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Count", "2");
        headers.put("X-Upload-Chunk-Index", "0");
        headers.put("X-File-Size", "a");
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0",
                "chunkContent", headers)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    /**
     * @since 8.4
     */
    @Test
    public void testRemoveFile() throws IOException {
        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        int numfiles = 5;

        // Upload test files
        String fileName, data, fileSize = null, mimeType = "text/plain";
        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < numfiles; i++) {
            fileName = URLEncoder.encode("Test File " + Integer.toString(i + 1) + ".txt", "UTF-8");
            data = "Test Content " + Integer.toString(i + 1);
            if (fileSize == null) {
                fileSize = String.valueOf(getUTF8Bytes(data).length);
                headers.put("Content-Type", "text/plain");
                headers.put("X-Upload-Type", "normal");
                headers.put("X-File-Size", fileSize);
                headers.put("X-File-Type", mimeType);
            }
            headers.put("X-File-Name", fileName);
            try (CloseableClientResponse response = getResponse(RequestType.POST,
                    "upload/" + batchId + "/" + Integer.toString(i), data, headers)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
        }

        // Get batch info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
            assertEquals(numfiles, nodes.size());
            for (int i = 0; i < numfiles; i++) {
                JsonNode node = nodes.get(i);
                assertEquals("Test File " + Integer.toString(i + 1) + ".txt", node.get("name").getValueAsText());
                assertEquals(fileSize, node.get("size").getValueAsText());
                assertEquals("normal", node.get("uploadType").getValueAsText());
            }
        }

        // remove files #2 and #4
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "upload/" + batchId + "/1")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "upload/" + batchId + "/3")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // check if the remaining files are the correct ones
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
            assertEquals(numfiles - 2, nodes.size());
            JsonNode node = nodes.get(0);
            assertEquals("Test File 1.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
            node = nodes.get(1);
            assertEquals("Test File 3.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
            node = nodes.get(2);
            assertEquals("Test File 5.txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        // test removal of invalid file index
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "upload/" + batchId + "/3")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    protected byte[] getUTF8Bytes(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    protected void assertBatchExists(String batchId) {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

    /** @since 9.3 */
    @Test
    public void testEmptyFileUpload() throws IOException {
        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        // Upload an empty file not in multipart
        String fileName1 = URLEncoder.encode("Fichier accentué 1.txt", "UTF-8");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-File-Name", fileName1);
        headers.put("X-File-Size", "0");

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
            assertEquals("0", node.get("uploadedSize").getValueAsText());
        }

        // Upload a file in multipart
        String fileName2 = "Fichier accentué 2.txt";
        headers = new HashMap<>();
        headers.put("X-File-Size", "0");

        BodyPart fdp = new StreamDataBodyPart(fileName2, new ByteArrayInputStream(new byte[] {}));
        try (FormDataMultiPart form = new FormDataMultiPart()) {
            form.bodyPart(fdp);
            try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/1", form,
                    headers)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
                String strResponse = IOUtils.toString(response.getEntityInputStream(), "UTF-8");
                assertTrue(strResponse.startsWith("<html>") && strResponse.endsWith("</html>"));
                strResponse = strResponse.substring(6, strResponse.length() - 7);
                JsonNode node = mapper.readTree(strResponse);
                assertEquals("true", node.get("uploaded").getValueAsText());
                assertEquals(batchId, node.get("batchId").getValueAsText());
                assertEquals("1", node.get("fileIdx").getValueAsText());
                assertEquals("normal", node.get("uploadType").getValueAsText());
                assertEquals("0", node.get("uploadedSize").getValueAsText());
            }
        }

        // Get batch info
        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId)) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
            assertEquals(2, nodes.size());
            JsonNode node = nodes.get(0);
            assertEquals("Fichier accentué 1.txt", node.get("name").getValueAsText());
            assertEquals("0", node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
            node = nodes.get(1);
            assertEquals("Fichier accentué 2.txt", node.get("name").getValueAsText());
            assertEquals("0", node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }
    }

    @Test
    public void testDefaultProviderAsLegacyFallback() throws Exception {
        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/info")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode jsonNode = mapper.readTree(response.getEntityInputStream());
            assertTrue(jsonNode.get("provider") != null && !jsonNode.get("provider").isNull());
            assertEquals(BatchManagerComponent.DEFAULT_BATCH_HANDLER, jsonNode.get("provider").getValueAsText());

            JsonNode fileEntriesNode = jsonNode.get("fileEntries");
            ArrayNode fileEntriesArrayNode = null;
            if (fileEntriesNode != null && !fileEntriesNode.isNull() && fileEntriesNode.isArray()) {
                fileEntriesArrayNode = (ArrayNode) fileEntriesNode;
            }
            assertNotNull(fileEntriesArrayNode);
            assertEquals(0, fileEntriesArrayNode.size());

            assertEquals(batchId, jsonNode.get("batchId").getValueAsText());
        }
    }

    @Test
    public void testConflictOnCompleteUploadError() throws Exception {
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/new/dummy")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode responseJson = mapper.readTree(response.getEntityInputStream());
            batchId = responseJson.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0/complete",
                "{}")) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testBatchUploadRemoveFileEntryWithProvider() throws Exception {
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/new/dummy")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode responseJson = mapper.readTree(response.getEntityInputStream());
            batchId = responseJson.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        // Upload a file not in multipart
        String fileName1 = URLEncoder.encode("Fichier accentué 1.txt", "UTF-8");
        String mimeType = "text/plain";
        String data1 = "Contenu accentué du premier fichier";
        String fileSize1 = String.valueOf(getUTF8Bytes(data1).length);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName1);
        headers.put("X-File-Size", fileSize1);
        headers.put("X-File-Type", mimeType);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data1,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        // Upload a file not in multipart
        String fileName2 = URLEncoder.encode("Fichier accentué 2.txt", "UTF-8");
        headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName2);
        headers.put("X-File-Size", fileSize1);
        headers.put("X-File-Type", mimeType);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/1", data1,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("1", node.get("fileIdx").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/info")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode fileEntriesJsonNode = node.get("fileEntries");

            assertTrue(fileEntriesJsonNode.isArray());
            ArrayNode fileEntries = (ArrayNode) fileEntriesJsonNode;
            assertEquals(2, fileEntries.size());
        }

        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "upload/" + batchId + "/0")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, "upload/" + batchId + "/info")) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            JsonNode fileEntriesJsonNode = node.get("fileEntries");

            assertTrue(fileEntriesJsonNode.isArray());
            ArrayNode fileEntries = (ArrayNode) fileEntriesJsonNode;
            assertEquals(1, fileEntries.size());
        }
    }

    @Test
    public void testBatchUploadWithMultivaluedBlobProperty() throws Exception {

        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").getValueAsText();
            assertNotNull(batchId);
        }

        // Upload a file not in multipart
        String fileName1 = URLEncoder.encode("File.txt", "UTF-8");
        String mimeType = "text/plain";
        String data1 = "Content";
        String fileSize1 = String.valueOf(getUTF8Bytes(data1).length);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName1);
        headers.put("X-File-Size", fileSize1);
        headers.put("X-File-Type", mimeType);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data1,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").getValueAsText());
            assertEquals(batchId, node.get("batchId").getValueAsText());
            assertEquals("0", node.get("fileIdx").getValueAsText());
        }

        String json = "{";
        json += "\"entity-type\":\"document\" ,";
        json += "\"name\":\"testBatchUploadDoc\" ,";
        json += "\"type\":\"File\" ,";
        json += "\"properties\" : {";
        json += "\"files:files\" : [ ";
        json += "{ \"file\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"0\" }},";
        json += "{ \"file\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"1\" }}";
        json += "]}}";

        // Assert second batch won't make the upload fail because the file does not exist
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/", json)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob1 = (Blob) doc.getPropertyValue("files:files/0/file");
        assertNotNull(blob1);
        assertEquals("File.txt", blob1.getFilename());

    }

}
