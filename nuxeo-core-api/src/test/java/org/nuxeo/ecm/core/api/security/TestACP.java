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

package org.nuxeo.ecm.core.api.security;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

public class TestACP extends TestCase {

    private ACP acp;

    @Override
    public void setUp() {
        acp = new ACPImpl();
    }

    @Override
    public void tearDown() {
        acp = null;
    }

    public void testGetOwners() {
        String[] owners = acp.getOwners();
        assertEquals(0, owners.length);
    }

    public void testAddOwners() {
        acp.addOwner("joe");
        String[] owners = acp.getOwners();
        assertEquals(1, owners.length);
        assertEquals("joe", owners[0]);
    }

    public void testSetOwners() {
        acp.setOwners(new String[] {});
        String[] owners = acp.getOwners();
        assertEquals(0, owners.length);
    }

    public void testGetACLs() {
        ACL[] acls = acp.getACLs();
        assertEquals(0, acls.length);
    }

    public void testAddAndRemoveACL() {
        ACL acl1 = new ACLImpl("acl1");
        ACL acl2 = new ACLImpl("acl2");

        acp.addACL(acl1);
        assertEquals(1, acp.getACLs().length);
        assertEquals(acl1, acp.getACLs()[0]);

        acp.addACL(0, acl2);

        acp.removeACL("acl1");
        acp.removeACL("acl2");
        assertEquals(0, acp.getACLs().length);

        // Check that order doesn't matter
        acp.addACL(acl1);
        acp.addACL(acl2);
        acp.removeACL("acl1");
        acp.removeACL("acl2");
        assertEquals(0, acp.getACLs().length);

        acp.addACL(acl2);
        acp.addACL(acl1);
        acp.removeACL("acl1");
        acp.removeACL("acl2");
        assertEquals(0, acp.getACLs().length);

        assertNull(acp.removeACL("acl1"));
    }

    public void testCheckAccess() {
        ACL acl1 = new ACLImpl("acl1");
        ACE ace1 = new ACE("joe", SecurityConstants.EVERYTHING, true);
        acl1.add(ace1);
        acp.addACL(acl1);

        assertEquals(Access.GRANT, acp.getAccess("joe", SecurityConstants.READ));
        assertEquals(Access.UNKNOWN, acp.getAccess("joe",
                SecurityConstants.RESTRICTED_READ));
        assertEquals(Access.UNKNOWN, acp.getAccess("jack",
                SecurityConstants.READ));
    }

    public void testPermissionsAPI() {
        ACL acl = new ACLImpl("acl1");

        ACE bart = new ACE("bart", SecurityConstants.EVERYTHING, true);
        ACE notbart = new ACE("notbart", SecurityConstants.EVERYTHING, false);
        ACE homer = new ACE("homer", SecurityConstants.BROWSE, true);
        ACE lisa = new ACE("lisa", SecurityConstants.BROWSE, true);

        acl.add(bart);
        acl.add(notbart);
        acl.add(homer);
        acl.add(lisa);
        acp.addACL(acl);

        String[] usernames = acp.listUsernamesForPermission(SecurityConstants.BROWSE);
        assertEquals(2, usernames.length);

        usernames = acp.listUsernamesForPermission(SecurityConstants.EVERYTHING);
        assertEquals(1, usernames.length);

        Set<String> perms = new HashSet<String>(3);
        perms.add(SecurityConstants.BROWSE);
        perms.add(SecurityConstants.READ);
        perms.add(SecurityConstants.WRITE);
        usernames = acp.listUsernamesForAnyPermission(perms);
        assertEquals(2, usernames.length);
    }

    public void testGetOrCreateAcl() {
        // create ACL with name ACL.LOCAL_ACL
        ACL createdAcl = acp.getOrCreateACL();
        createdAcl.add(new ACE("john", "Sing", true));
        createdAcl.add(new ACE("anne", "Joke", false));

        // check that the ACP has already been affected by the ACL editing
        assertTrue(acp.getAccess("john", "Sing").toBoolean());
        assertFalse(acp.getAccess("anne", "Joke").toBoolean());

        // check that by fetching the acl again we get the same instance
        ACL fetchedAcl = acp.getOrCreateACL();
        assertEquals(createdAcl, fetchedAcl);
        assertTrue(acp.getAccess("john", "Sing").toBoolean());
        assertFalse(acp.getAccess("anne", "Joke").toBoolean());

        // check that setting the same ACL again does not clear it
        acp.addACL(fetchedAcl);
        assertEquals(createdAcl, fetchedAcl);
        assertTrue(acp.getAccess("john", "Sing").toBoolean());
        assertFalse(acp.getAccess("anne", "Joke").toBoolean());

        // check that setting an empty ACL with the same name clear the
        // permissions
        acp.addACL(new ACLImpl(ACL.LOCAL_ACL));
        assertFalse(acp.getAccess("john", "Sing").toBoolean());
    }

}
