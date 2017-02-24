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
import java.util.Objects;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningPolicyFilter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-document-type.xml")
@Deploy({"org.nuxeo.ecm.core.test.tests", "org.nuxeo.ecm.platform.el"})
public class TestAutoVersioning {

    @Inject
    private CoreSession session;

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-initial-version.xml")
    public void testInitialVersion() {
        // No initial state defined by policy
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
        // Document with facet Folderish or Downloadable should have a new version for each update
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        doc = session.createDocumentModel("/", "testfile1", "Note2");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-auto-versioning-standard-filter-schemas.xml")
    public void testWithStandardFilterAndSchemas() {
        // No initial state defined by policy
        // Document with file or note schema should have a new version for each update
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
