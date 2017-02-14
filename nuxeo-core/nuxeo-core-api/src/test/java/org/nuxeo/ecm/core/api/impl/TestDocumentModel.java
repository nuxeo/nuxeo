/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @since 9.1
 */
public class TestDocumentModel extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-documentmodel-types-contrib.xml");
    }

    @Test
    public void testSetRemovedScalarProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("common", "size", 10L);
        assertNull(doc.getProperty("common", "size"));
    }

    @Test
    public void testSetRemovedScalarPropertyValue() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("common:size", 10L);
        assertNull(doc.getPropertyValue("common:size"));
    }

    @Test
    public void testSetRemovedScalarProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("common", Collections.singletonMap("size", 10L));
        assertNull(doc.getProperties("common").get("size"));
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "scalarfallback"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:scalarfallback"));
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar2scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperties("deprecated").get("scalarfallback").toString());
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("file", "filename", "test filename");
        assertEquals("test filename", doc.getProperty("file", "content/name"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("file:filename", "test filename");
        assertEquals("test filename", doc.getPropertyValue("file:content/name"));
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("file", Collections.singletonMap("filename", "test filename"));
        assertEquals("test filename", ((Blob) doc.getProperties("file").get("content")).getFilename());
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedScalarPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar2complex", "test scalar"));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("deprecated").get("complexfallback")).get("scalar")
                                                                                                    .toString());
    }

    @Test
    public void testSetRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex", (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedComplexPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated",
                Collections.singletonMap("complex", Collections.singletonMap("scalar", "test scalar")));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("deprecated").get("complexfallback")).get("scalar")
                                                                                                    .toString());
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));
    }

}
