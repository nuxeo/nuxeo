/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestBlobsExtractor extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-propmodel-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-blobsextractor-types-contrib.xml");
    }

    protected Blob createBlob(String filename) {
        return Blobs.createBlob("dummy", null, null, filename);
    }

    @Test
    public void testGetBlobPathsVarious() throws Exception {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        BlobsExtractor blobsExtractor = new BlobsExtractor();
        List<String> paths;

        paths = blobsExtractor.getBlobPaths(schemaManager.getDocumentType("NoBlobDocument"));
        assertEquals(Collections.emptyList(), paths);

        paths = blobsExtractor.getBlobPaths(schemaManager.getDocumentType("SimpleBlobDocument"));
        assertEquals(Arrays.asList("simpleblob:blob"), paths);

        paths = blobsExtractor.getBlobPaths(schemaManager.getDocumentType("WithoutPrefixDocument"));
        assertEquals(Arrays.asList("wihtoutpref:blob"), paths);

        paths = blobsExtractor.getBlobPaths(schemaManager.getDocumentType("BlobInListDocument"));
        assertEquals(Arrays.asList("bil:files/*/file"), paths);
    }

    @Test
    public void testGetBlobsPropertiesNoBlob() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "NoBlobDocument");

        List<Property> blobProperties = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(0, blobProperties.size());
    }

    @Test
    public void testGetBlobsPropertiesSimpleBlob() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "SimpleBlobDocument");
        Blob blob = createBlob("test.pdf");
        doc.setPropertyValue("simpleblob:blob", (Serializable) blob);

        List<Property> blobs = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(1, blobs.size());
        assertEquals("test.pdf", ((Blob) blobs.get(0).getValue()).getFilename());
    }

    @Test
    public void testGetBlobsPropertiesSimpleBlobWithoutPrefix() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "WithoutPrefixDocument");
        Blob blob = createBlob("test.pdf");
        doc.setPropertyValue("wihtoutpref:blob", (Serializable) blob);

        List<Property> blobs = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(1, blobs.size());
        assertEquals("test.pdf", ((Blob) blobs.get(0).getValue()).getFilename());
    }

    @Test
    public void testGetBlobsPropertiesBlobListEmpty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "BlobInListDocument");

        List<Property> blobs = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(0, blobs.size());
    }

    @Test
    public void testGetBlobsPropertiesBlobList() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "BlobInListDocument");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("file", createBlob("test1.pdf"));
        Map<String, Object> map2 = new HashMap<>();
        map2.put("file", createBlob("test2.pdf"));
        doc.setPropertyValue("bil:files", (Serializable) Arrays.asList(map1, map2));
        List<Property> blobs;

        blobs = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(2, blobs.size());
        assertEquals("test1.pdf", ((Blob) blobs.get(0).getValue()).getFilename());
        assertEquals("test2.pdf", ((Blob) blobs.get(1).getValue()).getFilename());
    }

    @Test
    public void testGetBlobsFromTwoSchemas() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "BlobWithTwoSchemasContainingBlob");
        doc.setPropertyValue("simpleblob:blob", (Serializable) createBlob("test1.pdf"));
        doc.setPropertyValue("simpleblob2:blob", (Serializable) createBlob("test2.pdf"));

        List<Property> blobs = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(2, blobs.size());

        Blob blob = (Blob) blobs.get(0).getValue();
        Blob blob2 = (Blob) blobs.get(1).getValue();
        assertEquals(new HashSet<>(Arrays.asList("test1.pdf", "test2.pdf")),
                new HashSet<>(Arrays.asList(blob.getFilename(), blob2.getFilename())));
    }

    @Test
    public void testGetTwoBlobsFromOneSchema() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "BlobWithOneSchemaContainingTwoBlobs");
        doc.setPropertyValue("simpleblob3:blob", (Serializable) createBlob("test1.pdf"));
        doc.setPropertyValue("simpleblob3:blob2", (Serializable) createBlob("test2.pdf"));

        List<Property> blobs = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(2, blobs.size());

        Blob blob = (Blob) blobs.get(0).getValue();
        Blob blob2 = (Blob) blobs.get(1).getValue();
        assertEquals(new HashSet<>(Arrays.asList("test1.pdf", "test2.pdf")),
                new HashSet<>(Arrays.asList(blob.getFilename(), blob2.getFilename())));
    }

    @Test
    public void testGetBlobPaths() throws Exception {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        List<String> blobPaths = new BlobsExtractor().getBlobPaths(schemaManager.getDocumentType("ComplexDoc"));
        assertEquals(
                new HashSet<>(
                        Arrays.asList("file:content", "cmpf:aList/*/content", "cmpf:attachedFile/vignettes/*/content")),
                new HashSet<>(blobPaths));
    }

    @Test
    public void testGetBlobs() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");

        Blob blob1 = createBlob("file1.txt");
        doc.setPropertyValue("file:content", (Serializable) blob1);

        Blob blob2 = createBlob("file2.txt");
        Blob blob3 = createBlob("file3.txt");
        List<Map<String, Object>> listBlobs = Arrays.asList( //
                Collections.singletonMap("content", blob2), //
                Collections.singletonMap("content", blob3));
        doc.setPropertyValue("cmpf:aList", (Serializable) listBlobs);

        Blob blob4 = createBlob("file4.txt");
        Blob blob5 = createBlob("file5.txt");
        List<Map<String, Object>> vignettes = Arrays.asList( //
                Collections.singletonMap("content", blob4), //
                Collections.singletonMap("content", blob5));
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) Collections.singletonMap("vignettes", vignettes));

        BlobsExtractor extractor = new BlobsExtractor();
        List<Blob> blobs = extractor.getBlobs(doc);
        assertEquals(5, blobs.size());
        assertTrue(blobs.contains(blob1));
        assertTrue(blobs.contains(blob2));
        assertTrue(blobs.contains(blob3));
        assertTrue(blobs.contains(blob4));
        assertTrue(blobs.contains(blob5));
    }

    @Test
    public void testGetBlobsEmpty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");
        // don't create file:content
        List<Blob> blobs = new BlobsExtractor().getBlobs(doc);
        assertEquals(0, blobs.size());
    }

    @Test
    public void testGetBlobsPropertiesEmpty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");
        // don't create file:content
        List<Property> properties = new BlobsExtractor().getBlobsProperties(doc);
        assertEquals(0, properties.size());
    }
    @Test
    public void testWithRepositoryConfiguration() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");

        List<Map<String, Object>> vignettes = new ArrayList<>();
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob1 = createBlob("file1.txt");
        vignette.put("content", blob1);
        vignettes.add(vignette);

        vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob2 = createBlob("file2.txt");
        vignette.put("content", blob2);
        vignettes.add(vignette);

        Map<String, Object> attachedFile = new HashMap<String, Object>();
        attachedFile.put("name", "some name");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);

        Blob blob3 = createBlob("file3.txt");
        doc.setPropertyValue("file:content", (Serializable) blob3);

        BlobsExtractor extractor = new BlobsExtractor();
        List<Blob> blobs;

        // only a simple property
        // <field>dc:title</field>
        extractor.setExtractorProperties(null, null, false);
        blobs = extractor.getBlobs(doc);
        assertEquals(0, blobs.size());

        // only blobs
        // <fieldType>blob</fieldType>
        extractor.setExtractorProperties(null, null, true);
        blobs = extractor.getBlobs(doc);
        assertEquals(3, blobs.size());
        assertTrue(blobs.contains(blob1));
        assertTrue(blobs.contains(blob2));
        assertTrue(blobs.contains(blob3));

        // only a blob property with schema prefix
        // <field>cmpf:attachedFile/vignettes/*/content</field>
        Set<String> pathProps = new HashSet<String>();
        pathProps.add("cmpf:attachedFile/vignettes/*/content");
        extractor.setExtractorProperties(pathProps, null, false);
        blobs = extractor.getBlobs(doc);
        assertEquals(2, blobs.size());
        assertTrue(blobs.contains(blob1));
        assertTrue(blobs.contains(blob2));

        // only the blob of file (no schema prefix)
        // <field>content</field>
        pathProps = new HashSet<String>();
        pathProps.add("content");
        extractor.setExtractorProperties(pathProps, null, false);
        blobs = extractor.getBlobs(doc);
        assertEquals(1, blobs.size());
        assertTrue(blobs.contains(blob3));

        // only the blob of file (with prefix when schema defined without prefix)
        // <field>file:content</field>
        pathProps = new HashSet<String>();
        pathProps.add("file:content");
        extractor.setExtractorProperties(pathProps, null, false);
        blobs = extractor.getBlobs(doc);
        assertEquals(1, blobs.size());
        assertTrue(blobs.contains(blob3));

        // only the blob of file (no schema prefix)
        // the /data part is ignored  because we do prefix match of existing properties (???)
        // <field>content/data</field>
        pathProps = new HashSet<String>();
        pathProps.add("content/data");
        extractor.setExtractorProperties(pathProps, null, false);
        blobs = extractor.getBlobs(doc);
        assertEquals(1, blobs.size());
        assertTrue(blobs.contains(blob3));

        // exclude specific blob
        // <fieldType>blob</fieldType>
        // <excludeField>content</excludeField>
        pathProps = new HashSet<String>();
        pathProps.add("content");
        extractor.setExtractorProperties(null, pathProps, true);
        blobs = extractor.getBlobs(doc);
        assertEquals(2, blobs.size());
        assertTrue(blobs.contains(blob2));

        // exclude specific blob using schema prefix when schema is defined without prefix
        // <fieldType>blob</fieldType>
        // <excludeField>file:content</excludeField>
        pathProps = new HashSet<String>();
        pathProps.add("file:content");
        extractor.setExtractorProperties(null, pathProps, true);
        blobs = extractor.getBlobs(doc);
        assertEquals(2, blobs.size());
        assertTrue(blobs.contains(blob2));
    }

}
