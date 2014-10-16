/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.computedgroups.test;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.platform.computedgroups.ComputedGroupsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestLabelledComputedGroupService extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DatabaseHelper.DATABASE.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
        super.tearDown();
    }

    @Test
    public void testContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests", "labelled-computedgroups-framework.xml");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");

        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/directory-config.xml");

        ComputedGroupsService cgs = Framework.getLocalService(ComputedGroupsService.class);
        assertNotNull(cgs);

        NuxeoGroup group = cgs.getComputedGroup("Grp1");
        assertNotNull(group);
        assertEquals("Groupe 1", group.getLabel());

        group = cgs.getComputedGroup("Grp2");
        assertNotNull(group);
        assertEquals("Groupe 2", group.getLabel());

    }

}
