/*
 * (C) Copyright 2009-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIDeleteConstraint;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.ecm.directory.api.ui.HierarchicalDirectoryUIDeleteConstraint;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @author Anahide Tchertchian
 *
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.core",
        // deploy directory service + sql factory
        "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.directory.types.contrib",
        // deploy directory ui service
        "org.nuxeo.ecm.directory.web" })
// deploy test dirs + ui config
@LocalDeploy("org.nuxeo.ecm.directory.web.tests:OSGI-INF/test-directory-ui-sql-contrib.xml")
public class TestDirectoryUIManager {

    @Inject
    DirectoryService dirService;

    @Inject
    DirectoryUIManager service;

    @Before
    public void setUp() throws Exception {
        DatabaseHelper.DATABASE.setUp();
    }

    @After
    public void tearDown() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
    }

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

        List<DirectoryUIDeleteConstraint> constraints = continent.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(1, constraints.size());

        DirectoryUIDeleteConstraint constraint = constraints.get(0);
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
        Session continentSession = null;
        Session countrySession = null;
        try {
            Directory continentDir = dirService.getDirectory("continent");
            assertNotNull(continentDir);
            continentSession = dirService.open("continent");
            assertTrue(continentSession.hasEntry("asia"));

            Directory country = dirService.getDirectory("country");
            assertNotNull(country);
            countrySession = dirService.open("country");
            assertTrue(countrySession.hasEntry("Afghanistan"));
            DocumentModel afgha = countrySession.getEntry("Afghanistan");
            assertEquals("asia", afgha.getProperty("xvocabulary", "parent"));

            DirectoryUI continent = service.getDirectoryInfo("continent");
            assertNotNull(continent);
            List<DirectoryUIDeleteConstraint> constraints = continent.getDeleteConstraints();
            assertNotNull(constraints);
            assertEquals(1, constraints.size());
            DirectoryUIDeleteConstraint constraint = constraints.get(0);
            assertTrue(constraint instanceof HierarchicalDirectoryUIDeleteConstraint);

            // test can delete when there a dep in child dir
            assertFalse(constraint.canDelete(dirService, "asia"));
            assertTrue(constraint.canDelete(dirService, "antartica"));

        } finally {
            if (continentSession != null) {
                continentSession.close();
            }
            if (countrySession != null) {
                countrySession.close();
            }
        }

    }

}
