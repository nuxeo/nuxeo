/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public TestExternalBlob(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-externalblob-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-externalblob-adapters-contrib.xml");
    }

    protected File createTempFile() throws Exception {
        File file = File.createTempFile("testExternalBlob", ".txt");
        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Hello External Blob");
        out.close();
        file.deleteOnExit();
        return file;
    }

    public void testExternalBlobAdapter() throws Exception {
        BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
        assertNotNull(service);
        ExternalBlobAdapter adapter = service.getExternalBlobAdapterForPrefix("fs");
        assertNotNull(adapter);
        assertEquals("fs", adapter.getPrefix());
        assertTrue(adapter instanceof FileSystemExternalBlobAdapter);
        assertEquals(
                "/tmp/",
                adapter.getProperty(FileSystemExternalBlobAdapter.CONTAINER_PROPERTY_NAME));

        File file = createTempFile();
        String uri = String.format("fs:%s", file.getName());
        ExternalBlobAdapter otherAdapter = service.getExternalBlobAdapterForUri(uri);
        assertEquals(otherAdapter, adapter);

        Blob blob = service.getExternalBlobForUri(uri);
        assertNotNull(blob);
        assertEquals("Hello External Blob", blob.getString());
        assertEquals(file.getName(), (blob).getFilename());
    }

    public void testExternalBlobDocumentProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        HashMap<String, String> map = new HashMap<String, String>();
        String uri = String.format("fs:%s", file.getName());
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
    public void testExternalBlobDocumentProperty2() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        String uri = String.format("fs:%s", file.getName());
        doc.setPropertyValue("extfile:content/uri", uri);

        Object blob = doc.getPropertyValue("extfile:content");
        assertNotNull(blob);
        assertTrue(blob instanceof Blob);
        assertEquals("Hello External Blob", ((Blob) blob).getString());
        assertEquals(file.getName(), ((Blob) blob).getFilename());
        // filename not set on property => return null
        assertEquals(null, doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
    }

    // test update of blob properties
    public void testExternalBlobDocumentPropertyUpdate() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ExternalBlobDoc");
        File file = createTempFile();
        HashMap<String, String> map = new HashMap<String, String>();
        String uri = String.format("fs:%s", file.getName());
        map.put("uri", uri);
        doc.setPropertyValue("extfile:content/uri", uri);

        Object blobValue = doc.getPropertyValue("extfile:content");
        assertNotNull(blobValue);
        assertTrue(blobValue instanceof Blob);
        Blob blob = (Blob) blobValue;
        assertEquals("Hello External Blob", blob.getString());
        assertEquals(file.getName(), blob.getFilename());
        // filename not set on property => return null
        assertEquals(null, doc.getPropertyValue("extfile:content/name"));
        assertEquals(uri, doc.getPropertyValue("extfile:content/uri"));
        assertEquals(null, doc.getPropertyValue("extfile:content/mime-type"));

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
