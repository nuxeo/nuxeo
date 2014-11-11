/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestTreeManagerService extends NXRuntimeTestCase {

    protected TreeManager treeManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // deploy needed bundles
        deployTestContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/nxtreemanager-framework.xml");
        deployTestContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/nxtreemanager-contrib.xml");

        treeManager = Framework.getService(TreeManager.class);
        assertNotNull(treeManager);
    }

    @Test
    public void testDefaultContribs() {
        String filterName = "navigation";
        assertEquals("tree_children",
                treeManager.getPageProviderName(filterName));
        assertNull(treeManager.getFilter(filterName));
        assertNotNull(treeManager.getLeafFilter(filterName));
        assertNull(treeManager.getSorter(filterName));
    }

    @Test
    public void testOverride() {
        deployContrib(Thread.currentThread().getContextClassLoader().getResource(
                "test-nxtreemanager-contrib.xml"));
        String filterName = "navigation";
        assertEquals("tree_children",
                treeManager.getPageProviderName(filterName));
        assertNotNull(treeManager.getFilter(filterName));
        assertNull(treeManager.getLeafFilter(filterName));
        assertNotNull(treeManager.getSorter(filterName));
    }

}
