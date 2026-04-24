/*
 * (C) Copyright 2006-2025 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
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
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @author George Lefter
 * @author Florent Guillaume
 * @author Anahide Tchertchian
 */
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUserManager extends UserManagerTestCase {

    @Test
    public void testExistingSetup() {
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        List<String> groups = principal.getGroups();
        assertTrue(groups.contains("administrators"));
    }

    private DocumentModel getUser(String userId) {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userId);
        return newUser;
    }

    private DocumentModel getGroup(String groupId) {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupId);
        return newGroup;
    }

    @Test
    public void testGetAnonymous() {
        NuxeoPrincipal principal = userManager.getPrincipal("Guest");
        assertNotNull(principal);
        assertEquals("Guest", principal.getName());
        assertEquals("Anonymous", principal.getFirstName());
        assertEquals("Coward", principal.getLastName());
        assertNull(principal.getCompany());
    }

    @Test
    public void testGetAdministrator() {
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
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/userservice-override-config.xml")
    public void testGetAdministratorOverride() {
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
    public void testGetVirtualUsers() {
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
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/userservice-override-config.xml")
    public void testGetVirtualUsersOverride() {
        doTestGetVirtualUsersOverride();
    }

    public void doTestGetVirtualUsersOverride() {
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
        assertEquals(List.of("administrators"), adminGroups);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/userservice-override-config.xml")
    public void testGetAdministratorGroupsOverride() {
        doTestGetAdministratorGroupsOverride();
    }

    public void doTestGetAdministratorGroupsOverride() {
        List<String> adminGroups = userManager.getAdministratorsGroups();
        assertEquals(List.of("myAdministrators"), adminGroups);
    }

    @Test
    public void testSearchAnonymous() {
        DocumentModelList users;
        DocumentModel principal;

        users = userManager.searchUsers("Gu");
        assertEquals(1, users.size());

        principal = users.getFirst();
        assertEquals("Guest", principal.getId());
        assertEquals("Anonymous", principal.getProperty("user", "firstName"));
        assertEquals("Coward", principal.getProperty("user", "lastName"));

        // search by map
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("lastName", "Cow");
        users = userManager.searchUsers(filter, filter.keySet());
        assertEquals(1, users.size());

        principal = users.getFirst();
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

        principal = users.getFirst();
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

    @Test
    public void testSearchAnonymousWithQueryBuilder() {
        QueryBuilder queryBuilder;
        DocumentModelList users;

        // add other users
        DocumentModel user1 = getUser("Gudule");
        userManager.createUser(user1);
        DocumentModel user2 = getUser("Gustave");
        userManager.createUser(user2);

        // no match
        queryBuilder = new QueryBuilder().predicate(Predicates.like("firstName", "NoSuchUserFirstName"))
                                         .countTotal(true);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(0, users.size());
        assertEquals(0, users.totalSize());
        // same without counting total
        queryBuilder.countTotal(false);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(0, users.size());
        assertEquals(0, users.totalSize()); // total is available anyway

        // search all
        queryBuilder = new QueryBuilder().order(OrderByExprs.asc("username")).countTotal(true);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(4, users.size());
        assertEquals(4, users.totalSize());
        assertEquals("Administrator", users.get(0).getId());
        assertEquals("Gudule", users.get(1).getId());
        assertEquals("Guest", users.get(2).getId());
        assertEquals("Gustave", users.get(3).getId());
        // same without counting total
        queryBuilder.countTotal(false);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(4, users.size());
        assertEquals(4, users.totalSize()); // total is available anyway

        // match 3 users including Guest
        queryBuilder = new QueryBuilder().predicate(Predicates.like("username", "Gu%"))
                                         .order(OrderByExprs.asc("username"))
                                         .countTotal(true);
        // search all
        users = userManager.searchUsers(queryBuilder);
        assertEquals(3, users.size());
        assertEquals(3, users.totalSize());
        assertEquals("Gudule", users.get(0).getId());
        assertEquals("Guest", users.get(1).getId());
        assertEquals("Gustave", users.get(2).getId());
        // same without counting total
        queryBuilder.countTotal(false);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(3, users.size());
        assertEquals(3, users.totalSize()); // total is available anyway

        // with offset
        queryBuilder.limit(10).offset(1).countTotal(true);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(2, users.size());
        assertEquals(3, users.totalSize());
        assertEquals("Guest", users.get(0).getId());
        assertEquals("Gustave", users.get(1).getId());
        // same without counting total
        queryBuilder.countTotal(false);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(2, users.size());
        assertEquals(-2, users.totalSize());
    }

    public void deleteTestObjects() {
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
    public void testMemoryLeak() {
        deleteTestObjects();
        DocumentModel userModel = getUser("test_usr0");
        userManager.createUser(userModel);
        DocumentModel groupModel = getGroup("test_grp0");
        userManager.createGroup(groupModel);

        for (int i = 0; i < 100; i++) {
            String userName = "test_u" + i;
            userModel = getUser(userName);
            userModel.setProperty("user", "username", userName);
            userModel.setProperty("user", "groups", List.of("test_grp0"));
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
    public void testCreatePrincipal() {
        deleteTestObjects();
        // force User Directory initialization first - so that the sql script
        // executes

        DocumentModel user = getUser("test_u1");
        DocumentModel group = getGroup("test_g1");

        userManager.createGroup(group);
        NuxeoGroup g1 = userManager.getGroup("test_g1");

        assertNotNull(g1);

        user.setProperty("user", "firstName", "fname1");
        user.setProperty("user", "lastName", "lname1");
        user.setProperty("user", "company", "company1");
        user.setProperty("user", "groups", List.of("test_g1"));

        userManager.createUser(user);

        NuxeoPrincipal newPrincipal = userManager.getPrincipal("test_u1");
        assertNotNull(newPrincipal);
        assertEquals("test_u1", newPrincipal.getName());
        assertEquals("fname1", newPrincipal.getFirstName());
        assertEquals("lname1", newPrincipal.getLastName());
        assertEquals("company1", newPrincipal.getCompany());

        List<String> groups = newPrincipal.getGroups();
        Collections.sort(groups);
        assertEquals(List.of("defgr", "test_g1"), groups); // default group is added by userManager
        assertEquals(List.of("regular"), newPrincipal.getRoles());
        assertEquals("test_u1", newPrincipal.getName());

        assertThrows(UserAlreadyExistsException.class, () -> userManager.createUser(user));
    }

    @Test
    public void testCreateGroup() {
        deleteTestObjects();
        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        userManager.createUser(u1);
        userManager.createUser(u2);

        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");
        DocumentModel g3 = getGroup("test_g3");
        g3.setPropertyValue("group:grouplabel", "test_g3_label");

        List<String> g1Users = List.of("test_u1");
        List<String> g2Users = List.of("test_u1", "test_u2");
        List<String> g2Groups = List.of("test_g1");

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
        assertThrows(GroupAlreadyExistsException.class, () -> userManager.createGroup(g1));
    }

    @Test
    public void testGetTopLevelGroups() {
        deleteTestObjects();

        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g2Groups = List.of("test_g1");

        userManager.createGroup(g1);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        List<String> topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(List.of("administrators", "members", "powerusers", "test_g2"), topLevelGroups);

        // delete test_g2 and test if test_g1 is toplevel
        userManager.deleteGroup(g2);
        topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(List.of("administrators", "members", "powerusers", "test_g1"), topLevelGroups);

        // re-create g2 as a parent of g1
        // test if g1 is not top-level and g2 is
        g2Groups = List.of("test_g1");
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);
        topLevelGroups = userManager.getTopLevelGroups();
        Collections.sort(topLevelGroups);
        assertEquals(List.of("administrators", "members", "powerusers", "test_g2"), topLevelGroups);
    }

    /**
     * Test the method getUsersInGroup, making sure it does return only the users of the group (and not the subgroups
     * ones).
     */
    @Test
    public void testGetUsersInGroup() {
        deleteTestObjects();

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = List.of("test_u1");
        List<String> g2Users = List.of("test_u2", "test_u2bis");

        List<String> g2Groups = List.of("test_g1");

        g1.setProperty("group", "members", g1Users);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        assertEquals(List.of("test_u1"), userManager.getUsersInGroup("test_g1"));
        assertEquals(List.of("test_u2", "test_u2bis"), userManager.getUsersInGroup("test_g2"));
    }

    /**
     * Test the method getUsersInGroupAndSubgroups, making sure it does return all the users from a group and its
     * subgroups.
     */
    @Test
    public void testGetUsersInGroupAndSubgroups() {
        deleteTestObjects();

        // G2 = (u2,u2bis)
        // |->G1 = (u1)
        // |->G3 = (u3)

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");
        DocumentModel u3 = getUser("test_u3");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        userManager.createUser(u3);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");
        DocumentModel g3 = getGroup("test_g3");

        List<String> g1Users = List.of("test_u1");
        List<String> g1Groups = List.of("test_g3");
        List<String> g2Users = List.of("test_u2", "test_u2bis");
        List<String> g2Groups = List.of("test_g1");
        List<String> g3Users = List.of("test_u3");

        g1.setProperty("group", "members", g1Users);
        g1.setProperty("group", "subGroups", g1Groups);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);
        g3.setProperty("group", "members", g3Users);
        userManager.createGroup(g3);

        List<String> usersInGroupAndSubGroups1 = userManager.getUsersInGroupAndSubGroups("test_g1");
        Collections.sort(usersInGroupAndSubGroups1);
        assertEquals(List.of("test_u1", "test_u3"), usersInGroupAndSubGroups1);

        // should have all the groups from group1 and group2
        List<String> usersInGroupAndSubGroups2 = userManager.getUsersInGroupAndSubGroups("test_g2");
        Collections.sort(usersInGroupAndSubGroups2);
        assertEquals(List.of("test_u1", "test_u2", "test_u2bis", "test_u3"), usersInGroupAndSubGroups2);
    }

    /**
     * Test the method getUsersInGroupAndSubgroups making sure it's not going into an infinite loop when a subgroup is
     * also parent of a group.
     */
    @Test
    public void testGetUsersInGroupAndSubgroupsWithoutInfiniteLoop() {
        deleteTestObjects();

        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
        DocumentModel u2bis = getUser("test_u2bis");

        userManager.createUser(u1);
        userManager.createUser(u2);
        userManager.createUser(u2bis);
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");

        List<String> g1Users = List.of("test_u1");
        List<String> g2Users = List.of("test_u2", "test_u2bis");
        List<String> g2Groups = List.of("test_g1");
        // group1 is also a subgroup of group2
        List<String> g1Groups = List.of("test_g2");

        g1.setProperty("group", "members", g1Users);
        g1.setProperty("group", "subGroups", g1Groups);
        userManager.createGroup(g1);
        g2.setProperty("group", "members", g2Users);
        g2.setProperty("group", "subGroups", g2Groups);
        userManager.createGroup(g2);

        // infinite loop can occur here:
        List<String> usersInGroupAndSubGroups2 = userManager.getUsersInGroupAndSubGroups("test_g2");
        Collections.sort(usersInGroupAndSubGroups2);
        assertEquals(List.of("test_u1", "test_u2", "test_u2bis"), usersInGroupAndSubGroups2);

        // and here
        List<String> g1AncestorGroups = userManager.getAncestorGroups("test_g1");
        Collections.sort(g1AncestorGroups);
        assertEquals(List.of("test_g1", "test_g2"), g1AncestorGroups);
    }

    @Test
    public void testDeletePrincipal() {
        deleteTestObjects();
        DocumentModel user = getUser("test_u1");
        userManager.createUser(user);
        assertNotNull(userManager.getPrincipal("test_u1"));
        userManager.deleteUser(user);
        assertNull(userManager.getPrincipal("test_u1"));

        // try to delete the principal twice
        var e = assertThrows(DirectoryException.class, () -> userManager.deleteUser(user));
        assertTrue(e.getMessage(), e.getMessage().contains("User does not exist: test_u1"));
    }

    @Test
    public void testDeleteGroup() {
        deleteTestObjects();
        DocumentModel group = getGroup("test_g1");
        userManager.createGroup(group);
        assertNotNull(userManager.getGroup("test_g1"));
        userManager.deleteGroup(group);
        assertNull(userManager.getGroup("test_g1"));

        // try to delete the group twice
        var e = assertThrows(DirectoryException.class, () -> userManager.deleteGroup(group));
        assertTrue(e.getMessage(), e.getMessage().contains("Group does not exist: test_g1"));
    }

    @Test
    public void testSearchUser() {
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
    public void testSearchUsersWithQueryBuilder() {
        QueryBuilder queryBuilder;
        DocumentModelList users;

        // add other users
        DocumentModel user1 = getUser("Alfred");
        userManager.createUser(user1);
        DocumentModel user2 = getUser("Arthur");
        userManager.createUser(user2);

        // no match
        queryBuilder = new QueryBuilder().predicate(Predicates.like("username", "NoSuchUserName")).countTotal(true);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(0, users.size());

        // match 3 users (but not Guest)
        queryBuilder = new QueryBuilder().predicate(Predicates.like("username", "A%"))
                                         .order(OrderByExprs.asc("username"))
                                         .countTotal(true);
        // search all
        users = userManager.searchUsers(queryBuilder);
        assertEquals(List.of("Administrator", "Alfred", "Arthur"), users.stream().map(DocumentModel::getId).toList());
        assertEquals(3, users.totalSize());
        // same without counting total
        queryBuilder.countTotal(false);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(3, users.size());
        assertEquals(3, users.totalSize()); // total is available anyway

        // with limit/offset
        queryBuilder.limit(1).offset(1).countTotal(true);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(List.of("Alfred"), users.stream().map(DocumentModel::getId).toList());
        assertEquals(3, users.totalSize());
        // same without counting total
        queryBuilder.countTotal(false);
        users = userManager.searchUsers(queryBuilder);
        assertEquals(List.of("Alfred"), users.stream().map(DocumentModel::getId).toList());
        assertEquals(-2, users.totalSize());
    }

    @Test
    public void testSearchGroupsWithQueryBuilder() {
        QueryBuilder queryBuilder;
        DocumentModelList groups;

        // no match
        queryBuilder = new QueryBuilder().predicate(Predicates.like("groupname", "NoSuchGroupName")).countTotal(true);
        groups = userManager.searchGroups(queryBuilder);
        assertEquals(0, groups.size());

        // add another group
        DocumentModel g1 = getGroup("g1");
        userManager.createGroup(g1);

        // match all
        queryBuilder = new QueryBuilder().order(OrderByExprs.asc("groupname")).countTotal(true);
        groups = userManager.searchGroups(queryBuilder);
        assertEquals(4, groups.size());
        assertEquals(4, groups.totalSize());
        assertEquals(List.of("administrators", "g1", "members", "powerusers"),
                groups.stream().map(DocumentModel::getId).toList());

        // match 3 groups (not g1)
        queryBuilder = new QueryBuilder().predicate(Predicates.like("groupname", "%r%"))
                                         .order(OrderByExprs.asc("groupname"))
                                         .countTotal(true);
        // search all
        groups = userManager.searchGroups(queryBuilder);
        assertEquals(3, groups.size());
        assertEquals(3, groups.totalSize());
        assertEquals(List.of("administrators", "members", "powerusers"),
                groups.stream().map(DocumentModel::getId).toList());
        // same without counting total
        queryBuilder.countTotal(false);
        groups = userManager.searchGroups(queryBuilder);
        assertEquals(3, groups.size());
        assertEquals(3, groups.totalSize()); // total is available anyway

        // with limit/offset
        queryBuilder.limit(1).offset(1).countTotal(true);
        groups = userManager.searchGroups(queryBuilder);
        assertEquals(1, groups.size());
        assertEquals(3, groups.totalSize());
        assertEquals(List.of("members"), groups.stream().map(DocumentModel::getId).toList());
        // same without counting total
        queryBuilder.countTotal(false);
        groups = userManager.searchGroups(queryBuilder);
        assertEquals(1, groups.size());
        assertEquals(-2, groups.totalSize());
    }

    @Test
    public void testUpdatePrincipal() {
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

        u1Model.setProperty("user", "groups", List.of("test_g1", "test_g2"));
        userManager.updateUser(u1Model);

        // refresh u1
        u1 = userManager.getPrincipal("test_u1");
        List<String> groups = u1.getGroups();
        Collections.sort(groups);
        assertEquals(List.of("defgr", "test_g1", "test_g2"), groups);

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
    public void testUpdateGroupLabel() {
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
    public void testUpdateGroup() {
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

        DocumentModel g = getGroup("test_g");
        g.setProperty("group", "members", List.of("test_u1", "test_u2"));
        g.setProperty("group", "subGroups", List.of("test_g1", "test_g2"));
        g = userManager.createGroup(g);

        // update group g
        g.setProperty("group", "members", List.of("test_u1", "test_u3"));
        g.setProperty("group", "subGroups", List.of("test_g1", "test_g3"));
        userManager.updateGroup(g);

        // check new group
        NuxeoGroup newG = userManager.getGroup("test_g");
        List<String> actualUsers = newG.getMemberUsers();
        Collections.sort(actualUsers);
        assertEquals(List.of("test_u1", "test_u3"), actualUsers);
        List<String> actualGroups = newG.getMemberGroups();
        Collections.sort(actualGroups);
        assertEquals(List.of("test_g1", "test_g3"), actualGroups);
    }

    @Test
    public void testGetPrincipalWithoutReferences() {
        DocumentModel u1 = getUser("user1");
        userManager.createUser(u1);
        DocumentModel g1 = getGroup("group1");
        g1.setProperty("group", "members", List.of("user1"));
        userManager.createGroup(g1);
        DocumentModel g2 = getGroup("group2");
        g2.setProperty("group", "members", List.of("user1"));
        userManager.createGroup(g2);

        NuxeoPrincipal principal1 = userManager.getPrincipal("user1");
        assertEquals(3, principal1.getAllGroups().size());

        // Now fetch without references
        principal1 = userManager.getPrincipal("user1", false);
        assertEquals(1, principal1.getAllGroups().size()); // 1 virtual group = defgr
        assertTrue(principal1.isMemberOf("defgr"));
        assertFalse(principal1.isMemberOf("group1"));
    }

    @Test
    public void testPasswordAuthenticate() {
        assertTrue(userManager.checkUsernamePassword("Administrator", "Administrator"));
    }

    @Test
    public void testPasswordChange() {
        DocumentModel doc = userManager.getUserModel("Administrator");
        doc.setProperty("user", "password", "newPassword123");
        userManager.updateUser(doc);
        // old one not valid anymore
        assertFalse(userManager.checkUsernamePassword("Administrator", "Administrator"));
        // new one can be used to authenticate
        assertTrue(userManager.checkUsernamePassword("Administrator", "newPassword123"));
    }

    @Test
    public void testPasswordNotReturned() {
        // getPrincipal
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        DocumentModel doc = principal.getModel();
        String password = (String) doc.getProperty("user", "password");
        assertNull(password);

        // getUserModel
        doc = userManager.getUserModel("Administrator");
        password = (String) doc.getProperty("user", "password");
        assertNull(password);

        // searchUsers
        List<DocumentModel> docs = userManager.searchUsers("Administrator");
        assertEquals(1, docs.size());
        doc = docs.getFirst();
        password = (String) doc.getProperty("user", "password");
        assertNull(password);
    }

    /**
     * common init method for initialising tests for the method getUsernamesForPermission.
     */
    private void initTestGetUsernamesForPermission() {
        userManager.getPrincipal("Administrator"); // creates tables
        deleteTestObjects();
        userManager.createUser(getUser("alex"));
        userManager.createUser(getUser("bree"));
        userManager.createUser(getUser("jdoe"));
        userManager.createUser(getUser("stef"));

        // group1 has alex and stef as members
        DocumentModel g1 = getGroup("group1");
        g1.setProperty("group", "members", List.of("alex", "stef"));
        userManager.createGroup(g1);

        // group2 has alex and bree as members
        DocumentModel g2 = getGroup("group2");
        g2.setProperty("group", "members", List.of("alex", "bree"));
        userManager.createGroup(g2);

        // group3 has jdoe as members and a subgroup: g2
        DocumentModel g3 = getGroup("group3");
        g3.setProperty("group", "members", List.of("jdoe"));
        g3.setProperty("group", "subGroups", List.of("group2"));
        userManager.createGroup(g3);
    }

    // NXP-33500
    @Test
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-permissions.xml")
    public void testGetLeafPermissionsWithCircularDependency() {
        if (userManager instanceof UserManagerImpl userManagerImpl) {
            assertEquals(List.of("Permission3", "Permission4", "Permission5"),
                    userManagerImpl.getLeafPermissions("Permission2"));
        } else {
            fail("Expected UserManagerImpl implementation.");
        }
    }

    /**
     * Testing the method getUsernamesForPermission for a simple case.
     */
    @Test
    public void testGetUsernamesForPermission() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acl.add(new ACE("alex", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = new ArrayList<>(List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp)));
        Collections.sort(users);
        assertEquals("Expected users having read access are ", List.of("Administrator", "alex", "bree", "jdoe"), users);
    }

    /**
     * Testing the method getUsernamesForPermission for a simple case.
     */
    @Test
    public void testGetUsernamesForPermission2() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(ACE.BLOCK);
        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acl.add(new ACE("alex", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp));

        assertEquals("Expected users having read access are ", List.of("alex"), users);
    }

    /**
     * Same test as before but without the first ace (default value: everyone, everything false).
     */
    @Test
    public void testGetUsernamesForPermissionWithoutEveryoneEverythingACE() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();

        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acl.add(new ACE("alex", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp));

        assertEquals("Expected users having read access are ", List.of("alex"), users);
    }

    /**
     * Testing getUsernamesForPermission with a user in 2 groups.
     */
    @Test
    public void testGetUsernamesForPermissionIn2Groups() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group2", SecurityConstants.READ, false));
        acl.add(new ACE("group1", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = new ArrayList<>(List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp)));
        Collections.sort(users);
        // Should contain alex and stef (in group1) and jdoe (in none of these groups) but not bree (in group2)
        assertEquals("Expected users having read access are ", List.of("Administrator", "alex", "jdoe", "stef"), users);
    }

    /**
     * Testing getUsernamesForPermission with compound permission. For example, READ_WRITE contains READ.
     */
    @Test
    public void testGetUsernamesForPermissionWithCompoundPermission() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group2", SecurityConstants.READ_WRITE, false));
        acl.add(new ACE("group1", SecurityConstants.READ, true));
        acp.addACL(acl);

        List<String> users = new ArrayList<>(List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp)));
        Collections.sort(users);
        // Should contain alex and stef (in group1) and jdoe (in none of these groups) but not bree (in group2)
        assertEquals("Expected users having read access are ", List.of("Administrator", "alex", "jdoe", "stef"), users);
    }

    /**
     * Testing getUsernamesForPermission with a ACP having more than one ACL
     */
    @Test
    public void testGetUsernamesForPermissionWithMultipleACL() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(ACL.INHERITED_ACL);
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE("group2", SecurityConstants.READ_WRITE, false));
        acp.addACL(acl);

        ACLImpl acl2 = new ACLImpl(ACL.LOCAL_ACL);
        acl2.add(new ACE("group1", SecurityConstants.READ, true));
        acp.addACL(acl2);

        List<String> users = new ArrayList<>(List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp)));
        Collections.sort(users);
        // Should contain stef (in group1) and jdoe (in none of these
        // groups) but not bree (in group2) neither alex (in group1 and group2)
        assertEquals("Expected users having read access are ", List.of("Administrator", "jdoe", "stef"), users);
    }

    /**
     * Testing getUsernamesForPermission with subgroups.
     */
    @Test
    public void testGetUsernamesForPermissionWithSubGroups() {
        initTestGetUsernamesForPermission();

        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE("group3", SecurityConstants.READ_WRITE, true));
        acl.add(new ACE("group1", SecurityConstants.READ, false));
        acp.addACL(acl);

        List<String> users = new ArrayList<>(List.of(userManager.getUsersForPermission(SecurityConstants.READ, acp)));
        Collections.sort(users);
        // group3 and group2 but alex should have read access
        assertEquals("Expected users having read access are ", List.of("bree", "jdoe"), users);
    }

    @Test
    public void testUsersAndGroupsWithSpaces() {

        String userNameWithSpaces = " test_u1 ";
        String groupNameWithSpaces = " test_g1 ";

        deleteTestObjects();
        DocumentModel u1 = getUser(userNameWithSpaces);
        u1 = userManager.createUser(u1);

        assertEquals(1, userManager.searchUsers(userNameWithSpaces).size());
        assertNotNull(userManager.getUserModel(userNameWithSpaces));

        assertEquals(1, userManager.searchUsers(userNameWithSpaces.trim()).size());
        assertNotNull(userManager.getUserModel(userNameWithSpaces.trim()));

        DocumentModel g1 = getGroup(groupNameWithSpaces);

        g1.setProperty("group", "members", List.of(u1.getId()));
        g1 = userManager.createGroup(g1);

        assertEquals(1, userManager.searchGroups(groupNameWithSpaces).size());
        assertNotNull(userManager.getGroup(groupNameWithSpaces));

        assertEquals(1, userManager.searchGroups(groupNameWithSpaces.trim()).size());
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
        assertTrue(transientUsername.endsWith("leela@nuxeo.com"));
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

        String otherTransientUsername = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        assertEquals(transientUsername, otherTransientUsername);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-unique-transient-contrib.xml")
    public void testUniqueTransientUsers() {
        String transientUsername1 = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        String transientUsername2 = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        assertNotEquals(transientUsername1, transientUsername2);

        Stream.of(transientUsername1, transientUsername2).forEach(tu -> {
            assertTrue(NuxeoPrincipal.isTransientUsername(tu));
            assertFalse(tu.endsWith("leela@nuxeo.com"));
            NuxeoPrincipal principal = userManager.getPrincipal(tu);
            assertNotNull(principal);
            assertTrue(principal.isTransient());
            assertFalse(principal.isAdministrator());
            assertFalse(principal.isAnonymous());
            assertTrue(principal.getAllGroups().isEmpty());
            assertEquals("leela@nuxeo.com", principal.getFirstName());
            assertEquals("leela@nuxeo.com", principal.getEmail());
            assertEquals(tu, principal.getName());
        });
    }

    @Test
    public void testTransientUsersCreation() {
        String username = NuxeoPrincipal.computeTransientUsername("user1@hyland.com");
        DocumentModel transientUser = getUser(username);
        transientUser.setProperty("user", "firstName", "User");
        transientUser.setProperty("user", "lastName", "One");
        transientUser.setProperty("user", "company", "Hyland");
        //
        transientUser.setProperty("user", "groups", List.of("members", "ops"));
        userManager.createUser(transientUser);

        NuxeoPrincipal transientPrincipal = userManager.getPrincipal(username);
        assertNotNull(transientPrincipal);
        assertTrue(transientPrincipal.isTransient());
        assertFalse(transientPrincipal.isAdministrator());
        assertFalse(transientPrincipal.isAnonymous());
        assertEquals(username, transientPrincipal.getName());
        assertEquals("User", transientPrincipal.getFirstName());
        assertEquals("One", transientPrincipal.getLastName());
        assertEquals("Hyland", transientPrincipal.getCompany());
        assertEquals(List.of("members", "ops"), transientPrincipal.getGroups()); // no default group on transient user
        // check that ops is set as a virtual group
        DocumentModel transientModel = transientPrincipal.getModel();
        assertEquals(List.of("members"), transientModel.getProperty("user", "groups"));

        assertThrows(UserAlreadyExistsException.class, () -> userManager.createUser(transientUser));

        // check underlying storage
        var transientUserMap = UserManagerImpl.getTransientDataStore().getAll(username);
        assertNotNull(transientUserMap);
        assertEquals(username, transientUserMap.get("username"));
        assertEquals("User", transientUserMap.get("firstName"));
        assertEquals("One", transientUserMap.get("lastName"));
        assertEquals("Hyland", transientUserMap.get("company"));
        assertEquals(List.of("members", "ops"), transientUserMap.get("groups"));
    }

    @Test
    public void testTransientUsersUpdate() {
        String username = NuxeoPrincipal.computeTransientUsername("user1@hyland.com");
        NuxeoPrincipal transientPrincipal = new NuxeoPrincipalImpl(username);
        assertTrue(transientPrincipal.isTransient());
        assertFalse(transientPrincipal.isAdministrator());
        assertFalse(transientPrincipal.isAnonymous());
        DocumentModel transientUser = userManager.createUser(transientPrincipal.getModel());

        transientUser.setProperty("user", "firstName", "User");
        transientUser.setProperty("user", "lastName", "One");
        transientUser.setProperty("user", "company", "Hyland");
        userManager.updateUser(transientUser);

        // refresh transientPrincipal
        transientPrincipal = userManager.getPrincipal(username);
        assertNotNull(transientPrincipal);
        assertTrue(transientPrincipal.isTransient());
        assertFalse(transientPrincipal.isAdministrator());
        assertFalse(transientPrincipal.isAnonymous());
        assertEquals(username, transientPrincipal.getName());
        assertEquals("User", transientPrincipal.getFirstName());
        assertEquals("One", transientPrincipal.getLastName());
        assertEquals("Hyland", transientPrincipal.getCompany());

        // check underlying storage
        var transientUserMap = UserManagerImpl.getTransientDataStore().getAll(username);
        assertNotNull(transientUserMap);
        assertEquals(username, transientUserMap.get("username"));
        assertEquals("User", transientUserMap.get("firstName"));
        assertEquals("One", transientUserMap.get("lastName"));
        assertEquals("Hyland", transientUserMap.get("company"));
    }

    @Test
    public void testTransientUsersDeletion() {
        String username = NuxeoPrincipal.computeTransientUsername("user1@hyland.com");
        DocumentModel transientUser = getUser(username);
        transientUser.setProperty("user", "firstName", "User");
        userManager.createUser(transientUser);

        NuxeoPrincipal transientPrincipal = userManager.getPrincipal(username);
        assertNotNull(transientPrincipal);
        assertEquals("User", transientPrincipal.getFirstName());

        userManager.deleteUser(transientUser);
        // deleting a transient user is deleting its data from storage, but we still get a principal
        transientPrincipal = userManager.getPrincipal(username);
        assertNotNull(transientPrincipal);
        assertEquals("user1@hyland.com", transientPrincipal.getFirstName()); // see testTransientUsers

        // check underlying storage
        var transientUserMap = UserManagerImpl.getTransientDataStore().getAll(username);
        assertNull(transientUserMap);

        // try to delete the principal twice
        var e = assertThrows(DirectoryException.class, () -> userManager.deleteUser(transientUser));
        assertTrue(e.getMessage(), e.getMessage().contains("User does not exist: " + username));
    }

    @Test
    public void testTransientUsersNotSearchable() {
        String username = NuxeoPrincipal.computeTransientUsername("user1@hyland.com");
        DocumentModel transientUser = getUser(username);
        userManager.createUser(transientUser);

        assertTrue("Transient users should not be searchable", userManager.searchUsers(username).isEmpty());
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

    /**
     * Checks the ancestor groups of the ABCD group with the following hierarchy:
     *
     * <pre>
     * A  B  C  D
     *  \/    \/
     *  AB    CD
     *   \    /
     *    \  /
     *    ABCD
     * </pre>
     */
    @Test
    public void testAncestorGroups() {
        DocumentModel groupABCD = getGroup("ABCD");
        userManager.createGroup(groupABCD);

        DocumentModel groupAB = getGroup("AB");
        groupAB.setPropertyValue("group:subGroups", (Serializable) List.of("ABCD"));
        userManager.createGroup(groupAB);

        DocumentModel groupCD = getGroup("CD");
        groupCD.setPropertyValue("group:subGroups", (Serializable) List.of("ABCD"));
        userManager.createGroup(groupCD);

        DocumentModel groupA = getGroup("A");
        groupA.setPropertyValue("group:subGroups", (Serializable) List.of("AB"));
        userManager.createGroup(groupA);

        DocumentModel groupB = getGroup("B");
        groupB.setPropertyValue("group:subGroups", (Serializable) List.of("AB"));
        userManager.createGroup(groupB);

        DocumentModel groupC = getGroup("C");
        groupC.setPropertyValue("group:subGroups", (Serializable) List.of("CD"));
        userManager.createGroup(groupC);

        DocumentModel groupD = getGroup("D");
        groupD.setPropertyValue("group:subGroups", (Serializable) List.of("CD"));
        userManager.createGroup(groupD);

        List<String> ancestorGroups = userManager.getAncestorGroups("ABCD");
        Collections.sort(ancestorGroups);
        assertEquals(List.of("A", "AB", "B", "C", "CD", "D"), ancestorGroups);
    }

    @Test
    public void testNuxeoPrincipalSerialization() {
        String userId = "test";
        DocumentModel doc = getUser(userId);
        userManager.createUser(doc);
        NuxeoPrincipal principal = userManager.getPrincipal(userId);
        // clear cache to force directory lookup on deserialization
        userManager.notifyUserChanged(userId, UserManagerImpl.USERMODIFIED_EVENT_ID);

        // remove any user from the login stack
        Deque<Principal> savedStack = LoginComponent.getPrincipalStack();
        LoginComponent.clearPrincipalStack();
        try {
            // check that no user is logged in
            assertNull(NuxeoPrincipal.getCurrent());
            byte[] buffer = SerializationUtils.serialize(principal);
            Object object = SerializationUtils.deserialize(buffer);
            assertTrue(object instanceof NuxeoPrincipalImpl); // actually a TransferableClone but that's a detail
            principal = (NuxeoPrincipal) object;
            assertEquals(userId, principal.getName());
        } finally {
            // restore login stack
            savedStack.forEach(LoginComponent::pushPrincipal);
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-search-escape-compat-true.xml")
    public void testGetQueryForPatternCompatTrue() {
        doTestGetQueryForPattern(true);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-search-escape-compat-false.xml")
    public void testGetQueryForPatternCompatFalse() {
        doTestGetQueryForPattern(false);
    }

    @Test
    public void testGetQueryForPatternDefault() {
        doTestGetQueryForPattern(false);
    }

    protected void doTestGetQueryForPattern(boolean compat) {
        String query;

        // basic query, directory configured for "subinitial" substring match type
        query = getQueryForPattern("foo");
        assertEquals("((firstName ILIKE 'foo%') OR (lastName ILIKE 'foo%') OR (username ILIKE 'foo%'))", query);

        // query containing wildcards that should be escaped (except if compat)
        query = getQueryForPattern("a_b%c");
        if (!compat) {
            // note: this is a debug string, escapes are not really equivalent to the generated NXQL
            assertEquals(
                    "((firstName ILIKE 'a\\_b\\%c%') OR (lastName ILIKE 'a\\_b\\%c%') OR (username ILIKE 'a\\_b\\%c%'))",
                    query);
        } else {
            // compat: we don't escape special characters
            assertEquals("((firstName ILIKE 'a_b%c%') OR (lastName ILIKE 'a_b%c%') OR (username ILIKE 'a_b%c%'))",
                    query);
        }
    }

    protected String getQueryForPattern(String pattern) {
        UserManagerImpl um = (UserManagerImpl) userManager;
        QueryBuilder queryBuilder = um.getQueryForPattern(pattern, um.getUserDirectoryName(), um.userSearchFields,
                um.getUserOrderBy());
        return queryBuilder.predicate().toString();
    }

    /**
     * Tests that concurrent getPrincipal calls return independent copies under
     * {@link UserManagerImpl#getPrincipalUsingCache}. Does not assert single-flight loading.
     *
     * @since 2025.20
     */
    @Test
    public void testConcurrentGetPrincipal() throws Exception {
        int threadCount = 10;
        // Ensure cache is empty
        userManager.getPrincipal("Administrator").getAllGroups(); // warm up
        ((UserManagerImpl) userManager).principalCache.invalidateAll();

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<NuxeoPrincipal>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return Framework.doPrivileged(() -> userManager.getPrincipal("Administrator"));
                }));
            }

            // Release all threads at once
            startLatch.countDown();

            List<NuxeoPrincipal> results = new ArrayList<>();
            for (Future<NuxeoPrincipal> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            assertEquals(threadCount, results.size());
            // All results should be non-null and independent copies
            for (NuxeoPrincipal principal : results) {
                assertNotNull(principal);
                assertEquals("Administrator", principal.getName());
            }
            // Verify they are different object instances (deep copies)
            for (int i = 0; i < results.size() - 1; i++) {
                for (int j = i + 1; j < results.size(); j++) {
                    assertNotSame(results.get(i), results.get(j));
                }
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

}
