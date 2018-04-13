/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.security.Access.GRANT;
import static org.nuxeo.ecm.core.api.security.Access.UNKNOWN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.RESTRICTED_READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class TestACP {

    @Mock
    @RuntimeService
    protected AdministratorGroupsProvider administratorGroupsProvider;

    private ACP acp;

    @Before
    public void doBefore() {
        when(administratorGroupsProvider.getAdministratorsGroups()).thenReturn(
                Collections.singletonList("administrators"));
    }

    @Before
    public void setUp() {
        acp = new ACPImpl();
    }

    @After
    public void tearDown() {
        acp = null;
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

        acp.addACL(acl2);

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

        Set<String> perms = new HashSet<>(3);
        perms.add(BROWSE);
        perms.add(READ);
        perms.add(WRITE);
        String[] usernames = acp.listUsernamesForAnyPermission(perms);
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

    @Test
    public void itCanAddExistingPermission() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();
        // When i set Permission to a user
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).creator("john").build());

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));

        assertEquals(1, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
        assertEquals(ACE.builder("john", READ_WRITE).creator("john").build(), acp.getACL(ACL.LOCAL_ACL).getACEs()[0]);

    }

    @Test
    public void itCanAddPermission() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // Make a first call to fill cache
        assertEquals(Access.UNKNOWN, acp.getAccess("john", "comment"));

        // When i set Permission to a user
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", "comment").creator("john").build());

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", "comment"));

        assertEquals(1, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
        assertEquals(ACE.builder("john", "comment").creator("john").build(), acp.getACL(ACL.LOCAL_ACL).getACEs()[0]);

    }

    @Test
    public void itShouldNotAddPermissionTwice() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();
        // When i set Permission to a user

        boolean hasChanged = acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).build());
        assertTrue(hasChanged);
        // When i call the operation another time
        hasChanged = acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).build());
        // Then nothing should change
        assertFalse(hasChanged);
        assertEquals(1, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
    }

    @Test
    public void itCanBlockInheritance() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // When i add the permission to a user with blocking inheritance
        acp.blockInheritance(ACL.LOCAL_ACL, "john");
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).creator("john").build());

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));

        // Other user don't have access anymore
        assertEquals(Access.DENY, acp.getAccess("jack", READ_WRITE));

        // Administrators still have access
        assertEquals(Access.GRANT, acp.getAccess("administrators", READ_WRITE));

        // unblock the inheritance
        acp.unblockInheritance(ACL.LOCAL_ACL);
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));
        assertEquals(Access.GRANT, acp.getAccess("jack", READ_WRITE));
    }

    @Test(expected = NullPointerException.class)
    public void blockingInheritanceNeedsACurrentPrincipal() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // When i add the permission to a user with blocking inheritance
        acp.blockInheritance(ACL.LOCAL_ACL, null);
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).build());

    }

    @Test
    public void itShouldAddInheritanceEvenIfItAlreadyHasPermission() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE("john", READ_WRITE, true));

        // When i add the permission to a user with blocking inheritance
        acp.blockInheritance(ACL.LOCAL_ACL, "john");
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).creator("john").build());

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));

    }

    @Test
    public void itCanRemovePermissionsToAUser() {
        // Given an ACP
        ACP acp = new ACPImpl();
        String jack = "jack";
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE("john", READ_WRITE, true));
        acl.add(new ACE(jack, READ_WRITE, true));
        acl.add(new ACE(jack, "comment", true));
        acl.add(new ACE("jerry", READ_WRITE, true));

        assertEquals(Access.GRANT, acp.getAccess("jack", READ_WRITE));
        assertEquals(4, acp.getACL(ACL.LOCAL_ACL).getACEs().length);

        // When i call the removePermission operation
        boolean hasChanged = acp.removeACEsByUsername(ACL.LOCAL_ACL, jack);

        // Then the user doesn't have any permission anymore
        assertTrue(hasChanged);
        assertEquals(Access.UNKNOWN, acp.getAccess("jack", READ_WRITE));
        assertEquals(Access.UNKNOWN, acp.getAccess("jack", "comment"));
        assertEquals(2, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
    }

    @Test
    public void itDoesNotChangeSecurityWhenRemovingNonExistentUser() {
        // Given an ACP
        ACP acp = new ACPImpl();

        assertEquals(Access.UNKNOWN, acp.getAccess("jack", READ_WRITE));

        // When i call the removePermission operation
        boolean hasChanged = acp.removeACEsByUsername(ACL.LOCAL_ACL, "jack");

        // Then the user doesn't have any permission anymore
        assertFalse(hasChanged);
        assertEquals(Access.UNKNOWN, acp.getAccess("jack", READ_WRITE));

    }

    @Test
    public void itCanRemovePermissionGivenItsId() {
        // Given an ACP
        ACP acp = new ACPImpl();
        String jack = "jack";
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE("john", READ_WRITE, true));
        acl.add(new ACE(jack, READ_WRITE, true));
        acl.add(new ACE(jack, "comment", true));
        ACE ace = new ACE("jerry", READ_WRITE, true);
        acl.add(ace);

        assertEquals(Access.GRANT, acp.getAccess("jack", READ_WRITE));
        assertEquals(Access.GRANT, acp.getAccess("jerry", READ_WRITE));
        assertEquals(4, acp.getACL(ACL.LOCAL_ACL).getACEs().length);

        // When i call the removePermission operation
        boolean hasChanged = acp.removeACE(ACL.LOCAL_ACL, ace);

        // Then the user doesn't have any permission anymore
        assertTrue(hasChanged);
        assertEquals(Access.GRANT, acp.getAccess("jack", READ_WRITE));
        assertEquals(Access.GRANT, acp.getAccess("jack", "comment"));
        assertEquals(Access.UNKNOWN, acp.getAccess("jerry", READ_WRITE));
        assertEquals(3, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
    }

    @Test
    public void testMultipleNewPermissionsWithBlockInheritance() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // Make a first call to fill cache
        assertEquals(Access.UNKNOWN, acp.getAccess("john", "comment"));

        // When i set Permission to a user with inheritance block
        acp.blockInheritance(ACL.LOCAL_ACL, "john");
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("john", READ_WRITE).creator("john").build());

        // only john have Permission
        assertEquals(Access.GRANT, acp.getAccess("john", "ReadWrite"));
        assertEquals(Access.DENY, acp.getAccess("jack", "ReadWrite"));
        assertEquals(Access.DENY, acp.getAccess("jerry", "ReadWrite"));

        acp.addACE(ACL.LOCAL_ACL, ACE.builder("jack", READ_WRITE).creator("john").build());
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("jerry", READ_WRITE).creator("john").build());

        // Check jack and jerry have permission, even with inheritance block
        assertEquals(Access.GRANT, acp.getAccess("john", "ReadWrite"));
        assertEquals(Access.GRANT, acp.getAccess("jack", "ReadWrite"));
        assertEquals(Access.GRANT, acp.getAccess("jerry", "ReadWrite"));

    }

    private ACP getInheritedReadWriteACP() {
        ACP acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl = acp.getOrCreateACL(ACL.INHERITED_ACL);
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ_WRITE, true));
        return acp;
    }

}
