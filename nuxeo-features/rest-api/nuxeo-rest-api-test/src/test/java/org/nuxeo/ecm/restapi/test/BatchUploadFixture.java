/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.operations.blob.CreateBlob;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.transientstore.test.TransientStoreFeature;

import com.sun.jersey.api.client.ClientResponse;
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
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:multiblob-doctype.xml")
public class BatchUploadFixture extends BaseTest {

    @Inject
    CoreSession session;

    /**
     * Tests the deprecated /batch/upload endpoint.
     *
     * @deprecated since 7.4
     * @see org.nuxeo.ecm.automation.server.jaxrs.batch.BatchResource#doPost(HttpServletRequest)
     * @see #itCanUseBatchUpload()
     */
    @Deprecated
    @Test
    public void itCanUseBatchResource() throws Exception {
        String filename = "testfile";
        String data = "batchUploadedData";

        // upload the file in automation
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-File-Idx", "0");
        headers.put("X-File-Name", filename);
        ClientResponse response = getResponse(RequestType.POST, "automation/batch/upload", data, headers);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();
        assertNotNull(batchId);

        // create the doc which references the given blob
        String json = "{";
        json += "\"entity-type\":\"document\" ,";
        json += "\"name\":\"testBatchUploadDoc\" ,";
        json += "\"type\":\"MultiBlobDoc\" ,";
        json += "\"properties\" : {";
        json += "\"mb:blobs\" : [ ";
        json += "{ \"filename\" : \"" + filename + "\" , \"content\" : { \"upload-batch\": \"" + batchId
                + "\", \"upload-fileId\": \"0\" } }";
        json += "]}}";
        response = getResponse(RequestType.POST, "path/", json);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNotNull(blob);
        assertEquals(data, blob.getString());
    }

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
        ClientResponse response = getResponse(RequestType.POST, "upload");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();
        assertNotNull(batchId);

