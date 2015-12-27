/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * Tests file upload with the {@link BatchResource}.
 * <p>
 * Uses {@link URLConnection}.
 *
 * @author Antoine Taillefer
 * @deprecated since 7.4
 * @see {@link BatchUploadTest}
 */
@Deprecated
@RunWith(FeaturesRunner.class)
@Features({ TransientStoreFeature.class, EmbeddedAutomationServerFeature.class })
@Jetty(port = 18080)
public class TestBatchResource {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected Session clientSession;

    protected ObjectMapper mapper;

    protected String uploadURL = "http://localhost:18080/automation/batch/upload";

    protected String fileIndex = "0";

    protected String fileName = "New file.txt";

    protected String mimeType = "text/plain";

    protected String content = "This is the content of a new file.";

    @Before
    public void doBefore() throws Exception {
        mapper = new ObjectMapper();
    }

    @Test(expected = NuxeoException.class)
    public void testBatchUploadClientGeneratedIdNotAllowed() throws IOException {
        String batchId = UUID.randomUUID().toString();
        batchUpload(uploadURL, batchId, fileIndex, fileName, mimeType, content);
    }

    @Test
    public void testBatchUploadClientGeneratedIdAllowed() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.automation.test.test",
                "test-batchmanager-client-generated-id-allowed-contrib.xml");
        String batchId = UUID.randomUUID().toString();
        String responseBatchId = batchUpload(uploadURL, batchId, fileIndex, fileName, mimeType, content);
        assertEquals(batchId, responseBatchId);
        harness.undeployContrib("org.nuxeo.ecm.automation.test.test",
                "test-batchmanager-client-generated-id-allowed-contrib.xml");
    }

    @Test
    public void testBatchUploadServerGeneratedId() throws IOException {
        String batchId = Framework.getService(BatchManager.class).initBatch();
        assertEquals(batchId, batchUpload(uploadURL, batchId, fileIndex, fileName, mimeType, content));
    }

    @Test
    public void testBatchUpload() throws Exception {

        // Create a File document
        DocumentModel file = session.createDocumentModel("/", "testFile", "File");
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Upload a blob and attach it to the document
        String executeURL = "http://localhost:18080/automation/batch/execute";
        String docPath = file.getPathAsString();
        String batchId = batchUpload(uploadURL, null, fileIndex, fileName, mimeType, content);
        batchExecuteAttachBlob(executeURL, batchId, fileIndex, docPath);

        // Get blob from document and check its content
        Blob blob = (Blob) clientSession.newRequest(GetDocumentBlob.ID).setInput(file.getPathAsString()).execute();
        assertNotNull(blob);
        String blobString = new String(IOUtils.toByteArray(blob.getStream()));
        assertEquals("This is the content of a new file.", blobString);
    }

    protected String batchUpload(String urlStr, String batchId, String fileIndex, String fileName, String mimeType,
            String content) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            // Set request headers
            byte[] bytes = content.getBytes();
            String fileSize = Integer.toString(bytes.length);
            conn.setRequestProperty("Authorization", getAuthHeader("Administrator", "Administrator"));
            if (batchId != null) {
                conn.setRequestProperty("X-Batch-Id", batchId);
            }
            conn.setRequestProperty("X-File-Idx", fileIndex);
            conn.setRequestProperty("X-File-Name", fileName);
            conn.setRequestProperty("X-File-Size", fileSize);
            conn.setRequestProperty("X-File-Type", mimeType);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", fileSize);
            // Write bytes
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                IOUtils.write(content, os);
            }
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new NuxeoException("Batch upload request failed with status code " + conn.getResponseCode());
            }
            // Read response and return batch id
            try (InputStream is = conn.getInputStream()) {
                JsonNode node = mapper.readTree(is);
                return node.get("batchId").getValueAsText();
            }
        } finally {
            conn.disconnect();
        }
    }

    protected boolean batchExecuteAttachBlob(String urlStr, String batchId, String fileIndex, String docPath)
            throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            // Set request headers
            conn.setRequestProperty("Authorization", getAuthHeader("Administrator", "Administrator"));
            conn.setRequestProperty("Content-Type", "application/json+nxrequest");
            conn.setRequestProperty("Accept", "application/json+nxentity, */*");
            conn.setRequestProperty("X-NXDocumentProperties", "*");
            // Write JSON data
            conn.setDoOutput(true);
            String JSONData = String.format(
                    "{\"params\": {\"operationId\": \"%s\", \"batchId\": \"%s\", \"fileIdx\": \"%s\", \"document\": \"%s\"}}",
                    "Blob.Attach", batchId, fileIndex, docPath);
            try (OutputStream os = conn.getOutputStream()) {
                IOUtils.write(JSONData, os);
            }
            // Consume response and return true if OK
            try (InputStream is = conn.getInputStream()) {
                IOUtils.toByteArray(is);
                return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            }
        } finally {
            conn.disconnect();
        }
    }

    protected String getAuthHeader(String userName, String password) {
        return "Basic " + new String(Base64.encodeBase64((userName + ":" + password).getBytes()));
    }

}
