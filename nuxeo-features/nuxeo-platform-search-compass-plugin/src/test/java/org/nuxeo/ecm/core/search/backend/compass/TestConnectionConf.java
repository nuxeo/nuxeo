/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass;

import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestConnectionConf extends NXRuntimeTestCase {

    private CompassBackend compass;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-framework.xml");
        SearchServiceInternals service = (SearchServiceInternals)
                        SearchServiceDelegate.getRemoteSearchService();
        assertNotNull(service);
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-nxsearch-backend-compass-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxsearch-compass-test-contrib.xml");
        compass = (CompassBackend) service.getSearchEngineBackendByName("compass");
        assertNotNull(compass);
    }

    public void testFS() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-fs-connection.xml");
        assertEquals("file:///foo/bar", compass.getConnectionString());
    }

    public void testRelativeFS() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-fs-relative-connection.xml");
        // exactly the same construction as in tested code so this just
        // tests the branching part of the code...
        String expected = "file://" +
            Framework.getRuntime().getHome().getPath() +
            "foo/bar";
        assertEquals(expected, compass.getConnectionString());
        // check calling it twice is ok
        assertEquals(expected, compass.getConnectionString());
    }

    public void testLastWins() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-fs-relative-connection.xml");
        testFS();
    }

    public void testDefault() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-default-connection.xml");
        assertNull(compass.getConnectionString());
    }

    public void testDefaultLastWins() throws Exception {
        // this one is really vital
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-fs-relative-connection.xml");
        testDefault();
    }

}
