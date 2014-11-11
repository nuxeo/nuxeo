/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIDeleteConstraint;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.ecm.directory.api.ui.HierarchicalDirectoryUIDeleteConstraint;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 *
 */
public class TestDirectoryUIManager extends NXRuntimeTestCase {

    DirectoryService dirService;

    DirectoryUIManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");

        // deploy directory service + sql factory
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");

        // deploy directory ui service
        deployBundle("org.nuxeo.ecm.directory.web");
        // deploy test dirs + ui config
        deployContrib("org.nuxeo.ecm.directory.web.tests",
                "OSGI-INF/test-directory-ui-sql-contrib.xml");

        service = Framework.getService(DirectoryUIManager.class);
        assertNotNull(service);

        dirService = Framework.getService(DirectoryService.class);
        assertNotNull(dirService);
    }

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
