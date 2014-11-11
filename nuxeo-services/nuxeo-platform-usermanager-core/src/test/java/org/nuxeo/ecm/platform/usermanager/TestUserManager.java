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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author George Lefter
 * @author Florent Guillaume
 * @author Anahide Tchertchian
 */
public class TestUserManager extends NXRuntimeTestCase {

    protected UserManager userManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.core.schema", "OSGI-INF/SchemaService.xml");
        deployContrib("org.nuxeo.ecm.directory",
                "OSGI-INF/DirectoryService.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager",
                "OSGI-INF/UserService.xml");
        deployContrib("org.nuxeo.ecm.directory.sql",
                "OSGI-INF/SQLDirectoryFactory.xml");

        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/schemas-config.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/directory-config.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/userservice-config.xml");

        UserService service = (UserService) Framework.getRuntime().getComponent(
                UserService.NAME);

        userManager = service.getUserManager();
    }

    public void testConnect() {
        assertNotNull(userManager);
    }

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

    public void testGetAnonymous() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("Guest");
        assertNotNull(principal);
        assertEquals("Guest", principal.getName());
        assertEquals("Anonymous", principal.getFirstName());
        assertEquals("Coward", principal.getLastName());
        assertNull(principal.getCompany());
    }

    public void testGetVirtualUsers() throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal("MyCustomAdministrator");
        assertNotNull(principal);
        assertEquals("MyCustomAdministrator", principal.getName());
        assertEquals("My Custom", principal.getFirstName());
        assertEquals("Administrator", principal.getLastName());
        assertNull(principal.getCompany());
        assertTrue(principal.isMemberOf("administrators"));
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
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
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
    public void XXXtestMemoryLeak() throws Exception {
        deleteTestObjects();
        DocumentModel userModel = getUser("test_usr0");
        userManager.createUser(userModel);
        DocumentModel groupModel = getGroup("test_grp0");
        userManager.createGroup(groupModel);

        for (int i = 0; i < 100; i++) {
            String userName = "test_u" + i;
            userModel = getUser(userName);
            userModel.setProperty("user", "username", userName);
            userModel.setProperty("user", "groups", Arrays.asList("test_grp0"));
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

    public void testCreatePrincipal() throws Exception {
        deleteTestObjects();
        // force User Directory initialization first - so that the sql script
        // executes

        DocumentModel user = getUser("test_u1");
        DocumentModel group = getGroup("test_g1");

        userManager.createGroup(group);
        NuxeoGroup g1 = userManager.getGroup("test_g1");

        assertNotNull(g1);

        List<String> groupNames = Arrays.asList("test_g1");
        List<String> groupNamesWithDefault = Arrays.asList("defgr", "test_g1");
        List<String> roleNames = Arrays.asList("regular");
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

    public void testCreateGroup() throws Exception {
        deleteTestObjects();
        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        userManager.createUser(u1);
        userManager.createUser(u2);

        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = Arrays.asList("test_u1");
        List<String> g2Users = Arrays.asList("test_u1", "test_u2");
        List<String> g2Groups = Arrays.asList("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);

        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        NuxeoGroup newG1 = userManager.getGroup("test_g1");
        NuxeoGroup newG2 = userManager.getGroup("test_g2");

        assertNotNull(newG1);
        assertNotNull(newG2);

        assertEquals("test_g1", newG1.getName());
        assertEquals("test_g2", newG2.getName());
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

    public void testGetTopLevelGroups() throws Exception {
        deleteTestObjects();

        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g2Groups = Arrays.asList("test_g1");

        userManager.createGroup(g1);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedTopLevelGroups = Arrays.asList("administrators",
                "members", "test_g2");
        List<String> topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);

        assertEquals(expectedTopLevelGroups, topLevelGroups);

        // delete test_g2 and test if test_g1 is toplevel
        userManager.deleteGroup(g2);
        expectedTopLevelGroups = Arrays.asList("administrators", "members",
                "test_g1");
        topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(expectedTopLevelGroups, topLevelGroups);

        // re-create g2 as a parent of g1
        // test if g1 is not top-level and g2 is
        g2Groups = Arrays.asList("test_g1");
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);
        expectedTopLevelGroups = Arrays.asList("administrators", "members",
                "test_g2");
        topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(expectedTopLevelGroups, topLevelGroups);
    }

    /**
     * Test the method getUsersInGroup, making sure it does return only the
     * users of the group (and not the subgroups ones)
     * 
     * @throws Exception
     */
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

        List<String> g1Users = Arrays.asList("test_u1");
        List<String> g2Users = Arrays.asList("test_u2", "test_u2bis");

        List<String> g2Groups = Arrays.asList("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedUsersInGroup1 = Arrays.asList("test_u1");
        List<String> expectedUsersInGroup2 = Arrays.asList("test_u2bis",
                "test_u2");
        Collections.sort(expectedUsersInGroup1);
        Collections.sort(expectedUsersInGroup2);
        assertEquals(expectedUsersInGroup1,
                userManager.getUsersInGroup("test_g1"));
        assertEquals(expectedUsersInGroup2,
                userManager.getUsersInGroup("test_g2"));
    }

    /**
     * Test the method getUsersInGroupAndSubgroups, making sure it does return
     * all the users from a group and its subgroups.
     * 
     * @throws Exception
     */
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

        List<String> g1Users = Arrays.asList("test_u1");
        List<String> g2Users = Arrays.asList("test_u2", "test_u2bis");
        List<String> g2Groups = Arrays.asList("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedUsersInGroup1 = Arrays.asList("test_u1");
        List<String> usersInGroupAndSubGroups1 = userManager.getUsersInGroupAndSubGroups("test_g1");
        Collections.sort(expectedUsersInGroup1);
        Collections.sort(usersInGroupAndSubGroups1);
        assertEquals(expectedUsersInGroup1, usersInGroupAndSubGroups1);

        // should have all the groups from group1 and group2
        List<String> expectedUsersInGroup2 = Arrays.asList("test_u2bis",
                "test_u2", "test_u1");
        List<String> usersInGroupAndSubGroups2 = userManager.getUsersInGroupAndSubGroups("test_g2");
        Collections.sort(expectedUsersInGroup2);
        Collections.sort(usersInGroupAndSubGroups2);
        assertEquals(expectedUsersInGroup2, usersInGroupAndSubGroups2);

    }

    /**
     * Test the method getUsersInGroupAndSubgroups making sure it's not going
     * into an infinite loop when a subgroup is also parent of a group.
     * 
     * @throws Exception
     */
    public void testGetUsersInGroupAndSubgroupsWithoutInfiniteLoop()
            throws Exception {
        deleteTestObjects();

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = Arrays.asList("test_u1");
        List<String> g2Users = Arrays.asList("test_u2", "test_u2bis");
        List<String> g2Groups = Arrays.asList("test_g1");
        // group1 is also a subgroup of group2
        List<String> g1Groups = Arrays.asList("test_g2");

        g1.setProperty("group", "members", g1Users);
        g1.setProperty("group", "subGroups", g1Groups);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> expectedUsersInGroup2 = Arrays.asList("test_u2bis",
                "test_u2", "test_u1");
        // infinite loop can occure here:
        List<String> usersInGroupAndSubGroups2 = userManager.getUsersInGroupAndSubGroups("test_g2");
        Collections.sort(expectedUsersInGroup2);
        Collections.sort(usersInGroupAndSubGroups2);
        assertEquals(expectedUsersInGroup2, usersInGroupAndSubGroups2);

    }

    public void testDeletePrincipal() throws Exception {
        deleteTestObjects();
        DocumentModel user = getUser("test_u1");
        userManager.createUser(user);
        assertNotNull(userManager.getPrincipal("test_u1"));
        userManager.deleteUser(user);
        assertNull(userManager.getPrincipal("test_u1"));

        // try to delete the principal twice
        boolean gotException = false;
        try {
            userManager.deleteUser(user);
        } catch (ClientException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testDeleteGroup() throws Exception {
        deleteTestObjects();
        DocumentModel group = getGroup("test_g1");
        userManager.createGroup(group);
        assertNotNull(userManager.getGroup("test_g1"));
        userManager.deleteGroup(group);
        assertNull(userManager.getGroup("test_g1"));

        // try to delete the group twice
        boolean gotException = false;
        try {
            userManager.deleteGroup(group);
        } catch (ClientException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testSearchPrincipals() throws Exception {
        deleteTestObjects();
        userManager.createUser(getUser("test_u1"));
        userManager.createUser(getUser("test_u2"));

        List<NuxeoPrincipal> principals = userManager.searchPrincipals("test_");

        assertEquals(2, principals.size());
        String name1 = principals.get(0).getName();
        String name2 = principals.get(1).getName();
        assertTrue("test_u1".equals(name1) && "test_u2".equals(name2)
                || "test_u1".equals(name2) && "test_u2".equals(name1));
    }

    public void testUpdatePrincipal() throws Exception {
        deleteTestObjects();
        NuxeoPrincipal u1 = new NuxeoPrincipalImpl("test_u1");
        u1.setFirstName("fname1");
        u1.setLastName("lname1");
        u1.setCompany("company1");
        userManager.createPrincipal(u1);

        NuxeoGroup g1 = new NuxeoGroupImpl("test_g1");
        g1.setMemberUsers(Arrays.asList("test_u1"));
        userManager.createGroup(g1);

        NuxeoGroup g2 = new NuxeoGroupImpl("test_g2");
        g2.setMemberUsers(Arrays.asList("test_u1"));
        userManager.createGroup(g2);

        NuxeoGroup g3 = new NuxeoGroupImpl("test_g3");
        userManager.createGroup(g3);

        // refresh u1
        u1 = userManager.getPrincipal("test_u1");
        List<String> expectedGroups = Arrays.asList("defgr", "test_g1",
                "test_g2");
        List<String> groups = u1.getGroups();
        Collections.sort(groups);
        assertEquals(expectedGroups, groups);

        u1.setFirstName("fname2");
        u1.setLastName("lname2");
        u1.setCompany("company2");
        u1.getGroups().remove("test_g2"); // ???!!!
        u1.getGroups().add("test_g3");
        userManager.updatePrincipal(u1);

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

    public void testUpdateGroup() throws Exception {
        deleteTestObjects();
        // setup group g
        NuxeoPrincipal u1 = new NuxeoPrincipalImpl("test_u1");
        userManager.createPrincipal(u1);

        NuxeoPrincipal u2 = new NuxeoPrincipalImpl("test_u2");
        userManager.createPrincipal(u2);

        NuxeoPrincipal u3 = new NuxeoPrincipalImpl("test_u3");
        userManager.createPrincipal(u3);

        NuxeoGroup g1 = new NuxeoGroupImpl("test_g1");
        userManager.createGroup(g1);
        NuxeoGroup g2 = new NuxeoGroupImpl("test_g2");
        userManager.createGroup(g2);
        NuxeoGroup g3 = new NuxeoGroupImpl("test_g3");
        userManager.createGroup(g3);

        List<String> gUsers = Arrays.asList("test_u1", "test_u2");
        List<String> gGroups = Arrays.asList("test_g1", "test_g2");

        NuxeoGroup g = new NuxeoGroupImpl("test_g");
        g.setMemberUsers(gUsers);
        g.setMemberGroups(gGroups);
        userManager.createGroup(g);

        // update group g
        g.getMemberUsers().remove("test_u2");
        g.getMemberUsers().add("test_u3");
        g.getMemberGroups().remove("test_g2");
        g.getMemberGroups().add("test_g3");
        userManager.updateGroup(g);

        // check new group
        NuxeoGroup newG = userManager.getGroup("test_g");
        List<String> newGUsers = Arrays.asList("test_u1", "test_u3");
        List<String> newGGroups = Arrays.asList("test_g1", "test_g3");
        assertEquals(newGUsers, newG.getMemberUsers());
        assertEquals(newGGroups, newG.getMemberGroups());
        assertEquals(newG, g);
    }

}
