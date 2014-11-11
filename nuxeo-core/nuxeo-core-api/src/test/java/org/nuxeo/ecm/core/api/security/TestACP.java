/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

import static org.nuxeo.ecm.core.api.security.Access.GRANT;
import static org.nuxeo.ecm.core.api.security.Access.UNKNOWN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.*;

public class TestACP {

    private ACP acp;

    @Before
    public void setUp() {
        acp = new ACPImpl();
    }

    @After
    public void tearDown() {
        acp = null;
    }

    @Test
    public void testGetOwners() {
        String[] owners = acp.getOwners();
        assertEquals(0, owners.length);
    }

    @Test
    public void testAddOwners() {
        acp.addOwner("joe");
        String[] owners = acp.getOwners();
        assertEquals(1, owners.length);
        assertEquals("joe", owners[0]);
    }

    @Test
    public void testSetOwners() {
        acp.setOwners(new String[] {});
        String[] owners = acp.getOwners();
        assertEquals(0, owners.length);
    }

    @Test
    public void testGetACLs() {
        ACL[] acls = acp.getACLs();
        assertEquals(0, acls.length);
    }

    @Test
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

    @Test
    public void testCheckAccess() {
        ACL acl1 = new ACLImpl("acl1");
        ACE ace1 = new ACE("joe", EVERYTHING, true);
        acl1.add(ace1);
        acp.addACL(acl1);

        assertSame(GRANT, acp.getAccess("joe", READ));
        assertSame(UNKNOWN, acp.getAccess("joe", RESTRICTED_READ));
        assertSame(UNKNOWN, acp.getAccess("jack", READ));
    }

    @Test
    public void testCheckAccessNullACE() {
        ACL acl1 = new ACLImpl("acl1");
        acl1.add(new ACE());
        acl1.add(new ACE(null, EVERYTHING, true));
        acl1.add(new ACE(EVERYONE, null, true));
        acp.addACL(acl1);

        assertSame(UNKNOWN, acp.getAccess("joe", READ));
        assertSame(UNKNOWN, acp.getAccess("joe", RESTRICTED_READ));
        assertSame(UNKNOWN, acp.getAccess("jack", READ));
    }
    
    @Test
    public void testPermissionsAPI() {
        ACL acl = new ACLImpl("acl1");

        ACE bart = new ACE("bart", EVERYTHING, true);
        ACE notbart = new ACE("notbart", EVERYTHING, false);
        ACE homer = new ACE("homer", BROWSE, true);
        ACE lisa = new ACE("lisa", BROWSE, true);

        acl.add(bart);
        acl.add(notbart);
        acl.add(homer);
        acl.add(lisa);
        acp.addACL(acl);

        String[] usernames = acp.listUsernamesForPermission(BROWSE);
        assertEquals(2, usernames.length);

        usernames = acp.listUsernamesForPermission(EVERYTHING);
        assertEquals(1, usernames.length);

        Set<String> perms = new HashSet<String>(3);
        perms.add(BROWSE);
        perms.add(READ);
        perms.add(WRITE);
        usernames = acp.listUsernamesForAnyPermission(perms);
        assertEquals(2, usernames.length);
    }

    @Test
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
