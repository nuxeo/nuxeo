/*
 * (C) Copyright 2010-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.computedgroups.test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.computedgroups.ComputedGroupsService;
import org.nuxeo.ecm.platform.computedgroups.ComputedGroupsServiceImpl;
import org.nuxeo.ecm.platform.computedgroups.GroupComputer;
import org.nuxeo.ecm.platform.computedgroups.GroupComputerDescriptor;
import org.nuxeo.ecm.platform.computedgroups.UserManagerWithComputedGroups;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:computedgroups-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestComputedGroupService {

    @Inject
    protected ComputedGroupsService cgs;

    @Inject
    protected UserManager um;

    @Test
    public void testContrib() {
        ComputedGroupsServiceImpl component = (ComputedGroupsServiceImpl) cgs;

        GroupComputerDescriptor desc = component.getComputerDescriptors().get(0);
        assertNotNull(desc);
        assertEquals("dummy", desc.getName());

        GroupComputer computer = desc.getComputer();
        assertNotNull(computer);
        assertTrue(computer.getAllGroupIds().contains("Grp1"));

        NuxeoGroup group = cgs.getComputedGroup("Grp1", um.getGroupConfig());
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
    public void testUserManagerIntegration() {

        boolean isUserManagerWithComputedGroups = false;
        if (um instanceof UserManagerWithComputedGroups) {
            isUserManagerWithComputedGroups = true;
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
        List<String> staticGroups = new ArrayList<>();
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
    public void testResolveMembersInVirtualGroup() {
        List<String> users = um.getUsersInGroupAndSubGroups("Grp1");
        assertEquals(2, users.size());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:companycomputedgroups-contrib.xml")
    public void testCompanyComputer() {
        dotTestCompanyComputer();
    }

    public void dotTestCompanyComputer() {

        Map<String, Serializable> filter = new HashMap<>();
        HashSet<String> fulltext = new HashSet<>();
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

    @Test(expected = NuxeoException.class)
    public void shouldNotCreateComputedGroup() {
        NuxeoGroup group = um.getGroup("Grp1");
        um.createGroup(group.getModel());
    }

    @Test(expected = NuxeoException.class)
    public void shouldNotUpdateComputedGroup() {
        NuxeoGroup group = um.getGroup("Grp1");
        um.updateGroup(group.getModel());
    }

    @Test(expected = NuxeoException.class)
    public void shouldNotDeleteComputedGroup() {
        NuxeoGroup group = um.getGroup("Grp1");
        um.deleteGroup(group.getName());
    }

}
