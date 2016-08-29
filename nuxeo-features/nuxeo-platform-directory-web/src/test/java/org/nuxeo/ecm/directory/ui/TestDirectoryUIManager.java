/*
 * (C) Copyright 2009-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.ecm.directory.api.ui.HierarchicalDirectoryUIDeleteConstraint;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({
// deploy directory service + sql factory
        "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.directory.types.contrib",
        // deploy directory ui service
        "org.nuxeo.ecm.actions", "org.nuxeo.ecm.directory.web" })
// deploy test dirs + ui config
@LocalDeploy("org.nuxeo.ecm.directory.web.tests:OSGI-INF/test-directory-ui-sql-contrib.xml")
public class TestDirectoryUIManager {

    @Inject
    DirectoryService dirService;

    @Inject
    DirectoryUIManager service;

    @Test
    public void testDirectoryUIRegistration() throws Exception {
        List<String> dirs = service.getDirectoryNames();
        assertNotNull(dirs);
        assertEquals(2, dirs.size());
        assertEquals("continent", dirs.get(0));
        assertEquals("country", dirs.get(1));

        DirectoryUI continent = service.getDirectoryInfo("continent");
        assertNotNull(continent);
        assertEquals("continent", continent.getName());
        assertEquals("vocabulary", continent.getLayout());
        assertEquals("label", continent.getSortField());
        assertNull(continent.getView());

        List<DirectoryDeleteConstraint> constraints = continent.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(1, constraints.size());

        DirectoryDeleteConstraint constraint = constraints.get(0);
        assertTrue(constraint instanceof HierarchicalDirectoryUIDeleteConstraint);

        DirectoryUI country = service.getDirectoryInfo("country");
        assertNotNull(country);
        assertEquals("country", country.getName());
        assertEquals("country_vocabulary", country.getLayout());
        assertEquals("parent", country.getSortField());
        assertNull(country.getView());

        constraints = country.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(0, constraints.size());
    }

    @Test
    public void testDirectoryUIDeleteConstraint() throws Exception {
        try (Session continentSession = dirService.open("continent");
                Session countrySession = dirService.open("country")) {
            assertTrue(continentSession.hasEntry("asia"));

            assertTrue(countrySession.hasEntry("Afghanistan"));
            DocumentModel afgha = countrySession.getEntry("Afghanistan");
            assertEquals("asia", afgha.getProperty("xvocabulary", "parent"));

            DirectoryUI continent = service.getDirectoryInfo("continent");
            assertNotNull(continent);
            List<DirectoryDeleteConstraint> constraints = continent.getDeleteConstraints();
            assertNotNull(constraints);
            assertEquals(1, constraints.size());
            DirectoryDeleteConstraint constraint = constraints.get(0);
            assertTrue(constraint instanceof HierarchicalDirectoryUIDeleteConstraint);

            // test can delete when there a dep in child dir
            assertFalse(constraint.canDelete(dirService, "asia"));
            assertTrue(constraint.canDelete(dirService, "antartica"));

        }
    }

}
