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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.Documents;
import org.nuxeo.client.api.objects.Operation;
import org.nuxeo.client.api.objects.blob.Blob;
import org.nuxeo.client.api.objects.blob.Blobs;
import org.nuxeo.client.api.objects.operation.DocRef;
import org.nuxeo.client.api.objects.operation.DocRefs;
import org.nuxeo.client.internals.spi.NuxeoClientException;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.common.io.Files;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class TestOperation extends TestBase {

    public static final String FOLDER_2_FILE = "/folder_2/file";

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanExecuteOperationOnDocument() {
        Document result = nuxeoClient.automation().param("value", "/").execute("Repository.GetDocument");
        assertNotNull(result);
    }

    @Test
    public void itCanExecuteOperationOnDocuments() {
        Operation operation = nuxeoClient.automation("Repository.Query").param("query", "SELECT * " + "FROM Document");
        Documents result = operation.execute();
        assertNotNull(result);
        assertTrue(result.getTotalSize() != 0);
    }

    @Test
    public void itCanExecuteOperationWithBlobs() throws IOException {
        // Get a blob
        Document result = nuxeoClient.automation().param("value", FOLDER_2_FILE).execute("Repository.GetDocument");
        Blob blob = nuxeoClient.automation().input(result).execute("Document.GetBlob");
        assertNotNull(blob);
        List<String> lines = Files.readLines(blob.getFile(), Charset.defaultCharset());
        assertEquals("[", lines.get(0));
        assertEquals("    \"fieldType\": \"string\",", lines.get(2));
        // Attach a blob
        File temp1 = FileUtils.getResourceFileFromContext("sample.jpg");
        Blob fileBlob = new Blob(temp1);
        int length = fileBlob.getLength();
        blob = nuxeoClient.automation()
                          .newRequest("Blob.AttachOnDocument")
                          .param("document", FOLDER_2_FILE)
                          .input(fileBlob)
                          .execute();
        assertNotNull(blob);
        assertEquals(length, blob.getLength());
        Blob resultBlob = nuxeoClient.automation().input(FOLDER_2_FILE).execute("Document.GetBlob");
        assertNotNull(resultBlob);
        assertEquals(length, resultBlob.getLength());
        // Attach a blobs and get them
        File temp2 = FileUtils.getResourceFileFromContext("sample.jpg");
        Blobs inputBlobs = new Blobs();
        inputBlobs.add(temp1);
        inputBlobs.add(temp2);
        Blobs blobs = nuxeoClient.automation()
                                 .newRequest("Blob.AttachOnDocument")
                                 .param("document", FOLDER_2_FILE)
                                 .param("xpath", "files:files")
                                 .input(inputBlobs)
                                 .execute();
        assertNotNull(blobs);
        Blobs resultBlobs = nuxeoClient.automation().input(FOLDER_2_FILE).execute("Document.GetBlobs");
        assertNotNull(resultBlobs);
        assertEquals(3, resultBlobs.size());
    }

    @Test
    public void testMultiThread() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Document result = nuxeoClient.automation().param("value", "/").execute("Repository.GetDocument");
                assertNotNull(result);
            } catch (Exception e) {
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                Document result = nuxeoClient.automation().param("value", "/").execute("Repository.GetDocument");
                assertNotNull(result);
            } catch (Exception e) {
            }
        });
        t.start();
        t2.start();
        t.join();
        t2.join();
    }

    @Test
    public void itCanExecuteOperationOnVoid() {
        try {
            nuxeoClient.automation()
                       .newRequest("Log")
                       .param("message", "Error Log Test")
                       .param("level", "error")
                       .execute();
        } catch (NuxeoClientException reason) {
            fail("Void operation failing");
        }
    }

    @Test
    public void itCanExecuteOperationWithDocumentRefs() {
        Document result = nuxeoClient.automation().param("value", "/").execute("Repository.GetDocument");
        assertNotNull(result);
        DocRef doc = new DocRef(result.getId());
        result = nuxeoClient.automation().input(doc).param("properties", null).execute("Document.Update");
        assertNotNull(result);
        DocRefs docRefs = new DocRefs();
        docRefs.addDoc(doc);
        result = nuxeoClient.automation().input(docRefs).param("properties", null).execute("Document.Update");
        assertNotNull(result);
    }
}