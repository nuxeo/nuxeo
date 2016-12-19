/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @author George Lefter
 * @author Florent Guillaume
 * @author Anahide Tchertchian
 */
@LocalDeploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUserManager extends UserManagerTestCase {

    @Inject
    protected RuntimeHarness harness;

    @Test
    public void testExistingSetup() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        List<String> groups = principal.getGroups();
        assertTrue(groups.contains("administrators"));
    }

    private DocumentModel getUser(String userId) throws Exception {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userId);
        return newUser;
    }

    private DocumentModel getGroup(String groupId) throws Exception {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupId);
        return newGroup;
    }

    @Test
    public void testGetAnonymous() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("Guest");
        assertNotNull(principal);
        assertEquals("Guest", principal.getName());
        assertEquals("Anonymous", principal.getFirstName());
        assertEquals("Coward", principal.getLastName());
        assertNull(principal.getCompany());
    }

    @Test
    public void testGetAdministrator() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("tehroot");
        assertNotNull(principal);
        assertTrue(principal.isAdministrator());
        assertTrue(principal.isMemberOf("administrators"));
        assertTrue(principal.isMemberOf("defgr"));
        assertFalse(principal.isMemberOf("myAdministrators"));
        assertEquals("tehroot", principal.getName());
        assertEquals("The", principal.getFirstName());
        assertEquals("Root", principal.getLastName());
        assertNull(principal.getCompany());
    }

    @Test
    public void testGetAdministratorOverride() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/userservice-override-config.xml");
        // user manager is recomputed after deployment => refetch it
        userManager = Framework.getService(UserManager.class);
        try {
            doTestGetAdministratorOverride();
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                    "test-usermanagerimpl/userservice-override-config.xml");
            // user manager is recomputed after undeployment => refetch it
            userManager = Framework.getService(UserManager.class);
        }
    }

    public void doTestGetAdministratorOverride() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("tehroot");
        assertNotNull(principal);
        assertTrue(principal.isAdministrator());
        // no administrators groups anymore
        assertFalse(principal.isMemberOf("administrators"));
        assertTrue(principal.isMemberOf("defgr"));
        // new administrators group as virtual
        assertTrue(principal.isMemberOf("myAdministrators"));
        assertEquals("tehroot", principal.getName());
        assertEquals("The", principal.getFirstName());
        assertEquals("Root", principal.getLastName());
        assertNull(principal.getCompany());
    }

    @Test
    public void testGetVirtualUsers() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("ClassicAdministrator");
        assertNotNull(principal);
        assertEquals("ClassicAdministrator", principal.getName());
        assertEquals("Classic", principal.getFirstName());
        assertEquals("Administrator", principal.getLastName());
        assertNull(principal.getCompany());
        assertTrue(principal.isMemberOf("administrators"));
        assertFalse(principal.isMemberOf("myAdministrators"));
        assertTrue(principal.isAdministrator());

        principal = userManager.getPrincipal("MyCustomAdministrator");
        assertNotNull(principal);
        assertEquals("MyCustomAdministrator", principal.getName());
        assertEquals("My Custom", principal.getFirstName());
        assertEquals("Administrator", principal.getLastName());
        assertNull(principal.getCompany());
        // test additional admin group
        assertFalse(principal.isMemberOf("administrators"));
        assertTrue(principal.isMemberOf("myAdministrators"));
        assertFalse(principal.isAdministrator());

        principal = userManager.getPrincipal("MyCustomMember");
        // error in logs normal, we check an extra field do not compromise the
        // main action
        assertNotNull(principal);
        assertEquals("MyCustomMember", principal.getName());
        assertEquals("My Custom", principal.getFirstName());
        assertEquals("Member", principal.getLastName());
        assertNull(principal.getCompany());
        // assertEquals(4, principal.getAllGroups().size());
        assertFalse(principal.isAdministrator());
        assertTrue(principal.isMemberOf("othergroup"));
        assertTrue(principal.isMemberOf("defgr"));
        // this one is taken from props
        assertTrue(principal.isMemberOf("members"));
        // group1 does not exist => not here
        assertFalse(principal.isMemberOf("group1"));
    }

    @Test
    public void testGetVirtualUsersOverride() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/userservice-override-config.xml");
        // user manager is recomputed after deployment => refetch it
        userManager = Framework.getService(UserManager.class);
        try {
            doTestGetVirtualUsersOverride();
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                    "test-usermanagerimpl/userservice-override-config.xml");
            // user manager is recomputed after undeployment => refetch it
            userManager = Framework.getService(UserManager.class);
        }
    }

    public void doTestGetVirtualUsersOverride() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("ClassicAdministrator");
        assertNotNull(principal);
        assertEquals("ClassicAdministrator", principal.getName());
        assertEquals("Classic", principal.getFirstName());
        assertEquals("Administrator", principal.getLastName());
        assertNull(principal.getCompany());
        assertTrue(principal.isMemberOf("administrators"));
        assertFalse(principal.isMemberOf("myAdministrators"));
        assertFalse(principal.isAdministrator());

        principal = userManager.getPrincipal("MyCustomAdministrator");
        assertNotNull(principal);
        assertEquals("MyCustomAdministrator", principal.getName());
        assertEquals("My Custom", principal.getFirstName());
        assertEquals("Administrator", principal.getLastName());
        assertNull(principal.getCompany());
        // test additional admin group
        assertFalse(principal.isMemberOf("administrators"));
        assertTrue(principal.isMemberOf("myAdministrators"));
        assertTrue(principal.isAdministrator());

        principal = userManager.getPrincipal("MyCustomMember");
        assertNotNull(principal);
        assertEquals("MyCustomMember", principal.getName());
        assertEquals("My Custom", principal.getFirstName());
        assertEquals("Member", principal.getLastName());
        assertNull(principal.getCompany());
        // assertEquals(4, principal.getAllGroups().size());
        assertFalse(principal.isAdministrator());
        assertTrue(principal.isMemberOf("othergroup"));
        assertTrue(principal.isMemberOf("defgr"));
        // this one is taken from props
        assertTrue(principal.isMemberOf("members"));
        // group1 does not exist => not here
        assertFalse(principal.isMemberOf("group1"));
    }

    @Test
    public void testGetAdministratorGroups() {
        List<String> adminGroups = userManager.getAdministratorsGroups();
        assertEquals(Collections.singletonList("administrators"), adminGroups);
    }

    @Test
    public void testGetAdministratorGroupsOverride() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/userservice-override-config.xml");
        // user manager is recomputed after deployment => refetch it
        userManager = Framework.getService(UserManager.class);
        try {
            doTestGetAdministratorGroupsOverride();
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                    "test-usermanagerimpl/userservice-override-config.xml");
            // user manager is recomputed after undeployment => refetch it
            userManager = Framework.getService(UserManager.class);
        }
    }

    public void doTestGetAdministratorGroupsOverride() throws Exception {
        List<String> adminGroups = userManager.getAdministratorsGroups();
        assertEquals(Collections.singletonList("myAdministrators"), adminGroups);
    }

    @Test
    public void testSearchAnonymous() throws Exception {
        DocumentModelList users;
        DocumentModel principal;

        users = userManager.searchUsers("Gu");
        assertEquals(1, users.size());

        principal = users.get(0);
        assertEquals("Guest", principal.getId());
        assertEquals("Anonymous", principal.getProperty("user", "firstName"));
        assertEquals("Coward", principal.getProperty("user", "lastName"));

        // search by map
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("lastName", "Cow");
        users = userManager.searchUsers(filter, filter.keySet());
        assertEquals(1, users.size());

        principal = users.get(0);
        assertEquals("Guest", principal.getId());

        // with a non-matching criterion
        filter.put("firstName", "Bob");
        users = userManager.searchUsers(filter, filter.keySet());
        assertEquals(0, users.size());

        // another search
        filter.clear();
        filter.put("username", "Gue");
        users = userManager.searchUsers(filter, filter.keySet());
        assertEquals(1, users.size());

        principal = users.get(0);
        assertEquals("Guest", principal.getId());

        // now add another non-Anonymous user that matches the same query
        DocumentModel newUser = getUser("Gudule");
        userManager.createUser(newUser);
        users = userManager.searchUsers("Gu");
        assertEquals(2, users.size());

        String name1 = users.get(0).getId();
        String name2 = users.get(1).getId();
        if (!name1.equals("Guest")) {
            final String tmp = name1;
            name1 = name2;
            name2 = tmp;
        }
        assertEquals("Guest", name1);
        assertEquals("Gudule", name2);
    }

    public void deleteTestObjects() throws Exception {
        List<String> users = userManager.getUserIds();
        List<String> groups = userManager.getGroupIds();
        for (String user : users) {
            if (user.startsWith("test_")) {
                userManager.deleteUser(user);
            }
        }
        for (String group : groups) {
            if (group.startsWith("test_")) {
                userManager.deleteGroup(group);
            }
        }
    }

    // resource-intensive test, disabled by default
    @Test
    @Ignore
    public void testMemoryLeak() throws Exception {
        deleteTestObjects();
        DocumentModel userModel = getUser("test_usr0");
        userManager.createUser(userModel);
        DocumentModel groupModel = getGroup("test_grp0");
        userManager.createGroup(groupModel);

        for (int i = 0; i < 100; i++) {
            String userName = "test_u" + i;
            userModel = getUser(userName);
            userModel.setProperty("user", "username", userName);
            userModel.setProperty("user", "groups", Collections.singletonList("test_grp0"));
            userManager.createUser(userModel);
        }

        for (int i = 0; i < 100; i++) {
            String groupName = "test_g" + i;
            groupModel = getGroup(groupName);
            groupModel.setProperty("group", "groupname", groupName);
            userManager.createGroup(groupModel);
        }

        for (int i = 0; i < 100; i++) {
            userManager.getGroupIds();
        }

        for (int i = 0; i < 100; i++) {
            userManager.getUserIds();
        }
    }

    @Test
    public void testCreatePrincipal() throws Exception {
        deleteTestObjects();
        // force User Directory initialization first - so that the sql script
        // executes

        DocumentModel user = getUser("test_u1");
        DocumentModel group = getGroup("test_g1");

        userManager.createGroup(group);
        NuxeoGroup g1 = userManager.getGroup("test_g1");

        assertNotNull(g1);

        List<String> groupNames = Collections.singletonList("test_g1");
        List<String> groupNamesWithDefault = Arrays.asList("defgr", "test_g1");
        List<String> roleNames = Collections.singletonList("regular");
        user.setProperty("user", "firstName", "fname1");
        user.setProperty("user", "lastName", "lname1");
        user.setProperty("user", "company", "company1");
        user.setProperty("user", "groups", groupNames);

        userManager.createUser(user);

        NuxeoPrincipal newPrincipal = userManager.getPrincipal("test_u1");
        assertNotNull(newPrincipal);
        assertEquals("test_u1", newPrincipal.getName());
        assertEquals("fname1", newPrincipal.getFirstName());
        assertEquals("lname1", newPrincipal.getLastName());
        assertEquals("company1", newPrincipal.getCompany());

        List<String> groups = newPrincipal.getGroups();
        Collections.sort(groups);
        assertEquals(groupNamesWithDefault, groups);
        assertEquals(roleNames, newPrincipal.getRoles());
        assertEquals("test_u1", newPrincipal.getName());

        try {
            userManager.createUser(user);
            fail("Should have raised UserAlreadyExistsException");
        } catch (UserAlreadyExistsException e) {
            // ok
        }
    }

    @Test
    public void testCreateGroup() throws Exception {
        deleteTestObjects();
        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        userManager.createUser(u1);
        userManager.createUser(u2);

        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");
        DocumentModel g3 = getGroup("test_g3");
        g3.setPropertyValue("group:grouplabel", "test_g3_label");

        List<String> g1Users = Collections.singletonList("test_u1");
        List<String> g2Users = Arrays.asList("test_u1", "test_u2");
        List<String> g2Groups = Collections.singletonList("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);

        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        // without users / groups
        userManager.createGroup(g3);

        NuxeoGroup newG1 = userManager.getGroup("test_g1");
        NuxeoGroup newG2 = userManager.getGroup("test_g2");
        NuxeoGroup newG3 = userManager.getGroup("test_g3");

        assertNotNull(newG1);
        assertNotNull(newG2);
        assertNotNull(newG3);

        assertEquals("test_g1", newG1.getName());
        assertEquals("test_g2", newG2.getName());
        assertEquals("test_g3", newG3.getName());
        assertEquals("test_g1", newG1.getLabel());
        assertEquals("test_g2", newG2.getLabel());
        assertEquals("test_g3_label", newG3.getLabel());
        assertEquals(g1Users, newG1.getMemberUsers());
        assertEquals(g2Users, newG2.getMemberUsers());
        assertEquals(g2Groups, newG2.getMemberGroups());

        // try to create the group again and test if an exception is thrown
        try {
            userManager.createGroup(g1);
            fail("Should have raised GroupAlreadyExistsException");
        } catch (GroupAlreadyExistsException e) {
            // ok
        }
    }

    @Test
    public void testGetTopLevelGroups() throws Exception {
        deleteTestObjects();

        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g2Groups = Collections.singletonList("test_g1");

        userManager.createGroup(g1);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedTopLevelGroups = Arrays.asList("administrators", "members", "powerusers", "test_g2");
        List<String> topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);

        assertEquals(expectedTopLevelGroups, topLevelGroups);

        // delete test_g2 and test if test_g1 is toplevel
        userManager.deleteGroup(g2);
        expectedTopLevelGroups = Arrays.asList("administrators", "members", "powerusers", "test_g1");
        topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(expectedTopLevelGroups, topLevelGroups);

        // re-create g2 as a parent of g1
        // test if g1 is not top-level and g2 is
        g2Groups = Collections.singletonList("test_g1");
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);
        expectedTopLevelGroups = Arrays.asList("administrators", "members", "powerusers", "test_g2");
        topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(expectedTopLevelGroups, topLevelGroups);
    }

    /**
     * Test the method getUsersInGroup, making sure it does return only the users of the group (and not the subgroups
     * ones).
     */
    @Test
    public void testGetUsersInGroup() throws Exception {
        deleteTestObjects();

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = Collections.singletonList("test_u1");
        List<String> g2Users = Arrays.asList("test_u2", "test_u2bis");

        List<String> g2Groups = Collections.singletonList("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedUsersInGroup1 = Collections.singletonList("test_u1");
        List<String> expectedUsersInGroup2 = Arrays.asList("test_u2bis", "test_u2");
        Collections.sort(expectedUsersInGroup1);
        Collections.sort(expectedUsersInGroup2);
        assertEquals(expectedUsersInGroup1, userManager.getUsersInGroup("test_g1"));
        assertEquals(expectedUsersInGroup2, userManager.getUsersInGroup("test_g2"));
    }

    /**
     * Test the method getUsersInGroupAndSubgroups, making sure it does return all the users from a group and its
     * subgroups.
     */
    @Test
    public void testGetUsersInGroupAndSubgroups() throws Exception {
        deleteTestObjects();

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = Collections.singletonList("test_u1");
        List<String> g2Users = Arrays.asList("test_u2", "test_u2bis");
        List<String> g2Groups = Collections.singletonList("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedUsersInGroup1 = Collections.singletonList("test_u1");
        List<String> usersInGroupAndSubGroups1 = userManager.getUsersInGroupAndSubGroups("test_g1");
        Collections.sort(expectedUsersInGroup1);
        Collections.sort(usersInGroupAndSubGroups1);
        assertEquals(expectedUsersInGroup1, usersInGroupAndSubGroups1);

        // should have all the groups from group1 and group2
        List<String> expectedUsersInGroup2 = Arrays.asList("test_u2bis", "test_u2", "test_u1");
        List<String> usersInGroupAndSubGroups2 = userManager.getUsersInGroupAndSubGroups("test_g2");
        Collections.sort(expectedUsersInGroup2);
        Collections.sort(usersInGroupAndSubGroups2);
        assertEquals(expectedUsersInGroup2, usersInGroupAndSubGroups2);
    }

    /**
     * Test the method getUsersInGroupAndSubgroups making sure it's not going into an infinite loop when a subgroup is
     * also parent of a group.
     */
    @Test
    public void testGetUsersInGroupAndSubgroupsWithoutInfiniteLoop() throws Exception {
        deleteTestObjects();

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = Collections.singletonList("test_u1");
        List<String> g2Users = Arrays.asList("test_u2", "test_u2bis");
        List<String> g2Groups = Collections.singletonList("test_g1");
        // group1 is also a subgroup of group2
        List<String> g1Groups = Collections.singletonList("test_g2");

        g1.setProperty("group", "members", g1Users);
        g1.setProperty("group", "subGroups", g1Groups);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedUsersInGroup2 = Arrays.asList("test_u2bis", "test_u2", "test_u1");
        // infinite loop can occure here:
        List<String> usersInGroupAndSubGroups2 = userManager.getUsersInGroupAndSubGroups("test_g2");
        Collections.sort(expectedUsersInGroup2);
        Collections.sort(usersInGroupAndSubGroups2);
        assertEquals(expectedUsersInGroup2, usersInGroupAndSubGroups2);
    }

    @Test
    public void testDeletePrincipal() throws Exception {
        deleteTestObjects();
        DocumentModel user = getUser("test_u1");
        userManager.createUser(user);
        assertNotNull(userManager.getPrincipal("test_u1"));
        userManager.deleteUser(user);
        assertNull(userManager.getPrincipal("test_u1"));

        // try to delete the principal twice
        try {
            userManager.deleteUser(user);
            fail();
        } catch (DirectoryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("User does not exist: test_u1"));
        }
    }

    @Test
    public void testDeleteGroup() throws Exception {
        deleteTestObjects();
        DocumentModel group = getGroup("test_g1");
        userManager.createGroup(group);
        assertNotNull(userManager.getGroup("test_g1"));
        userManager.deleteGroup(group);
        assertNull(userManager.getGroup("test_g1"));

        // try to delete the group twice
        try {
            userManager.deleteGroup(group);
            fail();
        } catch (DirectoryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Group does not exist: test_g1"));
        }
    }

    @Test
    public void testSearchUser() throws Exception {
        assertEquals(0, userManager.searchUsers("test").size());

        DocumentModel doc = getUser("test");
        userManager.createUser(doc);
        doc = getUser("test_2");
        userManager.createUser(doc);
        assertEquals(2, userManager.searchUsers("test").size());

        doc = getUser("else");
        doc.setProperty("user", "firstName", "test");
        userManager.createUser(doc);
        assertEquals(3, userManager.searchUsers("test").size());

        doc = getGroup("group");
        userManager.createGroup(doc);
        doc = getGroup("group_1");
        userManager.createGroup(doc);

        assertEquals(2, userManager.searchGroups("group").size());

        doc = getGroup("else");
        doc.setProperty("group", "grouplabel", "group");
        userManager.createGroup(doc);
        assertEquals(3, userManager.searchGroups("group").size());
    }

    @Test
    public void testUpdatePrincipal() throws Exception {
        deleteTestObjects();
        NuxeoPrincipal u1 = new NuxeoPrincipalImpl("test_u1");
        u1.setFirstName("fname1");
        u1.setLastName("lname1");
        u1.setCompany("company1");
        DocumentModel u1Model = userManager.createUser(u1.getModel());

        DocumentModel g1 = getGroup("test_g1");
        g1 = userManager.createGroup(g1);

        DocumentModel g2 = getGroup("test_g2");
        g2 = userManager.createGroup(g2);

        DocumentModel g3 = getGroup("test_g3");
        g3 = userManager.createGroup(g3);

        u1Model.setProperty("user", "groups", Arrays.asList("test_g1", "test_g2"));
        userManager.updateUser(u1Model);

        // refresh u1
        u1 = userManager.getPrincipal("test_u1");
        List<String> expectedGroups = Arrays.asList("defgr", "test_g1", "test_g2");
        List<String> groups = u1.getGroups();
        Collections.sort(groups);
        assertEquals(expectedGroups, groups);

        u1.setFirstName("fname2");
        u1.setLastName("lname2");
        u1.setCompany("company2");
        u1.getGroups().remove("test_g2"); // ???!!!
        u1.getGroups().add("test_g3");
        userManager.updateUser(u1.getModel());

        NuxeoPrincipal newU1 = userManager.getPrincipal("test_u1");
        assertNotNull(newU1);
        assertEquals("test_u1", newU1.getName());
        assertEquals("fname2", newU1.getFirstName());
        assertEquals("lname2", newU1.getLastName());
        assertEquals("company2", newU1.getCompany());
        assertEquals(newU1.getName(), u1.getName());
        assertEquals(newU1.getGroups(), u1.getGroups());
        assertEquals(newU1.getRoles(), u1.getRoles());
    }

    @Test
    public void testUpdateGroupLabel() throws Exception {
        deleteTestObjects();

        DocumentModel groupModel = getGroup("test_g");
        groupModel.setProperty("group", "grouplabel", "test group");
        groupModel = userManager.createGroup(groupModel);

        NuxeoGroup group = userManager.getGroup("test_g");
        assertEquals("test group", group.getLabel());

        groupModel.setProperty("group", "grouplabel", "another group");
        userManager.updateGroup(groupModel);

        group = userManager.getGroup("test_g");
        assertEquals("another group", group.getLabel());

    }

    @Test
    public void testUpdateGroup() throws Exception {
        deleteTestObjects();
        // setup group g
        DocumentModel u1 = getUser("test_u1");
        userManager.createUser(u1);

        DocumentModel u2 = getUser("test_u2");
        userManager.createUser(u2);

        DocumentModel u3 = getUser("test_u3");
        userManager.createUser(u3);

        DocumentModel g1 = getGroup("test_g1");
        userManager.createGroup(g1);
        DocumentModel g2 = getGroup("test_g2");
        userManager.createGroup(g2);
        DocumentModel g3 = getGroup("test_g3");
        userManager.createGroup(g3);

        List<String> gUsers = Arrays.asList("test_u1", "test_u2");
        List<String> gGroups = Arrays.asList("test_g1", "test_g2");

        DocumentModel g = getGroup("test_g");
        g.setProperty("group", "members", gUsers);
        g.setProperty("group", "subGroups", gGroups);
        g = userManager.createGroup(g);

        // update group g
        gUsers = new ArrayList<>(Arrays.asList("test_u1", "test_u3"));
        gGroups = new ArrayList<>(Arrays.asList("test_g1", "test_g3"));
        g.setProperty("group", "members", gUsers);
        g.setProperty("group", "subGroups", gGroups);
        userManager.updateGroup(g);

        // check new group
        NuxeoGroup newG = userManager.getGroup("test_g");
        List<String> newGUsers = Arrays.asList("test_u1", "test_u3");
        List<String> newGGroups = Arrays.asList("test_g1", "test_g3");
        List<String> actualUsers = newG.getMemberUsers();
        Collections.sort(actualUsers);
        assertEquals(newGUsers, actualUsers);
        List<String> actualGroups = newG.getMemberGroups();
        Collections.sort(actualGroups);
        assertEquals(newGGroups, actualGroups);
    }

    /**
     * common init method for initialising tests for the method getUsernamesForPermission.
     */
    private void initTestGetUsernamesForPermission() throws Exception {
        userManager.getPrincipal("Administrator"); // creates tables
        deleteTestObjects();
        userManager.createUser(getUser("alex"));
        userManager.createUser(getUser("bree"));
        userManager.createUser(getUser("jdoe"));
        userManager.createUser(getUser("stef"));

        List<String> g1Users = Arrays.asList("alex", "stef");
        DocumentModel g1 = getGroup("group1");
        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);

        List<String> g2Users = Arrays.asList("alex", "bree");
        DocumentModel g2 = getGroup("group2");
        g2.setProperty("group", "members", g2Users);
        userManager.createGroup(g2);

        // group3 has jdoe and a subgroup: g2
        List<String> g3Users = Collections.singletonList("jdoe");
        List<String> g3SubGroups = Collections.singletonList("group2");
        DocumentModel g3 = getGroup("group3");
        g3.setProperty("group", "members", g3Users);
        g3.setProperty("group", "subGroups", g3SubGroups);
        userManager.createGroup(g3);
    }

    /**
     * Testing the method getUsernamesForPermission for a simple case.
     */
    @Test
    public void testGetUsernamesForPermission() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acl.add(new ACE("alex", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));

        List<String> expectedUsers = Arrays.asList("Administrator", "alex", "jdoe", "bree");
        Collections.sort(users);
        Collections.sort(expectedUsers);

        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    /**
     * Testing the method getUsernamesForPermission for a simple case.
     */
    @Test
    public void testGetUsernamesForPermission2() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(ACE.BLOCK);
        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acl.add(new ACE("alex", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));

        List<String> expectedUsers = Collections.singletonList("alex");
        Collections.sort(users);
        Collections.sort(expectedUsers);

        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    /**
     * Same test as before but without the first ace (default value: everyone, everything false).
     */
    @Test
    public void testGetUsernamesForPermissionWithoutEveryoneEverythingACE() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();

        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acl.add(new ACE("alex", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));

        List<String> expectedUsers = Collections.singletonList("alex");
        Collections.sort(users);
        Collections.sort(expectedUsers);

        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    /**
     * Testing getUsernamesForPermission with a user in 2 groups.
     */
    @Test
    public void testGetUsernamesForPermissionIn2Groups() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group2", SecurityConstants.READ, false));
        acl.add(new ACE("group1", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));

        // Should contain alex and stef (in group1) and jdoe (in none of these
        // groups) but not bree (in group2)

        List<String> expectedUsers = Arrays.asList("Administrator", "alex", "stef", "jdoe");
        Collections.sort(users);
        Collections.sort(expectedUsers);

        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    /**
     * Testing getUsernamesForPermission with compound permission. For example, READ_WRITE contains READ.
     */
    @Test
    public void testGetUsernamesForPermissionWithCompoundPermission() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group2", SecurityConstants.READ_WRITE, false));
        acl.add(new ACE("group1", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));
        // Should contain alex and stef (in group1) and jdoe (in none of these
        // groups) but not bree (in group2)
        List<String> expectedUsers = Arrays.asList("Administrator", "alex", "stef", "jdoe");
        Collections.sort(users);
        Collections.sort(expectedUsers);

        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    /**
     * Testing getUsernamesForPermission with a ACP having more than one ACL
     */
    @Test
    public void testGetUsernamesForPermissionWithMultipleACL() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(ACL.INHERITED_ACL);
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group2", SecurityConstants.READ_WRITE, false));
        acp.addACL(acl);

        ACLImpl acl2 = new ACLImpl(ACL.LOCAL_ACL);
        acl2.add(new ACE("group1", SecurityConstants.READ, true));
        acp.addACL(acl2);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));
        // Should contain stef (in group1) and jdoe (in none of these
        // groups) but not bree (in group2) neither alex (in group1 and group2)
        List<String> expectedUsers = Arrays.asList("Administrator", "stef", "jdoe");
        Collections.sort(users);
        Collections.sort(expectedUsers);
        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    /**
     * Testing getUsernamesForPermission with subgroups.
     */
    @Test
    public void testGetUsernamesForPermissionWithSubGroups() throws Exception {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE("group3", SecurityConstants.READ_WRITE, true));
        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acp.addACL(acl);

        List<String> users = Arrays.asList(userManager.getUsersForPermission(SecurityConstants.READ, acp));
        // group3 and group2 but alex should have read access
        List<String> expectedUsers = Arrays.asList("bree", "jdoe");
        Collections.sort(users);
        Collections.sort(expectedUsers);
        assertEquals("Expected users having read access are ", expectedUsers, users);
    }

    @Test
    public void testUsersAndGroupsWithSpaces() throws Exception {

        String userNameWithSpaces = " test_u1 ";
        String groupNameWithSpaces = " test_g1 ";

        deleteTestObjects();
        DocumentModel u1 = getUser(userNameWithSpaces);
        u1 = userManager.createUser(u1);

        assertTrue(userManager.searchUsers(userNameWithSpaces).size() == 1);
        assertNotNull(userManager.getUserModel(userNameWithSpaces));

        assertTrue(userManager.searchUsers(userNameWithSpaces.trim()).size() == 1);
        assertNotNull(userManager.getUserModel(userNameWithSpaces.trim()));

        DocumentModel g1 = getGroup(groupNameWithSpaces);

        List<String> g1Users = Collections.singletonList(u1.getId());
        g1.setProperty("group", "members", g1Users);
        g1 = userManager.createGroup(g1);

        assertTrue(userManager.searchGroups(groupNameWithSpaces).size() == 1);
        assertNotNull(userManager.getGroup(groupNameWithSpaces));

        assertTrue(userManager.searchGroups(groupNameWithSpaces.trim()).size() == 1);
        assertNotNull(userManager.getGroup(groupNameWithSpaces.trim()));

        NuxeoPrincipal up1 = userManager.getPrincipal(userNameWithSpaces);
        assertNotNull(up1);
        up1 = userManager.getPrincipal(userNameWithSpaces.trim());
        assertNotNull(up1);

        assertTrue(up1.getGroups().contains(groupNameWithSpaces.trim()));

    }

    @Test
    public void testTransientUsers() {
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        assertFalse(principal.isTransient());

        String transientUsername = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        assertTrue(NuxeoPrincipal.isTransientUsername(transientUsername));
        principal = userManager.getPrincipal(transientUsername);
        assertNotNull(principal);
        assertTrue(principal.isTransient());
        assertFalse(principal.isAdministrator());
        assertFalse(principal.isAnonymous());
        assertTrue(principal.getAllGroups().isEmpty());
        assertEquals("leela@nuxeo.com", principal.getFirstName());
        assertEquals("leela@nuxeo.com", principal.getEmail());
        assertEquals(transientUsername, principal.getName());
    }

    @Test
    public void testCacheAlter() {
        // Given we use a cache
        assertNotNull(((UserManagerImpl) userManager).principalCache);
        // Given a principal
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        // When I alter the principal without saving it
        String value = principal.getFirstName();
        principal.setFirstName("pfouh");
        // Then the cached principal is not altered
        assertEquals(value, userManager.getPrincipal("Administrator").getFirstName());
        // When I save it
        userManager.updateUser(principal.getModel());
        // Then the cached principal is altered
        assertEquals("pfouh", userManager.getPrincipal("Administrator").getFirstName());
    }

    @Test
    public void testPrincipalSerialization() throws IOException, ClassNotFoundException {
        class DebuggingObjectOutputStream extends ObjectOutputStream {

            final List<Object> stack = new ArrayList<>();

            DebuggingObjectOutputStream(OutputStream out) throws IOException {
                super(out);
                enableReplaceObject(true);
            }

            /**
             * Abuse {@code replaceObject()} as a hook to maintain our stack.
             */
            @Override
            protected Object replaceObject(Object o) {
                stack.add(o);
                return o;
            }

        }

        class DebuggingObjectInputStream extends ObjectInputStream {
            DebuggingObjectInputStream(InputStream in) throws IOException {
                super(in);
                enableResolveObject(true);
            }

            final List<Object> stack = new ArrayList<>();

            @Override
            protected Object resolveObject(Object obj) throws IOException {
                Object resolveObject = super.resolveObject(obj);
                stack.add(resolveObject);
                return resolveObject;
            }
        }
        NuxeoPrincipal original = userManager.getPrincipal("Administrator");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DebuggingObjectOutputStream oos = new DebuggingObjectOutputStream(bos)) {
            oos.writeObject(original);
            assertEquals(NuxeoPrincipalImpl.TransferableClone.DataTransferObject.class, oos.stack.get(0).getClass());
            assertEquals("Administrator", oos.stack.get(1));
        }
        try (DebuggingObjectInputStream ois = new DebuggingObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()))) {
            assertEquals(original, ois.readObject());
            assertEquals("Administrator", ois.stack.get(0));
            assertEquals(NuxeoPrincipalImpl.TransferableClone.class, ois.stack.get(1).getClass());
        }
    }

}
