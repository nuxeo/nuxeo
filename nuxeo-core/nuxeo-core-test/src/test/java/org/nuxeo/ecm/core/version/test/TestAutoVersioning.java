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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.versioning.VersioningPolicyFilter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@Deploy({"org.nuxeo.ecm.core.test.tests"})
@LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-document-type.xml")
public class TestAutoVersioning extends AbstractTestVersioning {

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-initial-version.xml")
    public void testInitialVersion() {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("2.5+", doc.getVersionLabel());
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("2.5+", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-always-minor.xml")
    public void testAlwaysVersionMinor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.2", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-always-major.xml")
    public void testAlwaysVersionMajor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-always-major.xml")
    public void testAlwaysVersionMajorTwoSaves() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());
        // Save document again without editing it
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-custom-filter.xml")
    public void testWithCustomFilter() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        // Update of dc:title should trigger a new version
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        // an edition to dc:description should not trigger a new version
        doc.setPropertyValue("dc:description", "New Description");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-types.xml")
    public void testWithStandardFilterAndTypes() {
        // No initial state defined by policy
        // Document of type File or Note should have a new version for each update
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note2");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-facets.xml")
    public void testWithStandardFilterAndFacets() {
        // No initial state defined by policy
        // Document with facet Downloadable or Note3Facet should have a new version for each update
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note2");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note3");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-schemas.xml")
    public void testWithStandardFilterAndSchemas() {
        // No initial state defined by policy
        // Document with file schema should have a new version for each update
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note2");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-condition.xml")
    public void testWithStandardFilterAndCondition() {
        // No initial state defined by policy
        // Same condition as the custom filter below
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        // Update of dc:title should trigger a new version
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        // an edition to dc:description should not trigger a new version
        doc.setPropertyValue("dc:description", "New Description");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-ordering.xml")
    public void testAutoVersioningOrdering() {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile2", "Note");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-before-always-minor.xml")
    public void testAutoVersioningBeforeAlwaysMinor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        // get last version (document before update)
        DocumentModel lastVersion = session.getLastDocumentVersion(doc.getRef());
        assertFalse(lastVersion.isCheckedOut());
        assertEquals("0.1", lastVersion.getVersionLabel());
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        // get last version (document before update)
        lastVersion = session.getLastDocumentVersion(doc.getRef());
        assertFalse(lastVersion.isCheckedOut());
        assertEquals("0.2", lastVersion.getVersionLabel());
        assertTrue(doc.isCheckedOut());
        assertEquals("0.2+", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-before-minor-after-major.xml")
    public void testAutoVersioningBeforeAlwaysMinorAfterAlwaysMajor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        // get versions (document before update)
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertEquals(2, versions.size());
        DocumentModel versionBefore = versions.get(0);
        DocumentModel versionAfter = versions.get(1);
        assertFalse(versionBefore.isCheckedOut());
        assertEquals("0.1", versionBefore.getVersionLabel());
        assertFalse(versionAfter.isCheckedOut());
        assertEquals("1.0", versionAfter.getVersionLabel());
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        // get versions (document before update)
        versions = session.getVersions(doc.getRef());
        // as document before update was already a version - service will just create one new version
        assertEquals(3, versions.size());
        versionAfter = versions.get(2);
        assertFalse(versionAfter.isCheckedOut());
        assertEquals("2.0", versionAfter.getVersionLabel());
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());
    }

    /** A custom filter for tests. */
    public static class CustomVersioningFilter implements VersioningPolicyFilter {

        @Override
        public boolean test(DocumentModel previousDocument, DocumentModel currentDocument) {
            // Handle creation case - do nothing
            if (previousDocument == null) {
                return false;
            }
            // Run versioning only if dc:title has changed
            Serializable previousTitle = previousDocument.getPropertyValue("dc:title");
            Serializable currentTitle = currentDocument.getPropertyValue("dc:title");
            return !Objects.equals(previousTitle, currentTitle);
        }

    }

}
