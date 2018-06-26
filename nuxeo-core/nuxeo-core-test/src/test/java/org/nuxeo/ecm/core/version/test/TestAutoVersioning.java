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

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningPolicyFilter;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 9.1
 */
@Deploy("org.nuxeo.ecm.core.test.tests")
@Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-document-type.xml")
public class TestAutoVersioning extends AbstractTestVersioning {

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-initial-version.xml")
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
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-always-minor.xml")
    public void testAlwaysVersionMinor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        // creation should create a version
        doc.setPropertyValue("dc:title", "A");
        doc = session.createDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.2", doc.getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-always-major.xml")
    public void testAlwaysVersionMajor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        // creation should create a version
        doc.setPropertyValue("dc:title", "A");
        doc = session.createDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-always-major.xml")
    public void testAlwaysVersionMajorTwoSaves() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        // creation should create a version
        doc.setPropertyValue("dc:title", "A");
        doc = session.createDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());

        // second update without edition shouldn't create a version
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-custom-filter.xml")
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
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-types.xml")
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
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-facets.xml")
    public void testWithStandardFilterAndFacets() {
        // No initial state defined by policy
        // Document with facet Downloadable or Note3Facet should have a new version for each update
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile2", "Note2");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile3", "Note3");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-schemas.xml")
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
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-condition.xml")
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
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-ordering.xml")
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
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-before-always-minor.xml")
    public void testAutoVersioningBeforeAlwaysMinor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        // get last version (document before update)
        // here we use getVersions instead of getLastDocumentVersion because on mem repository created date could the
        // same and we don't retrieve the right versions
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertEquals(1, versions.size());
        DocumentModel lastVersion = versions.get(0);
        assertFalse(lastVersion.isCheckedOut());
        assertEquals("0.1", lastVersion.getVersionLabel());
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        maybeSleepToNextSecond();
        doc = session.saveDocument(doc);
        // get last version (document before update)
        versions = session.getVersions(doc.getRef());
        assertEquals(2, versions.size());
        lastVersion = versions.get(1);
        assertFalse(lastVersion.isCheckedOut());
        assertEquals("0.2", lastVersion.getVersionLabel());
        assertTrue(doc.isCheckedOut());
        assertEquals("0.2+", doc.getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-before-minor-after-major.xml")
    public void testAutoVersioningBeforeAlwaysMinorAfterAlwaysMajor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        // creation should create a version
        doc.setPropertyValue("dc:title", "A");
        doc = session.createDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // update document by disabling automatic versioning after update, furthermore automatic versioning before
        // update won't be performed cause document is already check in
        doc.setPropertyValue("dc:title", "B");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("1.0+", doc.getVersionLabel());
        // check there's only one version
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertEquals(1, versions.size());

        // an update should create a version before and after
        doc = session.saveDocument(doc);
        // get versions (document before update)
        versions = session.getVersions(doc.getRef());
        versions.sort((v1, v2) -> v1.getVersionLabel().compareTo(v2.getVersionLabel()));
        assertEquals(3, versions.size());
        DocumentModel versionAfterCreation = versions.get(0);
        DocumentModel versionBeforeUpdate = versions.get(1);
        DocumentModel versionAfterUpdate = versions.get(2);
        assertFalse(versionAfterCreation.isCheckedOut());
        assertEquals("1.0", versionAfterCreation.getVersionLabel());
        assertFalse(versionBeforeUpdate.isCheckedOut());
        assertEquals("1.1", versionBeforeUpdate.getVersionLabel());
        assertFalse(versionBeforeUpdate.isCheckedOut());
        assertEquals("2.0", versionAfterUpdate.getVersionLabel());
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());

        // wait for mem repository - repository is fast, we assume that some workers update states with a previous state
        // of metadata, in this case version label for version 3.0 is not correct (ie: 2.0)
        txFeature.nextTransaction();

        // an edition should create a version after update as document is already checked out
        doc.setPropertyValue("dc:title", "C");
        doc = session.saveDocument(doc);
        // get versions
        versions = session.getVersions(doc.getRef());
        versions.sort((v1, v2) -> v1.getVersionLabel().compareTo(v2.getVersionLabel()));
        // as document before update was already a version - service will just create one new version
        assertEquals(4, versions.size());
        versionAfterUpdate = versions.get(3);
        assertFalse(versionAfterUpdate.isCheckedOut());
        assertEquals("3.0", versionAfterUpdate.getVersionLabel());
        assertFalse(doc.isCheckedOut());
        assertEquals("3.0", doc.getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-before-always-minor.xml")
    public void testManualVersioningCreateABeforeVersion() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc.setPropertyValue("dc:title", "A");
        doc = session.createDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        // Ask for a version
        doc.setPropertyValue("dc:title", "B");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // Check that before automatic versioning was trigger
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        versions.sort((v1, v2) -> v1.getVersionLabel().compareTo(v2.getVersionLabel()));
        assertEquals(2, versions.size());
        assertEquals("0.1", versions.get(0).getVersionLabel());
        assertEquals("1.0", versions.get(1).getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-before-always-minor.xml")
    public void testManualVersioningOnlyDoesntCreateABeforeVersion() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc.setPropertyValue("dc:title", "A");
        doc = session.createDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        // Ask for a version
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // Check that before automatic versioning wasn't trigger
        List<DocumentModel> versions = session.getVersions(doc.getRef());
        assertEquals(1, versions.size());
        assertEquals("1.0", versions.get(0).getVersionLabel());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-no-auto-versioning-system.xml")
    public void testAutoVersioningNoneOption() {

        DocumentModel doc = session.createDocumentModel("/", "note", "Note4");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "newNote");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

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
