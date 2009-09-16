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
public class TestDirectoryUIManagerRegistration extends NXRuntimeTestCase {

    DirectoryService dirService;

    DirectoryUIManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // deploy directory
        deployBundle("org.nuxeo.ecm.directory");
        // deploy directory ui service
        deployBundle("org.nuxeo.ecm.directory.web");

        // deploy test dirs + ui config
        deployContrib("org.nuxeo.ecm.directory.web.tests",
                "OSGI-INF/test-directory-ui-contrib.xml");

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
        assertEquals("foo", country.getView());
        constraints = country.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(0, constraints.size());
    }

    public void testDirectoryUIOverride() throws Exception {
        deployContrib("org.nuxeo.ecm.directory.web.tests",
                "OSGI-INF/test-directory-ui-override-contrib.xml");

        List<String> dirs = service.getDirectoryNames();
        assertNotNull(dirs);
        assertEquals(1, dirs.size());
        assertEquals("country", dirs.get(0));

        DirectoryUI continent = service.getDirectoryInfo("continent");
        assertNull(continent);

        DirectoryUI country = service.getDirectoryInfo("country");
        assertNotNull(country);
        assertEquals("country", country.getName());
        assertEquals("country_vocabulary", country.getLayout());
        assertEquals("parent", country.getSortField());
        assertEquals("foo", country.getView());
        List<DirectoryUIDeleteConstraint> constraints = country.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(0, constraints.size());
    }
}
