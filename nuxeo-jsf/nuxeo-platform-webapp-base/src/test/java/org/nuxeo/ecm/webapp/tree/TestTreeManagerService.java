/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
        deployTestContrib("org.nuxeo.ecm.webapp.base", "OSGI-INF/nxtreemanager-framework.xml");
        deployTestContrib("org.nuxeo.ecm.webapp.base", "OSGI-INF/nxtreemanager-contrib.xml");

        treeManager = Framework.getService(TreeManager.class);
        assertNotNull(treeManager);
    }

    @Test
    public void testDefaultContribs() {
        String filterName = "navigation";
        assertEquals("tree_children", treeManager.getPageProviderName(filterName));
        assertNull(treeManager.getFilter(filterName));
        assertNotNull(treeManager.getLeafFilter(filterName));
        assertNull(treeManager.getSorter(filterName));
    }

    @Test
    public void testOverride() {
        deployContrib(Thread.currentThread().getContextClassLoader().getResource("test-nxtreemanager-contrib.xml"));
        String filterName = "navigation";
        assertEquals("tree_children", treeManager.getPageProviderName(filterName));
        assertNotNull(treeManager.getFilter(filterName));
        assertNull(treeManager.getLeafFilter(filterName));
        assertNotNull(treeManager.getSorter(filterName));
    }

}
