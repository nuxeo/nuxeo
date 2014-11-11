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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.virtualnavigation.tests;

import java.util.List;

import org.nuxeo.ecm.virtualnavigation.action.NavTreeDescriptor;
import org.nuxeo.ecm.virtualnavigation.service.NavTreeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestNavTreeService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.platform.virtualnavigation.web",
                "OSGI-INF/navtree-framework.xml");
        deployContrib("org.nuxeo.platform.virtualnavigation.web.test",
                "OSGI-INF/navtree-contrib.xml");
    }

    public void testServiceLookup() {
        NavTreeService service = Framework.getLocalService(NavTreeService.class);
        assertNotNull(service);
    }

    public void testNavTrees() throws Exception {
        NavTreeService service = Framework.getLocalService(NavTreeService.class);
        assertNotNull(service);

        List<NavTreeDescriptor> descs = service.getTreeDescriptors();
        assertEquals(1, descs.size());

        assertNotNull(descs.get(0).getXhtmlview());
        assertFalse(descs.get(0).isDirectoryTreeBased());
    }

    public void testNavTreesWithDirectories() throws Exception {

        deployContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/directorytreemanager-framework.xml");
        deployContrib("org.nuxeo.platform.virtualnavigation.web",
                "OSGI-INF/directorytreemanager-contrib.xml");
        fireFrameworkStarted(); // needed (org.nuxeo.runtime.started in contrib)

        NavTreeService service = Framework.getLocalService(NavTreeService.class);
        assertNotNull(service);

        List<NavTreeDescriptor> descs = service.getTreeDescriptors();
        assertEquals(3, descs.size());

        assertNotNull(descs.get(0).getXhtmlview());
        assertFalse(descs.get(0).isDirectoryTreeBased());

        assertNull(descs.get(1).getXhtmlview());
        assertTrue(descs.get(1).isDirectoryTreeBased());

        assertNull(descs.get(2).getXhtmlview());
        assertTrue(descs.get(2).isDirectoryTreeBased());

    }

}