        // Upload a file not in multipart
        String fileName1 = URLEncoder.encode("Fichier accentué 1.txt", "UTF-8");
        String mimeType = "text/plain";
        String data1 = "Contenu accentué du premier fichier";
        String fileSize1 = String.valueOf(data1.getBytes().length);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName1);
        headers.put("X-File-Size", fileSize1);
        headers.put("X-File-Type", mimeType);

        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data1, headers);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());
        assertEquals(fileSize1, node.get("uploadedSize").getValueAsText());

        // Upload a file in multipart
        String fileName2 = "Fichier accentué 2.txt";
        String data2 = "Contenu accentué du deuxième fichier";
        String fileSize2 = String.valueOf(data2.getBytes().length);
        headers = new HashMap<String, String>();
        headers.put("X-File-Size", fileSize2);

        FormDataMultiPart form = new FormDataMultiPart();
        BodyPart fdp = new StreamDataBodyPart(fileName2, new ByteArrayInputStream(data2.getBytes()));
        form.bodyPart(fdp);
        response = getResponse(RequestType.POST, "upload/" + batchId + "/1", form, headers);
        form.close();
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        String strResponse = IOUtils.toString(response.getEntityInputStream(), "UTF-8");
        assertTrue(strResponse.startsWith("<html>") && strResponse.endsWith("</html>"));
        strResponse = strResponse.substring(6, strResponse.length() - 7);
        node = mapper.readTree(strResponse);
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("1", node.get("fileIdx").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());
        assertEquals(fileSize2, node.get("uploadedSize").getValueAsText());

        // Get batch info
        response = getResponse(RequestType.GET, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
        assertEquals(2, nodes.size());
        node = nodes.get(0);
        assertEquals("Fichier accentué 1.txt", node.get("name").getValueAsText());
        assertEquals(fileSize1, node.get("size").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());
        node = nodes.get(1);
        assertEquals("Fichier accentué 2.txt", node.get("name").getValueAsText());
        assertEquals(fileSize2, node.get("size").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());

        // Get file infos
        response = getResponse(RequestType.GET, "upload/" + batchId + "/0");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Fichier accentué 1.txt", node.get("name").getValueAsText());
        assertEquals(fileSize1, node.get("size").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());

        response = getResponse(RequestType.GET, "upload/" + batchId + "/1");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Fichier accentué 2.txt", node.get("name").getValueAsText());
        assertEquals(fileSize2, node.get("size").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());

        // Get file upload statuses by doing an empty POST
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0");
        assertEquals(201, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());
        // TODO NXP-18247: Put actual uploaded size in response
        // assertEquals(fileSize1, node.get("uploadedSize").getValueAsText());

        response = getResponse(RequestType.POST, "upload/" + batchId + "/1");
        assertEquals(201, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("1", node.get("fileIdx").getValueAsText());
        assertEquals("normal", node.get("uploadType").getValueAsText());
        // TODO NXP-18247: Put actual uploaded size in response
        // assertEquals(fileSize2, node.get("uploadedSize").getValueAsText());

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
            response = getResponse(RequestType.POST, "path/", json, headers);
        } else {
            response = getResponse(RequestType.POST, "path/", json);
        }
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNotNull(blob);
        assertEquals("Fichier accentué 1.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(data1, blob.getString());
        blob = (Blob) doc.getPropertyValue("mb:blobs/1/content");
        assertNotNull(blob);
        assertEquals("Fichier accentué 2.txt", blob.getFilename());
        assertEquals("application/octet-stream", blob.getMimeType());
        assertEquals(data2, blob.getString());

        if (noDrop) {
            assertBatchExists(batchId);
        }
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
        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        // Upload file
        String fileName = URLEncoder.encode("Fichier accentué.txt", "UTF-8");
        String mimeType = "text/plain";
        String data = "Contenu accentué";
        String fileSize = String.valueOf(data.getBytes().length);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);
        getResponse(RequestType.POST, "upload/" + batchId + "/0", data, headers);

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
            response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/0/execute/Blob.Attach", json, headers);
        } else {
            response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/0/execute/Blob.Attach", json);
        }
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

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

        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        File file = Framework.createTempFile("nx-test-blob-", ".tmp");
        try {
            service = getServiceFor("user1", "user1");
            CreateBlob.skipProtocolCheck = true;
            String json = "{\"params\":{";
            json += "\"file\":\"" + file.toURI().toURL() + "\"";
            json += "}}";
            response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/execute/Blob.CreateFromURL", json);
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            service = getServiceFor("Administrator", "Administrator");
            response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/execute/Blob.CreateFromURL", json);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());

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
        ClientResponse response = getResponse(RequestType.POST, "upload");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();
        assertNotNull(batchId);

        // Upload chunks in desorder
        String fileName = URLEncoder.encode("Fichier accentué.txt", "UTF-8");
        String mimeType = "text/plain";
        String fileContent = "Contenu accentué composé de 3 chunks";
        String fileSize = String.valueOf(fileContent.getBytes().length);
        String chunk1 = "Contenu accentu";
        String chunkLength1 = String.valueOf(chunk1.getBytes().length);
        String chunk2 = "é composé de ";
        String chunkLength2 = String.valueOf(chunk2.getBytes().length);
        String chunk3 = "3 chunks";
        String chunkLength3 = String.valueOf(chunk3.getBytes().length);

        // Chunk 1
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Index", "0");
        headers.put("X-Upload-Chunk-Count", "3");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);

        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk1, headers);
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        assertEquals(chunkLength1, node.get("uploadedSize").getValueAsText());
        ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(1, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get file upload status by doing an empty POST
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0");
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        // TODO NXP-18247: Put actual uploaded size in response
        // assertEquals(chunkLength1, node.get("uploadedSize").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(1, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get file info, here just to test the GET method
        response = getResponse(RequestType.GET, "upload/" + batchId + "/0");
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
        assertEquals(fileSize, node.get("size").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(1, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Chunk 3
        headers.put("X-Upload-Chunk-Index", "2");
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk3, headers);
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        assertEquals(chunkLength3, node.get("uploadedSize").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(2, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("2", chunkIds.get(1).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get file upload status by doing an empty POST
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0");
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        // TODO NXP-18247: Put actual uploaded size in response
        // assertEquals(chunkLength3, node.get("uploadedSize").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(2, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("2", chunkIds.get(1).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get file info, here just to test the GET method
        response = getResponse(RequestType.GET, "upload/" + batchId + "/0");
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
        assertEquals(fileSize, node.get("size").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(2, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("2", chunkIds.get(1).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Chunk 2
        headers.put("X-Upload-Chunk-Index", "1");
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk2, headers);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        assertEquals(chunkLength2, node.get("uploadedSize").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(3, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("1", chunkIds.get(1).getValueAsText());
        assertEquals("2", chunkIds.get(2).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get file upload status by doing an empty POST
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0");
        assertEquals(201, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("true", node.get("uploaded").getValueAsText());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        // TODO NXP-18247: Put actual uploaded size in response
        // assertEquals(chunkLength2, node.get("uploadedSize").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(3, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("1", chunkIds.get(1).getValueAsText());
        assertEquals("2", chunkIds.get(2).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get file info, here just to test the GET method
        response = getResponse(RequestType.GET, "upload/" + batchId + "/0");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
        assertEquals(fileSize, node.get("size").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(3, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("1", chunkIds.get(1).getValueAsText());
        assertEquals("2", chunkIds.get(2).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

        // Get batch info
        response = getResponse(RequestType.GET, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
        assertEquals(1, nodes.size());
        node = nodes.get(0);
        assertEquals("Fichier accentué.txt", node.get("name").getValueAsText());
        assertEquals(fileSize, node.get("size").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(3, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("1", chunkIds.get(1).getValueAsText());
        assertEquals("2", chunkIds.get(2).getValueAsText());
        assertEquals("3", node.get("chunkCount").getValueAsText());

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
     * Tests the use of /upload using file chunks + /upload/{batchId}/{fileIdx}/execute  with the "X-Batch-No-Drop"
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
        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        // Upload chunks in desorder
        String fileName = URLEncoder.encode("Fichier accentué.txt", "UTF-8");
        String mimeType = "text/plain";
        String fileContent = "Contenu accentué composé de 2 chunks";
        String fileSize = String.valueOf(fileContent.getBytes().length);
        String chunk1 = "Contenu accentué compo";
        String chunkLength1 = String.valueOf(chunk1.getBytes().length);
        String chunk2 = "sé de 2 chunks";
        String chunkLength2 = String.valueOf(chunk2.getBytes().length);

        // Chunk 2
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Index", "1");
        headers.put("X-Upload-Chunk-Count", "2");
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);

        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk2, headers);
        assertEquals(308, response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        assertEquals(chunkLength2, node.get("uploadedSize").getValueAsText());
        ArrayNode chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(1, chunkIds.size());
        assertEquals("1", chunkIds.get(0).getValueAsText());
        assertEquals("2", node.get("chunkCount").getValueAsText());

        // Chunk 1
        headers.put("X-Upload-Chunk-Index", "0");
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", chunk1, headers);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
        assertEquals("chunked", node.get("uploadType").getValueAsText());
        assertEquals(chunkLength1, node.get("uploadedSize").getValueAsText());
        chunkIds = (ArrayNode) node.get("uploadedChunkIds");
        assertEquals(2, chunkIds.size());
        assertEquals("0", chunkIds.get(0).getValueAsText());
        assertEquals("1", chunkIds.get(1).getValueAsText());
        assertEquals("2", node.get("chunkCount").getValueAsText());

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
            response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/0/execute/Blob.Attach", json, headers);
        } else {
            response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/0/execute/Blob.Attach", json);
        }
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

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
     * @since 7.4
     */
    @Test
    public void testCancelBatch() throws IOException {

        // Init batch
        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        // Cancel batch
        response = getResponse(RequestType.DELETE, "upload/" + batchId);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    /**
     * @since 7.4
     */
    @Test
    public void testEmptyResponseCases() throws IOException {

        // Upload
        assertEquals(Status.NOT_FOUND.getStatusCode(),
                getResponse(RequestType.POST, "upload/fakeBatchId/0").getStatus());

        // Get batch info
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResponse(RequestType.GET, "upload/fakeBatchId").getStatus());
        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();
        assertEquals(Status.NO_CONTENT.getStatusCode(), getResponse(RequestType.GET, "upload/" + batchId).getStatus());

        // Get file info
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResponse(RequestType.GET, "upload/fakeBatchId/0").getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(),
                getResponse(RequestType.GET, "upload/" + batchId + "/0").getStatus());

        // Cancel batch
        assertEquals(Status.NOT_FOUND.getStatusCode(),
                getResponse(RequestType.DELETE, "upload/fakeBatchId").getStatus());
    }

    /**
     * @since 7.10
     */
    @Test
    public void testBadRequests() throws IOException {
        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        // Bad file index
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                getResponse(RequestType.POST, "upload/" + batchId + "/a").getStatus());

        // Bad chunk index
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Count", "2");
        headers.put("X-Upload-Chunk-Index", "a");
        headers.put("X-File-Size", "100");
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                getResponse(RequestType.POST, "upload/" + batchId + "/0", "chunkContent", headers).getStatus());

        // Bad chunk count
        headers = new HashMap<String, String>();
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Count", "a");
        headers.put("X-Upload-Chunk-Index", "0");
        headers.put("X-File-Size", "100");
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                getResponse(RequestType.POST, "upload/" + batchId + "/0", "chunkContent", headers).getStatus());

        // Bad file size
        headers = new HashMap<String, String>();
        headers.put("X-Upload-Type", "chunked");
        headers.put("X-Upload-Chunk-Count", "2");
        headers.put("X-Upload-Chunk-Index", "0");
        headers.put("X-File-Size", "a");
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                getResponse(RequestType.POST, "upload/" + batchId + "/0", "chunkContent", headers).getStatus());
    }

    /**
     * @since 8.4
     */
    @Test
    public void testRemoveFile() throws IOException {
        // Get batch id, used as a session id
        ClientResponse response = getResponse(RequestType.POST, "upload");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();
        assertNotNull(batchId);

        int numfiles = 5;

        // Upload test files
        String fileName, data,
               fileSize = null,
               mimeType = "text/plain";
        Map<String, String> headers = new HashMap<String, String>();
        for (int i = 0; i < numfiles; i++) {
            fileName = URLEncoder.encode("Test File " + Integer.toString(i+1) + ".txt", "UTF-8");
            data = "Test Content " + Integer.toString(i+1);
            if (fileSize == null) {
                fileSize = String.valueOf(data.getBytes().length);
                headers.put("Content-Type", "application/octet-stream");
                headers.put("X-Upload-Type", "normal");
                headers.put("X-File-Size", fileSize);
                headers.put("X-File-Type", mimeType);
            }
            headers.put("X-File-Name", fileName);
            response = getResponse(RequestType.POST, "upload/" + batchId + "/" + Integer.toString(i), data, headers);
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        // Get batch info
        response = getResponse(RequestType.GET, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
        assertEquals(numfiles, nodes.size());

        for (int i = 0; i < numfiles; i++) {
            node = nodes.get(i);
            assertEquals("Test File " + Integer.toString(i+1) + ".txt", node.get("name").getValueAsText());
            assertEquals(fileSize, node.get("size").getValueAsText());
            assertEquals("normal", node.get("uploadType").getValueAsText());
        }

        // remove files #2 and #4
        response = getResponse(RequestType.DELETE, "upload/" + batchId + "/1");
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response = getResponse(RequestType.DELETE, "upload/" + batchId + "/3");
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // check if the remaining files are the correct ones
        response = getResponse(RequestType.GET, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
        assertEquals(numfiles-2, nodes.size());
        node = nodes.get(0);
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

        // test removal of invalid file index
        response = getResponse(RequestType.DELETE, "upload/" + batchId + "/3");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private void assertBatchExists(String batchId) {
        ClientResponse response = getResponse(RequestType.GET, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

}
