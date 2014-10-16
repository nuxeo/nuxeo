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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.platform.computedgroups.ComputedGroupsService;
import org.nuxeo.ecm.platform.computedgroups.ComputedGroupsServiceImpl;
import org.nuxeo.ecm.platform.computedgroups.GroupComputer;
import org.nuxeo.ecm.platform.computedgroups.GroupComputerDescriptor;
import org.nuxeo.ecm.platform.computedgroups.UserManagerWithComputedGroups;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestComputedGroupService extends NXRuntimeTestCase {

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
    public void testLookup() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.usermanager", "OSGI-INF/computedgroups-framework.xml");
        ComputedGroupsService cgs = Framework.getLocalService(ComputedGroupsService.class);
        assertNotNull(cgs);
    }

    @Test
    public void testContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests", "computedgroups-contrib.xml");
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

        ComputedGroupsServiceImpl component = (ComputedGroupsServiceImpl) cgs;

        GroupComputerDescriptor desc =  component.getComputerDescriptors().get(0);
        assertNotNull(desc);
        assertEquals("dummy", desc.getName());

        GroupComputer computer = desc.getComputer();
        assertNotNull(computer);
        assertTrue(computer.getAllGroupIds().contains("Grp1"));

        NuxeoGroup group = cgs.getComputedGroup("Grp1");
        assertNotNull(group);
        List<String> users = group.getMemberUsers();
        assertEquals(2, users.size());
        assertTrue(users.contains("User1"));
        assertTrue(users.contains("User12"));
        assertFalse(users.contains("User2"));

        NuxeoPrincipalImpl nxPrincipal = new NuxeoPrincipalImpl("User2");
        List<String> vGroups = cgs.computeGroupsForUser(nxPrincipal);
        assertEquals(1, vGroups.size());
        assertTrue(vGroups.contains("Grp2"));

        nxPrincipal = new NuxeoPrincipalImpl("User12");
        vGroups = cgs.computeGroupsForUser(nxPrincipal);
        assertEquals(2, vGroups.size());
        assertTrue(vGroups.contains("Grp1"));
        assertTrue(vGroups.contains("Grp2"));
    }

    @Test
    public void testUserManagerIntegration() throws Exception {

        deployContrib("org.nuxeo.ecm.platform.usermanager.tests", "computedgroups-contrib.xml");
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

        UserManager um = Framework.getLocalService(UserManager.class);
        assertNotNull(um);

        boolean isUserManagerWithComputedGroups = false;
        if (um instanceof UserManagerWithComputedGroups) {
            isUserManagerWithComputedGroups=true;
        }
        assertTrue(isUserManagerWithComputedGroups);

        DocumentModel userModel = um.getBareUserModel();
        userModel.setProperty("user", "username", "User1");
        um.createUser(userModel);
        userModel.setProperty("user", "username", "User12");
        um.createUser(userModel);
        userModel.setProperty("user", "username", "User2");
        um.createUser(userModel);

        DocumentModel groupModel = um.getBareGroupModel();
        groupModel.setProperty("group", "groupname", "StaticGroup");
        um.createGroup(groupModel);
        List<String> staticGroups=new ArrayList<String>();
        staticGroups.add("StaticGroup");
        userModel = um.getUserModel("User1");
        userModel.setProperty("user", "groups", staticGroups);
        um.updateUser(userModel);

        NuxeoPrincipalImpl principal = (NuxeoPrincipalImpl) um.getPrincipal("User1");
        assertEquals(1, principal.getVirtualGroups().size());
        assertTrue(principal.getVirtualGroups().contains("Grp1"));
        assertEquals(2, principal.getAllGroups().size());
        assertTrue(principal.getAllGroups().contains("Grp1"));
        assertTrue(principal.getAllGroups().contains("StaticGroup"));

        principal = (NuxeoPrincipalImpl) um.getPrincipal("User2");
        assertEquals(1, principal.getVirtualGroups().size());
        assertTrue(principal.getVirtualGroups().contains("Grp2"));
        assertEquals(1, principal.getAllGroups().size());
        assertTrue(principal.getAllGroups().contains("Grp2"));

        principal = (NuxeoPrincipalImpl) um.getPrincipal("User12");
        assertEquals(2, principal.getVirtualGroups().size());
        assertTrue(principal.getVirtualGroups().contains("Grp1"));
        assertTrue(principal.getVirtualGroups().contains("Grp2"));
        assertEquals(2, principal.getAllGroups().size());

        NuxeoGroup group = um.getGroup("Grp1");
        assertEquals(2, group.getMemberUsers().size());
        assertTrue(group.getMemberUsers().contains("User1"));
        assertTrue(group.getMemberUsers().contains("User12"));

        group = um.getGroup("Grp2");
        assertEquals(2, group.getMemberUsers().size());
    }

    @Test
    public void testCompanyComputer() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "companycomputedgroups-contrib.xml");
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

        UserManager um = Framework.getLocalService(UserManager.class);
        assertNotNull(um);

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        HashSet<String> fulltext = new HashSet<String>();
        filter.put(um.getGroupIdField(), "Nux");

        DocumentModelList nxGroups = um.searchGroups(filter, fulltext);
        assertEquals(0, nxGroups.size());

        NuxeoGroup nxGroup = um.getGroup("Nuxeo");
        assertNull(nxGroup);

        DocumentModel newUser = um.getBareUserModel();
        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(), "toto");
        newUser.setProperty(um.getUserSchemaName(), "company", "Nuxeo");
        um.createUser(newUser);

        nxGroups = um.searchGroups(filter, fulltext);
        assertEquals(1, nxGroups.size());

        nxGroup = um.getGroup("Nuxeo");
        assertNotNull(nxGroup);
        assertEquals(1, nxGroup.getMemberUsers().size());

        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(), "titi");
        newUser.setProperty(um.getUserSchemaName(), "company", "Nuxeo");
        um.createUser(newUser);

        nxGroups = um.searchGroups(filter, fulltext);
        assertEquals(1, nxGroups.size());

        nxGroup = um.getGroup("Nuxeo");
        assertNotNull(nxGroup);
        assertEquals(2, nxGroup.getMemberUsers().size());

        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(), "tata");
        newUser.setProperty(um.getUserSchemaName(), "company", "MyInc");
        um.createUser(newUser);

        nxGroups = um.searchGroups(filter, fulltext);
        assertEquals(1, nxGroups.size());

        nxGroup = um.getGroup("MyInc");
        assertNotNull(nxGroup);
        assertEquals(1, nxGroup.getMemberUsers().size());
    }

}
