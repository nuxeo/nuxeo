/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
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
        headers.put("X-File-Type", mimeType);

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
        response = getResponse(RequestType.POST, "path/", json);
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
        assertEquals(mimeType, blob.getMimeType());
        assertEquals(data2, blob.getString());
    }

    /**
     * tests if the X-File-Type header is obeyed on multipart file upload
     * (NXP-22408)
     */
    @Test
    public void testObeyFileTypeHeader() throws IOException {
        // Get batch id, used as a session id
        ClientResponse response = getResponse(RequestType.POST, "upload");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();
        assertNotNull(batchId);

        String mimeType = "text/plain";

        // Upload a file in multipart, first without the X-File-Type header, the second with
        String fileName1 = "No header.txt";
        String data1 = "File without explicit file type";
        String fileSize1 = String.valueOf(data1.getBytes().length);
        Map<String, String> headers = new HashMap<String, String>();
        headers = new HashMap<String, String>();
        headers.put("X-File-Size", fileSize1);

        FormDataMultiPart form = new FormDataMultiPart();
        BodyPart fdp = new StreamDataBodyPart(fileName1, new ByteArrayInputStream(data1.getBytes()));
        form.bodyPart(fdp);
        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", form, headers);
        form.close();
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        String fileName2 = "With header.txt";
        String data2 = "File with explicit X-File-Type header";
        String fileSize2 = String.valueOf(data2.getBytes().length);
        headers = new HashMap<String, String>();
        headers.put("X-File-Size", fileSize2);
        headers.put("X-File-Type", mimeType);

        form = new FormDataMultiPart();
        fdp = new StreamDataBodyPart(fileName2, new ByteArrayInputStream(data2.getBytes()));
        form.bodyPart(fdp);
        response = getResponse(RequestType.POST, "upload/" + batchId + "/1", form, headers);
        form.close();
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

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

        response = getResponse(RequestType.POST, "path/", json);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

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
        response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/0/execute/Blob.Attach", json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchExecuteDoc"));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("Fichier accentué.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(data, blob.getString());
    }

    /**
     * @since 8.1O
     */
    @Test
    public void testBatchExecuteAutomationServerBindings() throws IOException {

        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        File file = File.createTempFile("nx-test-blob-", ".tmp");
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
        response = getResponse(RequestType.POSTREQUEST, "upload/" + batchId + "/0/execute/Blob.Attach", json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchExecuteDoc"));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("Fichier accentué.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("Contenu accentué composé de 2 chunks", blob.getString());
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

}
