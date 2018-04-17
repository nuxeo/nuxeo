/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.externalblob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-externalblob-types-contrib.xml")
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-externalblob-adapters-contrib.xml")
public class TestExternalBlob {

    public static String TEMP_DIRECTORY_NAME = "testExternalBlobDir";

    @Before
    public void postSetUp() throws Exception {
        // set container to temp directory here in case that depends on the OS
        // or machine configuration and add funny characters to avoid problems
        // due to xml parsing
        BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
        assertNotNull(service);
        ExternalBlobAdapter adapter = service.getExternalBlobAdapterForPrefix("fs");
        Map<String, String> props = new HashMap<>();
        props.put(FileSystemExternalBlobAdapter.CONTAINER_PROPERTY_NAME,
                "\n" + Environment.getDefault().getTemp().getPath() + " ");
        adapter.setProperties(props);
    }

    protected File createTempFile() throws Exception {
        File tempDir = new File(Environment.getDefault().getTemp(), TEMP_DIRECTORY_NAME);
        tempDir.mkdirs();
        Framework.trackFile(tempDir, this);
        File file = File.createTempFile("testExternalBlob", ".txt", tempDir);
        Framework.trackFile(file, file);
        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Hello External Blob");
        out.close();
        return file;
    }

    protected String getTempFileUri(File tempFile) {
        return String.format("fs:%s%s%s", TEMP_DIRECTORY_NAME, File.separator, tempFile.getName());
    }

    @Test
    public void testExternalBlobAdapter() throws Exception {
        BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
        assertNotNull(service);
        ExternalBlobAdapter adapter = service.getExternalBlobAdapterForPrefix("fs");
        assertNotNull(adapter);
        assertEquals("fs", adapter.getPrefix());
        assertTrue(adapter instanceof FileSystemExternalBlobAdapter);
        assertEquals(Environment.getDefault().getTemp().getPath(),
                adapter.getProperty(FileSystemExternalBlobAdapter.CONTAINER_PROPERTY_NAME));

        File file = createTempFile();
        String uri = getTempFileUri(file);
        ExternalBlobAdapter otherAdapter = service.getExternalBlobAdapterForUri(uri);
        assertEquals(otherAdapter, adapter);

        Blob blob = service.getExternalBlobForUri(uri);
        assertNotNull(blob);
        assertEquals("Hello External Blob", blob.getString());
        assertEquals(file.getName(), blob.getFilename());
    }

    @Test
    public void testExternalBlobDocumentProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        HashMap<String, String> map = new HashMap<>();
        String uri = getTempFileUri(file);
        map.put(ExternalBlobProperty.URI, uri);
        map.put(ExternalBlobProperty.FILE_NAME, "hello.txt");
        doc.setPropertyValue("extfile:content", map);

        Object blob = doc.getPropertyValue("extfile:content");
        assertNotNull(blob);
        assertTrue(blob instanceof Blob);
        assertEquals("Hello External Blob", ((Blob) blob).getString());
        assertEquals("hello.txt", ((Blob) blob).getFilename());
        assertEquals("hello.txt", doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
    }

    // this time only set the uri
    @Test
    public void testExternalBlobDocumentProperty2() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        String uri = getTempFileUri(file);
        doc.setPropertyValue("extfile:content/uri", uri);

        Object blob = doc.getPropertyValue("extfile:content");
        assertNotNull(blob);
        assertTrue(blob instanceof Blob);
        assertEquals("Hello External Blob", ((Blob) blob).getString());
        assertEquals(file.getName(), ((Blob) blob).getFilename());
        // filename not set on property => return null
        assertNull(doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
    }

    // test update of blob properties
    @Test
    public void testExternalBlobDocumentPropertyUpdate() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        Map<String, String> map = new HashMap<>();
        String uri = getTempFileUri(file);
        map.put("uri", uri);
        doc.setPropertyValue("extfile:content/uri", uri);

        Object blobValue = doc.getPropertyValue("extfile:content");
        assertNotNull(blobValue);
        assertTrue(blobValue instanceof Blob);

        Blob blob = (Blob) blobValue;
        assertEquals("Hello External Blob", blob.getString());
        assertEquals(file.getName(), blob.getFilename());
        // filename not set on property => return null
        assertNull(doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
        assertNull(doc.getPropertyValue("extfile:content/mime-type"));

        // update the blob properties
        blob.setMimeType("text");
        doc.setPropertyValue("extfile:content", (Serializable) blob);

        // test again

        blobValue = doc.getPropertyValue("extfile:content");
        assertNotNull(blobValue);
        assertTrue(blobValue instanceof Blob);

        blob = (Blob) blobValue;
        assertEquals("Hello External Blob", blob.getString());
        assertEquals(file.getName(), blob.getFilename());
        // filename now set on property
        assertEquals(file.getName(), doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
        assertEquals("text", doc.getPropertyValue("extfile:content/mime-type"));
    }

}
