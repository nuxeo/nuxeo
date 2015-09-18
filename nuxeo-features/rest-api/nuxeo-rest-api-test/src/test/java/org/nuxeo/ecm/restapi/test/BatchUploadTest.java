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
 *     ataillefer
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
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
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchResource;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.BatchUploadObject;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

/**
 * Tests the {@link BatchUploadObject} endpoints.
 *
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:multiblob-doctype.xml")
public class BatchUploadTest extends BaseTest {

    @Inject
    CoreSession session;

    /**
     * Tests the deprecated /batch/upload endpoint.
     *
     * @deprecated since 7.4
     * @see BatchResource#doPost(HttpServletRequest)
     * @see #itCanUseBatchUpload()
     */
    @Deprecated
    @Test
    public void itCanUseBatchResource() throws Exception {
        String batchId = "batch_" + Math.random();
        String filename = "testfile";
        String data = "batchUploadedData";

        // upload the file in automation
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Batch-Id", batchId);
        headers.put("X-File-Idx", "0");
        headers.put("X-File-Name", filename);
        FormDataMultiPart form = new FormDataMultiPart();
        BodyPart fdp = new StreamDataBodyPart(filename, new ByteArrayInputStream(data.getBytes()));
        form.bodyPart(fdp);
        getResponse(RequestType.POST, "automation/batch/upload", form, headers);
        form.close();

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
        ClientResponse response = getResponse(RequestType.POST, "path/", json);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchUploadDoc"));
        Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
        assertNotNull(blob);
        assertEquals(data, new String(blob.getByteArray()));
    }

    /**
     * Tests the /upload endpoints.
     */
    @Test
    public void itCanUseBatchUpload() throws IOException {

        // Get batch id, used as a session id
        ClientResponse response = getResponse(RequestType.POST, "upload");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
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
        headers.put("Content-Length", fileSize1);
        headers.put("X-File-Name", fileName1);
        headers.put("X-File-Size", fileSize1);
        headers.put("X-File-Type", mimeType);

        response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data1, headers);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("0", node.get("fileIdx").getValueAsText());
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
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("1", node.get("fileIdx").getValueAsText());
        assertEquals(fileSize2, node.get("uploadedSize").getValueAsText());

        // Get batch info
        response = getResponse(RequestType.GET, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ArrayNode nodes = (ArrayNode) mapper.readTree(response.getEntityInputStream());
        assertEquals(2, nodes.size());
        node = nodes.get(0);
        assertEquals("Fichier accentué 1.txt", node.get("name").getValueAsText());
        assertEquals(fileSize1, node.get("size").getValueAsText());
        node = nodes.get(1);
        assertEquals("Fichier accentué 2.txt", node.get("name").getValueAsText());
        assertEquals(fileSize2, node.get("size").getValueAsText());

        // Get file info
        response = getResponse(RequestType.GET, "upload/" + batchId + "/0");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(fileSize1, node.get("uploadedSize").getValueAsText());

        response = getResponse(RequestType.GET, "upload/" + batchId + "/1");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(fileSize2, node.get("uploadedSize").getValueAsText());

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
        assertEquals(data1, new String(blob.getByteArray()));
        blob = (Blob) doc.getPropertyValue("mb:blobs/1/content");
        assertNotNull(blob);
        assertEquals("Fichier accentué 2.txt", blob.getFilename());
        assertEquals(data2, new String(blob.getByteArray()));
    }

    /**
     * Tests the use of /upload + /batch/execute.
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
        headers.put("Content-Length", fileSize);
        headers.put("X-File-Name", fileName);
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", mimeType);
        getResponse(RequestType.POST, "upload/" + batchId + "/0", data, headers);

        // Create a doc and attach the uploaded blob to it using the /batch/execute endpoint
        DocumentModel file = session.createDocumentModel("/", "testBatchExecuteDoc", "File");
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        String json = "{\"params\":{";
        json += "\"operationId\":\"Blob.Attach\",";
        json += "\"batchId\":\"" + batchId + "\",";
        json += "\"fileIdx\":\"0\",";
        json += "\"document\":\"" + file.getPathAsString() + "\"";
        json += "}}";
        response = getResponse(RequestType.POSTREQUEST, "batch/execute", json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/testBatchExecuteDoc"));
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("Fichier accentué.txt", blob.getFilename());
        assertEquals(data, new String(blob.getByteArray()));
    }

    @Test
    public void testDropBatch() throws IOException {

        // Init batch
        ClientResponse response = getResponse(RequestType.POST, "upload");
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        String batchId = node.get("batchId").getValueAsText();

        // Drop batch
        response = getResponse(RequestType.DELETE, "upload/" + batchId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(batchId, node.get("batchId").getValueAsText());
        assertEquals("true", node.get("dropped").getValueAsText());
    }

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

        // Drop batch
        assertEquals(Status.NOT_FOUND.getStatusCode(),
                getResponse(RequestType.DELETE, "upload/fakeBatchId").getStatus());
    }
}
