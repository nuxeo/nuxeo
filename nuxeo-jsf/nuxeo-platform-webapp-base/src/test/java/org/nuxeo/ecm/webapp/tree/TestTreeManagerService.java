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

import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 *
 */
public class TestTreeManagerService extends RepositoryOSGITestCase {

    protected TreeManager treeManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // openRepository();

        // deploy needed bundles
        deployContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/nxtreemanager-framework.xml");
        deployContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/nxtreemanager-contrib.xml");

        treeManager = Framework.getService(TreeManager.class);
        assertNotNull(treeManager);
    }

    @Override
    public void tearDown() throws Exception {
        // undeploy bundles
        undeployContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/nxtreemanager-framework.xml");
        undeployContrib("org.nuxeo.ecm.webapp.base",
                "OSGI-INF/nxtreemanager-contrib.xml");
        super.tearDown();
    }

    public void testDefaultContribs() {
        Filter filter = treeManager.getFilter("navigation");
        assertNotNull(filter);

        Filter filter2 = treeManager.getLeafFilter("navigation");
        assertNotNull(filter2);

        Sorter sorter = treeManager.getSorter("navigation");
        assertNotNull(sorter);
    }

}
