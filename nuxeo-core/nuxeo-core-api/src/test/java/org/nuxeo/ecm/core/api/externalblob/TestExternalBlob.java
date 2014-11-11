/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.externalblob;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 *
 */
public class TestExternalBlob extends NXRuntimeTestCase {

    public static String TEMP_DIRECTORY_NAME = "testExternalBlobDir";

    public TestExternalBlob() {
        super();
    }

    public TestExternalBlob(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-externalblob-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-externalblob-adapters-contrib.xml");

        // set container to temp directory here in case that depends on the OS
        // or machine configuration and add funny characters to avoid problems
        // due to xml parsing
        BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
        assertNotNull(service);
        ExternalBlobAdapter adapter = service.getExternalBlobAdapterForPrefix("fs");
        Map<String, String> props = new HashMap<String, String>();
        props.put(FileSystemExternalBlobAdapter.CONTAINER_PROPERTY_NAME, "\n"
                + System.getProperty("java.io.tmpdir") + " ");
        adapter.setProperties(props);
    }

    protected File createTempFile() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"),
                TEMP_DIRECTORY_NAME);
        if (!tempDir.exists()) {
            tempDir.mkdir();
            tempDir.deleteOnExit();
        }
        File file = File.createTempFile("testExternalBlob", ".txt", tempDir);
        file.deleteOnExit();
        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Hello External Blob");
        out.close();
        file.deleteOnExit();
        return file;
    }

    protected String getTempFileUri(File tempFile) {
        return String.format("fs:%s%s%s", TEMP_DIRECTORY_NAME, File.separator,
                tempFile.getName());
    }

    @Test
    public void testExternalBlobAdapter() throws Exception {
        BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
        assertNotNull(service);
        ExternalBlobAdapter adapter = service.getExternalBlobAdapterForPrefix("fs");
        assertNotNull(adapter);
        assertEquals("fs", adapter.getPrefix());
        assertTrue(adapter instanceof FileSystemExternalBlobAdapter);
        assertEquals(
                System.getProperty("java.io.tmpdir"),
                adapter.getProperty(FileSystemExternalBlobAdapter.CONTAINER_PROPERTY_NAME));

        File file = createTempFile();
        String uri = getTempFileUri(file);
        ExternalBlobAdapter otherAdapter = service.getExternalBlobAdapterForUri(uri);
        assertEquals(otherAdapter, adapter);

        Blob blob = service.getExternalBlobForUri(uri);
        assertNotNull(blob);
        assertEquals("Hello External Blob", blob.getString());
        assertEquals(file.getName(), (blob).getFilename());
    }

    @Test
    public void testExternalBlobDocumentProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        HashMap<String, String> map = new HashMap<String, String>();
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
        Map<String, String> map = new HashMap<String, String>();
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
        assertEquals(file.getName(),
                doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
        assertEquals("text", doc.getPropertyValue("extfile:content/mime-type"));
    }

}
