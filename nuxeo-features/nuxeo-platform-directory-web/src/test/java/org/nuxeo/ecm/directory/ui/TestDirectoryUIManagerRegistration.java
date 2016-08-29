/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.ecm.directory.api.ui.HierarchicalDirectoryUIDeleteConstraint;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestDirectoryUIManagerRegistration extends NXRuntimeTestCase {

    DirectoryService dirService;

    DirectoryUIManager service;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // deploy directory
        deployBundle("org.nuxeo.ecm.directory");
        // deploy directory ui service
        deployBundle("org.nuxeo.ecm.directory.web");

        // deploy test dirs + ui config
        deployContrib("org.nuxeo.ecm.directory.web.tests", "OSGI-INF/test-directory-ui-contrib.xml");

        service = Framework.getService(DirectoryUIManager.class);
        assertNotNull(service);

        dirService = Framework.getService(DirectoryService.class);
        assertNotNull(dirService);
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
        assertEquals("foo", country.getView());
        constraints = country.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(0, constraints.size());
    }

    @Test
    public void testDirectoryUIOverride() throws Exception {
        deployContrib("org.nuxeo.ecm.directory.web.tests", "OSGI-INF/test-directory-ui-override-contrib.xml");

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
        List<DirectoryDeleteConstraint> constraints = country.getDeleteConstraints();
        assertNotNull(constraints);
        assertEquals(0, constraints.size());
    }
}
