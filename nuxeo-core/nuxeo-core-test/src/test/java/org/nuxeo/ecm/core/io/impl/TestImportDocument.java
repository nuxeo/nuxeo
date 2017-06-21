/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDirectoryReader;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests several import document cases.
 *
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-import-document-type-contrib.xml")
public class TestImportDocument {

    protected final String TEST_CLASS_DIRECTORY_PATH = "files/test-import-document/";

    @Inject
    protected CoreSession session;

    /**
     * Reference test.
     */
    @Test
    public void testDefaultImport() throws IOException {
        DocumentModel document = importDocumentFromDirectory("default-import");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
    }

    /**
     * Test to import a document with a removed property.
     */
    @Test
    public void testImportWithRemovedProperty() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
    }

    /**
     * Test to import a document with a renamed scalar property.
     */
    @Test
    public void testImportWithRemovedScalarWithFallback() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-scalar-fallback");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals("renamed", document.getPropertyValue("removed:scalar"));
    }

    /**
     * Test to import a document with a renamed complex property.
     */
    @Test
    public void testImportWithRemovedComplexWithFallback() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-complex-fallback");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals(Collections.singletonMap("scalar", "scalar"), document.getPropertyValue("removed:complex"));
    }

    /**
     * Test to import a document with a renamed list of scalar property.
     */
    @Test
    public void testImportWithRemovedScalarsWithFallback() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-scalars-fallback");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals(Arrays.asList("renamed1", "renamed2"), document.getPropertyValue("removed:scalars"));
    }

    /**
     * Test to import a document with a renamed list of complex property.
     */
    @Test
    public void testImportWithRemovedComplexesWithFallback() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-complexes-fallback");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals(Arrays.asList(Collections.singletonMap("scalar", "scalar1"),
                Collections.singletonMap("scalar", "scalar2")), document.getPropertyValue("removed:complexes"));
    }

    /**
     * Test to import a document with a renamed scalar inside a list of complex property.
     */
    @Test
    public void testImportWithRemovedScalarInComplexesWithFallback() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-scalar-complexes-fallback");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals(Arrays.asList(Collections.singletonMap("scalar", "renamed1"),
                Collections.singletonMap("scalar", "renamed2")), document.getPropertyValue("removed:complexes"));
    }

    /**
     * Test to import a document with a renamed scalar inside a list of complex property.
     */
    @Test
    public void testImportWithRemovedScalarInComplexWithFallbackScalar() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-scalar-complex-fallback-scalar");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals("moved", document.getPropertyValue("removed:scalar"));
    }

    /**
     * Test to import a document with a renamed scalar inside a list of complex property.
     */
    @Test
    public void testImportWithRemovedScalarWithFallbackComplex() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-scalar-fallback-complex");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals("moved", document.getPropertyValue("removed:complex/scalar"));
    }

    /**
     * Test to import a document with a renamed scalar inside a list of complex property.
     */
    @Test
    public void testImportWithRemovedScalarWithFallbackBlob() throws IOException {
        DocumentModel document = importDocumentFromDirectory("import-removed-scalar-fallback-blob");

        // Assert some properties
        assertEquals("dummy file", document.getPropertyValue("dc:title"));
        assertEquals("moved", document.getPropertyValue("removed:blob/name"));
    }

    protected DocumentModel importDocumentFromDirectory(String directoryName) throws IOException {
        // Init reader
        File directory = FileUtils.getResourceFileFromContext(TEST_CLASS_DIRECTORY_PATH + directoryName);
        XMLDirectoryReader reader = new XMLDirectoryReader(directory);

        // Init writer
        DocumentModelWriter writer = new DocumentModelWriter(session, "/import");

        // Init pipe
        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(reader);
        pipe.setWriter(writer);
        DocumentTranslationMap result = pipe.run();

        // There's only one imported document by test design
        assertNotNull(result);
        Map<DocumentRef, DocumentRef> docRefMap = result.getDocRefMap();
        assertNotNull(docRefMap);
        Collection<DocumentRef> destRefs = docRefMap.values();
        assertEquals(1, destRefs.size());
        DocumentRef destRef = destRefs.iterator().next();
        assertNotNull(destRef);
        return session.getDocument(destRef);
    }

}
