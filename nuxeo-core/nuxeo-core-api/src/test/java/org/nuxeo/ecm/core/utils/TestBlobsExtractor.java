/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public class TestBlobsExtractor extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-blobsextractor-types-contrib.xml");
    }

    @Test
    public void test() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");

        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob1 = Blobs.createBlob("foo1 bar1");
        blob1.setFilename("file1.txt");
        vignette.put("content", blob1);
        vignettes.add(vignette);

        vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob2 = Blobs.createBlob("foo2 bar2");
        blob2.setFilename("file2.txt");
        vignette.put("content", blob2);
        vignettes.add(vignette);

        Map<String, Object> attachedFile = new HashMap<String, Object>();
        attachedFile.put("name", "some name");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);

        Blob blob3 = Blobs.createBlob("foo3 bar3");
        doc.setProperty("file", "content", blob3);

        BlobsExtractor extractor = new BlobsExtractor();
        List<Blob> blobs = extractor.getBlobs(doc);
        assertEquals(3, blobs.size());
        assertTrue(blobs.contains(blob1));
        assertTrue(blobs.contains(blob2));
        assertTrue(blobs.contains(blob3));
    }

    @Test
    public void testBlobFieldPaths() throws Exception {
        BlobsExtractor extractor = new BlobsExtractor();
        extractor.getBlobFieldPathForDocumentType("ComplexDoc");
        Map<String, Map<String, List<String>>> blobFieldPaths = extractor.blobFieldPaths;
        assertEquals(Collections.singleton("ComplexDoc"), blobFieldPaths.keySet());
        Map<String, List<String>> values = blobFieldPaths.get("ComplexDoc");
        assertEquals(new HashSet<>(Arrays.asList("file", "complexschema")), values.keySet());
        assertEquals(Arrays.asList("file:content"), values.get("file"));
        assertEquals(Arrays.asList("cmpf:aList/*/content", "cmpf:attachedFile/vignettes/*/content"), values.get("complexschema"));
    }

    @Test
    public void testWithRepositoryConfiguration() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");

        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob1 = Blobs.createBlob("foo1 bar1");
        blob1.setFilename("file1.txt");
        vignette.put("content", blob1);
        vignettes.add(vignette);

        vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob2 = Blobs.createBlob("foo2 bar2");
        blob2.setFilename("file2.txt");
        vignette.put("content", blob2);
        vignettes.add(vignette);

        Map<String, Object> attachedFile = new HashMap<String, Object>();
        attachedFile.put("name", "some name");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);

        Blob blob3 = Blobs.createBlob("foo3 bar3");
        doc.setProperty("file", "content", blob3);

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
