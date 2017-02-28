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
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Restriction contributions always restrict to MAJOR option.
 *
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestVersioningRestriction {

    @Inject
    protected CoreSession session;

    @Test
    public void testNoRestriction() {
        testNoRestriction("File", false);
        testNoRestriction("File", true);
        testNoRestriction("Note", false);
        testNoRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-all-types-all-life-cycles.xml")
    public void testRestrictionAllTypesAllLifeCycles() {
        testRestriction("File", false);
        testRestriction("File", true);
        testRestriction("Note", false);
        testRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-all-types-project-life-cycle.xml")
    public void testRestrictionAllTypesProjectLifeCycles() {
        testRestriction("File", false);
        testNoRestriction("File", true);
        testRestriction("Note", false);
        testNoRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-file-type-all-life-cycles.xml")
    public void testRestrictionFileTypeAllLifeCycles() {
        testRestriction("File", false);
        testRestriction("File", true);
        testNoRestriction("Note", false);
        testNoRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-file-type-project-life-cycle.xml")
    public void testRestrictionFileTypeProjectLifeCycle() {
        testRestriction("File", false);
        testNoRestriction("File", true);
        testNoRestriction("Note", false);
        testNoRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-file-type-obsolete-life-cycle-all-types-project-life-cycle.xml")
    public void testRestrictionFileTypeObsoleteLifeCycleAndAllTypesProjectLifeCycle() {
        testRestriction("File", false);
        testNoRestriction("File", true);
        testRestriction("Note", false);
        testNoRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-file-type-obsolete-life-cycle-all-types-all-life-cycles.xml")
    public void testRestrictionFileTypeObsoleteLifeCycleAndAllTypesAllLifeCycles() {
        // even if restriction apply on File, as we don't have matching rule for 'project' or 'approved' lifeCycle,
        // everything is rejected
        testRestriction("File", false);
        testRestriction("File", true);
        testRestriction("Note", false);
        testRestriction("Note", true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:test-versioning-restriction-all-types-all-life-cycles-file-reinit.xml")
    public void testRestrictionAllTypesAllLifeCyclesAndFileReinit() {
        // here everything is restricted, unless File which re-init the rule
        testNoRestriction("File", false);
        testNoRestriction("File", true);
        testRestriction("Note", false);
        testRestriction("Note", true);
    }

    private void testNoRestriction(String type, boolean followApproveTransition) {
        DocumentModel doc = session.createDocumentModel("/", "testfile1", type);
        doc = session.createDocument(doc);
        if (followApproveTransition) {
            doc.followTransition("approve");
        }
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        // Test with NONE
        doc.setPropertyValue("dc:title", "title 1");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        // Test with MINOR
        doc.setPropertyValue("dc:title", "title 2");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        // Test with MAJOR
        doc.setPropertyValue("dc:title", "title 3");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());
    }

    private void testRestriction(String type, boolean followApproveTransition) {
        String lifeCycleState = "project";

        DocumentModel doc = session.createDocumentModel("/", "Test " + type, type);
        doc = session.createDocument(doc);
        if (followApproveTransition) {
            doc.followTransition("approve");
            lifeCycleState = "approved";
        }
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());

        // Test with NONE
        try {
            doc.setPropertyValue("dc:title", "title 1");
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
            doc = session.saveDocument(doc);
            fail("Should have raised an exception, NONE option is excluded");
        } catch (NuxeoException e) {
            assertEquals("Versioning option=NONE is not allowed by the configuration for type=" + type
                    + "/lifeCycleState=" + lifeCycleState, e.getMessage());
        }

        // Test with MINOR
        try {
            doc.setPropertyValue("dc:title", "title 2");
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
            doc = session.saveDocument(doc);
            fail("Should have raised an exception, MINOR option is excluded");
        } catch (NuxeoException e) {
            assertEquals("Versioning option=MINOR is not allowed by the configuration for type=" + type
                    + "/lifeCycleState=" + lifeCycleState, e.getMessage());
        }

        // Test with MAJOR
        doc.setPropertyValue("dc:title", "title 3");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());
    }

}
