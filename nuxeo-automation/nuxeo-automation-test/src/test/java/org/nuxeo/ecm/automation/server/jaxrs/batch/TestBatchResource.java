/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests file upload with the {@link BatchResource}.
 * <p>
 * Uses {@link URLConnection}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@Jetty(port = 18080)
public class TestBatchResource {

    @Inject
    protected CoreSession session;

    @Inject
    protected Session clientSession;

    @Test
    public void testBatchUpload() throws Exception {

        // Create a File document
        DocumentModel file = session.createDocumentModel("/", "testFile",
                "File");
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Upload a blob and attach it to the document
        String uploadURL = "http://localhost:18080/automation/batch/upload";
        String executeURL = "http://localhost:18080/automation/batch/execute";
        String batchId = UUID.randomUUID().toString();
        String fileIndex = "0";
        String fileName = "New file.txt";
        String mimeType = "text/plain";
        String content = "This is the content of a new file.";
        String docPath = file.getPathAsString();
        if (batchUpload(uploadURL, batchId, fileIndex, fileName, mimeType,
                content)) {
            batchExecuteAttachBlob(executeURL, batchId, fileIndex, docPath);
        } else {
            fail("File upload failed");
        }

        // Get blob from document and check its content
        Blob blob = (Blob) clientSession.newRequest(GetDocumentBlob.ID).setInput(
                file.getPathAsString()).execute();
        assertNotNull(blob);
        String blobString = new String(IOUtils.toByteArray(blob.getStream()));
        assertEquals("This is the content of a new file.", blobString);
    }

    protected boolean batchUpload(String urlStr, String batchId,
            String fileIndex, String fileName, String mimeType, String content)
            throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            // Set request headers
            byte[] bytes = content.getBytes();
            String fileSize = Integer.toString(bytes.length);
            conn.setRequestProperty("Authorization",
                    getAuthHeader("Administrator", "Administrator"));
            conn.setRequestProperty("X-Batch-Id", batchId);
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
            // Consume response and return true if OK
            try (InputStream is = conn.getInputStream()) {
                IOUtils.toByteArray(is);
                return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            }
        } finally {
            conn.disconnect();
        }
    }

    protected boolean batchExecuteAttachBlob(String urlStr, String batchId,
            String fileIndex, String docPath) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            // Set request headers
            conn.setRequestProperty("Authorization",
                    getAuthHeader("Administrator", "Administrator"));
            conn.setRequestProperty("Content-Type",
                    "application/json+nxrequest");
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
        return "Basic "
                + new String(
                        Base64.encodeBase64((userName + ":" + password).getBytes()));
    }

}
