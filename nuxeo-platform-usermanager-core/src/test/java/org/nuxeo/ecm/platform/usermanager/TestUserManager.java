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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author George Lefter
 * @author Florent Guillaume
 */
public class TestUserManager extends NXRuntimeTestCase {

    protected UserManager userManager;

    @Override
    protected void setUp() throws Exception {
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

    public void testExistingSetup() throws ClientException {
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        List<String> groups = principal.getGroups();

        assertTrue(groups.contains("administrators"));
    }

    public void testGetAnonymous() throws ClientException {
        NuxeoPrincipal principal = userManager.getPrincipal("Guest");
        assertNotNull(principal);
        assertEquals("Guest", principal.getName());
        assertEquals("Anonymous", principal.getFirstName());
        assertEquals("Coward", principal.getLastName());
        assertNull(principal.getCompany());
    }

    public void testSearchAnonymous() throws ClientException {
        List<NuxeoPrincipal> principals;
        NuxeoPrincipal principal;

        principals = userManager.searchPrincipals("Gu");
        assertEquals(1, principals.size());
        principal = principals.get(0);
        assertEquals("Guest", principal.getName());
        assertEquals("Anonymous", principal.getFirstName());
        assertEquals("Coward", principal.getLastName());

        // search by map
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("lastName", "Cow");
        principals = userManager.searchByMap(filter, filter.keySet());
        assertEquals(1, principals.size());
        principal = principals.get(0);
        assertEquals("Guest", principal.getName());
        // with a non-matching criterion
        filter.put("firstName", "Bob");
        principals = userManager.searchByMap(filter, filter.keySet());
        assertEquals(0, principals.size());
        // another search
        filter.clear();
        filter.put("username", "Gue");
        principals = userManager.searchByMap(filter, filter.keySet());
        assertEquals(1, principals.size());
        principal = principals.get(0);
        assertEquals("Guest", principal.getName());

        // now add another non-Anonymous user that matches the same query
        userManager.createPrincipal(new NuxeoPrincipalImpl("Gudule"));
        principals = userManager.searchPrincipals("Gu");
        assertEquals(2, principals.size());
        String name1 = principals.get(0).getName();
        String name2 = principals.get(1).getName();
        if (!name1.equals("Guest")) {
            final String tmp = name1;
            name1 = name2;
            name2 = tmp;
        }
        assertEquals("Guest", name1);
        assertEquals("Gudule", name2);
    }

    public void deleteTestObjects() throws ClientException {
        List<NuxeoPrincipal> principals = userManager.getAvailablePrincipals();
        List<NuxeoGroup> groups = userManager.getAvailableGroups();
        for (NuxeoPrincipal principal : principals) {
            if (principal.getName().startsWith("test_")) {
                userManager.deletePrincipal(principal);
            }
        }
        for (NuxeoGroup group : groups) {
            if (group.getName().startsWith("test_")) {
                userManager.deleteGroup(group);
            }
        }
    }

    // resource-intensive test, disabled by default
    public void xxxtestMemoryLeak() throws ClientException {
        deleteTestObjects();
        userManager.createPrincipal(new NuxeoPrincipalImpl("test_usr0"));
        userManager.createGroup(new NuxeoGroupImpl("test_grp0"));

        for (int i = 0; i < 100; i++) {
            String userName = "test_u" + i;
            NuxeoPrincipal principal = new NuxeoPrincipalImpl(userName);
            principal.setGroups(Arrays.asList("test_grp0"));
            principal.setRoles(Arrays.asList("regular"));
            userManager.createPrincipal(principal);
        }

        for (int i = 0; i < 100; i++) {
            String groupName = "test_g" + i;
            NuxeoGroup group = new NuxeoGroupImpl(groupName);
            userManager.createGroup(group);
        }

        for (int i = 0; i < 100; i++) {
            userManager.getAvailableGroups();
        }

        for (int i = 0; i < 100; i++) {
            userManager.getAvailablePrincipals();
        }
    }

    public void testCreatePrincipal() throws ClientException {
        deleteTestObjects();
        // force User Directory initialization first - so that the sql script
        // executes

        NuxeoPrincipal principal = new NuxeoPrincipalImpl("test_u1");
        NuxeoGroup g1 = new NuxeoGroupImpl("test_g1");

        userManager.createGroup(g1);
        g1 = userManager.getGroup("test_g1");

        assertNotNull(g1);

        List<String> groupNames = Arrays.asList("test_g1");
        List<String> groupNamesWithDefault = Arrays.asList("defgr", "test_g1");
        List<String> roleNames = Arrays.asList("regular");
        principal.setFirstName("fname1");
        principal.setLastName("lname1");
        principal.setCompany("company1");
        principal.setGroups(groupNames);
        principal.setRoles(roleNames);

        userManager.createPrincipal(principal);

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
        assertEquals(principal.getName(), newPrincipal.getName());
    }

    public void testCreateGroup() throws ClientException {
        deleteTestObjects();
        NuxeoPrincipal u1 = new NuxeoPrincipalImpl("test_u1");
        NuxeoPrincipal u2 = new NuxeoPrincipalImpl("test_u2");
        userManager.createPrincipal(u1);
        userManager.createPrincipal(u2);

        NuxeoGroup g1 = new NuxeoGroupImpl("test_g1");
        NuxeoGroup g2 = new NuxeoGroupImpl("test_g2");

        List<String> g1Users = Arrays.asList("test_u1");
        List<String> g2Users = Arrays.asList("test_u1", "test_u2");
        List<String> g2Groups = Arrays.asList("test_g1");

        g1.setMemberUsers(g1Users);
        userManager.createGroup(g1);

        g2.setMemberUsers(g2Users);
        g2.setMemberGroups(g2Groups);
        userManager.createGroup(g2);

        NuxeoGroup newG1 = userManager.getGroup("test_g1");
        NuxeoGroup newG2 = userManager.getGroup("test_g2");

        assertNotNull(newG1);
        assertNotNull(newG2);

        assertEquals("test_g1", g1.getName());
        assertEquals("test_g2", g2.getName());
        assertEquals(g1Users, g1.getMemberUsers());
        assertEquals(g2Users, g2.getMemberUsers());
        assertEquals(g2Groups, g2.getMemberGroups());

        assertEquals(g1, newG1);
        assertEquals(g2, newG2);

        // try to create the group again and test if an exception is thrown
        boolean gotException = false;
        try {
            userManager.createGroup(g1);
        } catch (ClientException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testGetTopLevelGroups() throws ClientException {
        deleteTestObjects();

        NuxeoGroup g1 = new NuxeoGroupImpl("test_g1");
        NuxeoGroup g2 = new NuxeoGroupImpl("test_g2");

        {
            List<String> g2Groups = Arrays.asList("test_g1");

            userManager.createGroup(g1);
            g2.setMemberGroups(g2Groups);
            userManager.createGroup(g2);

            List<String> expectedTopLevelGroups = Arrays.asList(
                    "administrators", "members", "test_g2");
            List<String> topLevelGroups = userManager.getTopLevelGroups();
            Collections.sort(topLevelGroups);

            assertEquals(expectedTopLevelGroups, topLevelGroups);
        }

        // delete test_g2 and test if test_g1 is toplevel
        {
            userManager.deleteGroup(g2);
            List<String> expectedTopLevelGroups = Arrays.asList(
                    "administrators", "members", "test_g1");
            List<String> topLevelGroups = userManager.getTopLevelGroups();
            Collections.sort(topLevelGroups);
            assertEquals(expectedTopLevelGroups, topLevelGroups);
        }

        // re-create g2 as a parent of g1
        // test if g1 is not top-level and g2 is
        {
            List<String> g2Groups = Arrays.asList("test_g1");
            g2.setMemberGroups(g2Groups);
            userManager.createGroup(g2);
            g2.setMemberGroups(g2Groups);
            List<String> expectedTopLevelGroups = Arrays.asList(
                    "administrators", "members", "test_g2");
            List<String> topLevelGroups = userManager.getTopLevelGroups();
            Collections.sort(topLevelGroups);
            assertEquals(expectedTopLevelGroups, topLevelGroups);
        }
    }

    public void testDeletePrincipal() throws ClientException {
        deleteTestObjects();
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("test_u1");
        userManager.createPrincipal(principal);
        userManager.deletePrincipal(principal);
        assertNull(userManager.getPrincipal("test_u1"));

        // try to delete the principal twice
        boolean gotException = false;
        try {
            userManager.deletePrincipal(principal);
        } catch (ClientException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    public void testDeleteGroup() throws ClientException {
        deleteTestObjects();
        NuxeoGroup group = new NuxeoGroupImpl("test_g1");
        userManager.createGroup(group);
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

    public void testSearchPrincipals() throws ClientException {
        deleteTestObjects();
        userManager.createPrincipal(new NuxeoPrincipalImpl("test_u1"));
        userManager.createPrincipal(new NuxeoPrincipalImpl("test_u2"));

        List<NuxeoPrincipal> principals = userManager.searchPrincipals("test_");

        assertEquals(2, principals.size());
        String name1 = principals.get(0).getName();
        String name2 = principals.get(1).getName();
        assertTrue("test_u1".equals(name1) && "test_u2".equals(name2)
                || "test_u1".equals(name2) && "test_u2".equals(name1));
    }

    public void testUpdatePrincipal() throws ClientException {
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

    public void testUpdateGroup() throws ClientException {
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
