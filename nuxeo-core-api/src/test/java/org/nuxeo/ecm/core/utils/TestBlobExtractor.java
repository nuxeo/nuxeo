/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestBlobExtractor extends NXRuntimeTestCase {

    static final Log log = LogFactory.getLog(TestBlobExtractor.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests",
        "OSGI-INF/test-propmodel-types-contrib.xml");
    }

    @Test
    public void testCaching() throws Exception {

        BlobsExtractor bec = new BlobsExtractor();

        Map<String, List<String>> paths = bec.getBlobFieldPathForDocumentType("NoBlobDocument");
        assertEquals(0, paths.size());

        paths = bec.getBlobFieldPathForDocumentType("SimpleBlobDocument");
        assertEquals(1, paths.size());
        assertEquals("simpleblob", paths.keySet().toArray()[0]);
        assertEquals(1, paths.get("simpleblob").size());
        assertEquals("/blob", paths.get("simpleblob").get(0));

        paths = bec.getBlobFieldPathForDocumentType("WithoutPrefixDocument");
        assertEquals(1, paths.size());
        assertEquals("wihtoutpref", paths.keySet().toArray()[0]);
        assertEquals(1, paths.get("wihtoutpref").size());
        assertEquals("/blob", paths.get("wihtoutpref").get(0));

        paths = bec.getBlobFieldPathForDocumentType("BlobInListDocument");
        assertEquals(1, paths.size());
        assertEquals("blobinlist", paths.keySet().toArray()[0]);
        assertEquals(1, paths.get("blobinlist").size());
        assertEquals("/files/*/file", paths.get("blobinlist").get(0));
    }

    @Test
    public void testGetBlobsFromDocumentModelNoBlob() throws Exception {
        BlobsExtractor bec = new BlobsExtractor();

        DocumentModel noBlob = new DocumentModelImpl("/",
                "testNoBlob", "NoBlobDocument");
        noBlob.setProperty("dublincore", "title", "NoBlobDocument");

        List<Property> blobProperties = bec.getBlobsProperties(noBlob);
        assertEquals(0, blobProperties.size());
    }

    @Test
    public void testGetBlobsFromDocumentModelSimpleBlob() throws Exception {
        BlobsExtractor bec = new BlobsExtractor();

        DocumentModel simpleBlob = new DocumentModelImpl("/",
                "testSimpleBlob", "SimpleBlobDocument");
        simpleBlob.setProperty("dublincore", "title", "SimpleBlobDocument");
        simpleBlob.setProperty("simpleblob", "blob", createTestBlob(false,
                "test.pdf"));


        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(simpleBlob);
        assertEquals(1, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test.pdf", blob.getFilename());
    }

    @Test
    public void testGetBlobsFromDocumentModelSimpleBlobWithoutPrefix() throws Exception {
        BlobsExtractor bec = new BlobsExtractor();

        DocumentModel simpleBlob = new DocumentModelImpl("/",
                "testSimpleBlob", "WithoutPrefixDocument");
        simpleBlob.setProperty("dublincore", "title", "WithoutPrefixDocument");
        simpleBlob.setProperty("wihtoutpref", "blob", createTestBlob(false,
                "test.pdf"));

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(simpleBlob);
        assertEquals(1, blobs.size());
        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test.pdf", blob.getFilename());
    }

    @Test
    public void testGetBlobsFromBlobInList() throws Exception {
        BlobsExtractor bec = new BlobsExtractor();

        DocumentModel blobInListEmpty = new DocumentModelImpl(
                "/", "testBlobInListDocumentEmpty", "BlobInListDocument");

        DocumentModel blobInListWithBlobs = new DocumentModelImpl(
                "/", "testBlobInListDocument1", "BlobInListDocument");
        blobInListWithBlobs.setProperty("dublincore", "title",
                "BlobInListDocument");
        List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();

        Map<String, Object> blob1Map = new HashMap<String, Object>();
        blob1Map.put("file", createTestBlob(false, "test1.pdf"));
        blob1Map.put("filename", "test1.pdf");

        Map<String, Object> blob2Map = new HashMap<String, Object>();
        blob2Map.put("file", createTestBlob(false, "test2.pdf"));
        blob2Map.put("filename", "test2.pdf");

        files.add(blob1Map);
        files.add(blob2Map);
        blobInListWithBlobs.setPropertyValue("bil:files", (Serializable) files);

        // END INITIALIZATION

        List<Property> blobs = bec.getBlobsProperties(blobInListEmpty);
        assertEquals(0, blobs.size());

        blobs = bec.getBlobsProperties(blobInListWithBlobs);
        assertEquals(2, blobs.size());

        Blob blob = (Blob) blobs.get(0).getValue();
        assertEquals("test1.pdf", blob.getFilename());

        blob = (Blob) blobs.get(1).getValue();
        assertEquals("test2.pdf", blob.getFilename());
    }

    @Test
    public void testGetBlobsFromTwoSchemas() throws Exception {
        BlobsExtractor bec = new BlobsExtractor();

        DocumentModel doc = new DocumentModelImpl(
                "/", "testDoc", "BlobWithTwoSchemasContainingBlob");

        doc.setProperty("dublincore", "title",
                "doc");
        doc.setProperty("simpleblob", "blob", createTestBlob(false,
                "test1.pdf"));
        doc.setProperty("simpleblob2", "blob", createTestBlob(false,
                "test2.pdf"));

        List<Property> blobs = bec.getBlobsProperties(doc);
        assertEquals(2, blobs.size());

        Blob blob = (Blob) blobs.get(0).getValue();
        Blob blob2 = (Blob) blobs.get(1).getValue();

        if ("test1.pdf".equals(blob.getFilename()) && "test2.pdf".equals(blob2.getFilename())) {
            return;
        }
        if ("test2.pdf".equals(blob.getFilename()) && "test1.pdf".equals(blob2.getFilename())) {
            return;
        }
        log.error("Blob detected : \"" + blob.getFilename() + "\" and \"" + blob2.getFilename() + "\"");
        log.error("Waiting Values test2.pdf and test1.pdf");
        fail();
    }

    @Test
    public void testGetTwoBlobsFromOneSchema() throws Exception {
        BlobsExtractor bec = new BlobsExtractor();

        DocumentModel doc = new DocumentModelImpl(
                "/", "testDoc", "BlobWithOneSchemaContainingTwoBlobs");

        doc.setProperty("dublincore", "title",
                "doc");
        doc.setProperty("simpleblob3", "blob", createTestBlob(false,
                "test1.pdf"));
        doc.setProperty("simpleblob3", "blob2", createTestBlob(false,
                "test2.pdf"));

        List<Property> blobs = bec.getBlobsProperties(doc);
        assertEquals(2, blobs.size());

        Blob blob = (Blob) blobs.get(0).getValue();
        Blob blob2 = (Blob) blobs.get(1).getValue();

        if ("test1.pdf".equals(blob.getFilename()) && "test2.pdf".equals(blob2.getFilename())) {
            return;
        }
        if ("test2.pdf".equals(blob.getFilename()) && "test1.pdf".equals(blob2.getFilename())) {
            return;
        }
        log.error("Blob detected : \"" + blob.getFilename() + "\" and \"" + blob2.getFilename() + "\"");
        log.error("Waiting Values test2.pdf and test1.pdf");
        fail();
    }

    protected Blob createTestBlob(boolean setMimeType, String filename) {
        Blob blob = new StringBlob("SOMEDUMMYDATA");
        blob.setFilename(filename);
        if (setMimeType) {
            blob.setMimeType("application/pdf");
        }
        return blob;
    }

}
