/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.client.api.ConstantsV1;
import org.nuxeo.client.api.objects.Operation;
import org.nuxeo.client.api.objects.blob.Blob;
import org.nuxeo.client.api.objects.upload.BatchFile;
import org.nuxeo.client.api.objects.upload.BatchUpload;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ TransientStoreFeature.class, RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class TestUpload extends TestBase {

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanManageBatch() {
        BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
        assertNotNull(batchUpload);
        assertNotNull(batchUpload.getBatchId());
        batchUpload.cancel();
        try {
            batchUpload.fetchBatchFiles();
            fail("Should be not found");
        } catch (NuxeoClientException reason) {
            assertEquals(404, reason.getStatus());
        }
    }

    @Test
    public void itCanUploadFiles() {
        // Upload the file
        BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
        assertNotNull(batchUpload);
        assertNotNull(batchUpload.getBatchId());
        File file = FileUtils.getResourceFileFromContext("sample.jpg");
        batchUpload = batchUpload.upload(file.getName(), file.length(), "jpg", batchUpload.getBatchId(), "1", file);
        assertNotNull(batchUpload);
        assertEquals(ConstantsV1.UPLOAD_NORMAL_TYPE, batchUpload.getUploadType());

        // Check the file in the ref batch
        BatchFile batchFile = batchUpload.fetchBatchFile("1");
        assertNotNull(batchFile);
        assertEquals(file.getName(), batchFile.getName());
        assertEquals(ConstantsV1.UPLOAD_NORMAL_TYPE, batchFile.getUploadType());

        // Upload another file and check files
        file = FileUtils.getResourceFileFromContext("blob.json");
        batchUpload.upload(file.getName(), file.length(), "json", batchUpload.getBatchId(), "2", file);
        List<BatchFile> batchFiles = batchUpload.fetchBatchFiles();
        assertNotNull(batchFiles);
        assertEquals(2, batchFiles.size());
        assertEquals("sample.jpg", batchFiles.get(0).getName());
        assertEquals("blob.json", batchFiles.get(1).getName());
    }

    @Test
    public void itCanUploadChunks() {
        // Upload file chunks
        BatchUpload batchUpload = nuxeoClient.fetchUploadManager().enableChunk();
        assertNotNull(batchUpload);
        File file = FileUtils.getResourceFileFromContext("sample.jpg");
        batchUpload = batchUpload.upload(file.getName(), file.length(), "jpg", batchUpload.getBatchId(), "1", file);
        assertNotNull(batchUpload);
        assertEquals(ConstantsV1.UPLOAD_CHUNKED_TYPE, batchUpload.getUploadType());
        // Check the file
        BatchFile batchFile = batchUpload.fetchBatchFile("1");
        assertNotNull(batchFile);
        assertEquals(file.getName(), batchFile.getName());
        assertEquals(ConstantsV1.UPLOAD_CHUNKED_TYPE, batchFile.getUploadType());
        assertEquals(file.length(), batchFile.getSize());
        assertEquals(4, batchFile.getChunkCount());
        assertEquals(batchFile.getChunkCount(), batchFile.getUploadedChunkIds().length);
    }

    @Test
    public void itCanAttachABatchToADoc() {
        // Upload file chunks
        BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
        assertNotNull(batchUpload);
        File file = FileUtils.getResourceFileFromContext("sample.jpg");
        batchUpload = batchUpload.upload(file.getName(), file.length(), "jpg", batchUpload.getBatchId(), "1", file);
        assertNotNull(batchUpload);

        // Getting a doc and attaching the batch file
        Document doc = new Document("file", "File");
        doc.setPropertyValue("dc:title", "new title");
        doc = nuxeoClient.repository().createDocumentByPath("/folder_1", doc);
        assertNotNull(doc);
        doc.setPropertyValue("file:content", batchUpload.getBatchBlob());
        doc = doc.updateDocument();
        assertEquals("sample.jpg", ((Map) doc.get("file:content")).get("name"));
    }

    @Test
    public void itCanExecuteOp() {
        // Upload file
        BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
        assertNotNull(batchUpload);
        File file = FileUtils.getResourceFileFromContext("sample.jpg");
        batchUpload = batchUpload.upload(file.getName(), file.length(), "jpg", batchUpload.getBatchId(), "1", file);
        assertNotNull(batchUpload);

        // Getting a doc and attaching the batch file
        Document doc = new Document("file", "File");
        doc.setPropertyValue("dc:title", "new title");
        doc = nuxeoClient.repository().createDocumentByPath("/folder_1", doc);
        assertNotNull(doc);
        Operation operation = nuxeoClient.automation("Blob.AttachOnDocument").param("document", doc);
        Blob blob = (Blob) batchUpload.execute(operation);
        assertNotNull(blob);
    }
}