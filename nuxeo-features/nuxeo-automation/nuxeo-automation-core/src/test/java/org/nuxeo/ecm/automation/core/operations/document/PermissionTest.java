/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 *
 *
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({RuntimeFeature.class, MockitoFeature.class})
public class PermissionTest {

    @Mock
    @RuntimeService
    UserManager um;

    @Before
    public void doBefore() throws Exception {
        when(um.getAdministratorsGroups()).thenReturn(
                Arrays.asList(new String[] { "administrators" }));
    }

    @Test
    public void itCanAddExistingPermission() throws Exception {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();
        // When i set Permission to a user
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "john",
                READ_WRITE, false, "john");

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));

        assertEquals(1, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
        assertEquals(new ACE("john", READ_WRITE, true),
                acp.getACL(ACL.LOCAL_ACL).getACEs()[0]);

    }

    @Test
    public void itCanAddPermission() throws Exception {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // Make a first call to fill cache
        assertEquals(Access.UNKNOWN, acp.getAccess("john", "comment"));

        // When i set Permission to a user
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "john",
                "comment", false, "john");

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", "comment"));

        assertEquals(1, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
        assertEquals(new ACE("john", "comment", true),
                acp.getACL(ACL.LOCAL_ACL).getACEs()[0]);

    }

    @Test
    public void itShouldNotAddPermisionTwice() throws Exception {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();
        // When i set Permission to a user
        boolean hasChanged = DocumentPermissionHelper.addPermission(acp,
                ACL.LOCAL_ACL, "john", READ_WRITE, false, null);
        assertTrue(hasChanged);
        // When i call the operation another time
        hasChanged = DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL,
                "john", READ_WRITE, false, null);
        // Then nothing should change
        assertFalse(hasChanged);
        assertEquals(1, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
    }

    @Test
    public void itCanBlockInheritance() throws Exception {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // When i add the permission to a user with blocking inheritance
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "john",
                READ_WRITE, true, "john");

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));

        // Other user don't have access anymor
        assertEquals(Access.DENY, acp.getAccess("jack", READ_WRITE));

        // Administrators still have access
        assertEquals(Access.GRANT, acp.getAccess("administrators", READ_WRITE));

    }

    @Test(expected = IllegalArgumentException.class)
    public void blockingInheritanceNeedsACurrentPrincipal() throws Exception {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // When i add the permission to a user with blocking inheritance
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "john",
                READ_WRITE, true, null);

    }

    @Test
    public void itShoulAddInheritanceEvenIfItAlreadyHasPermission()
            throws Exception {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE("john", READ_WRITE, true));

        // When i add the permission to a user with blocking inheritance
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "john",
                READ_WRITE, true, "john");

        // Then he still have access and local ACL have an entry
        assertEquals(Access.GRANT, acp.getAccess("john", READ_WRITE));

    }

    @Test
    public void itCanRemovePermissionsToAUser() throws Exception {
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
        boolean hasChanged = DocumentPermissionHelper.removePermission(acp,
                ACL.LOCAL_ACL, jack);

        // Then the user doesn't have any permission anymore
        assertTrue(hasChanged);
        assertEquals(Access.UNKNOWN, acp.getAccess("jack", READ_WRITE));
        assertEquals(Access.UNKNOWN, acp.getAccess("jack", "comment"));
        assertEquals(2, acp.getACL(ACL.LOCAL_ACL).getACEs().length);
    }

    @Test
    public void itDoesNotChangeSecurityWhenRemovingNonExistentUser()
            throws Exception {
        // Given an ACP
        ACP acp = new ACPImpl();

        assertEquals(Access.UNKNOWN, acp.getAccess("jack", READ_WRITE));

        // When i call the removePermission operation
        boolean hasChanged = DocumentPermissionHelper.removePermission(acp,
                ACL.LOCAL_ACL, "jack");

        // Then the user doesn't have any permission anymore
        assertFalse(hasChanged);
        assertEquals(Access.UNKNOWN, acp.getAccess("jack", READ_WRITE));

    }

    @Test
    public void testMultipleNewPermissionsWithBlockInheritance() {
        // Given an ACP
        ACP acp = getInheritedReadWriteACP();

        // Make a first call to fill cache
        assertEquals(Access.UNKNOWN, acp.getAccess("john", "comment"));

        // When i set Permission to a user with inheritance block
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "john",
                READ_WRITE, true, "john");

        // only john have Permission
        assertEquals(Access.GRANT, acp.getAccess("john", "ReadWrite"));
        assertEquals(Access.DENY, acp.getAccess("jack", "ReadWrite"));
        assertEquals(Access.DENY, acp.getAccess("jerry", "ReadWrite"));

        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "jack",
                READ_WRITE, false, "john");
        DocumentPermissionHelper.addPermission(acp, ACL.LOCAL_ACL, "jerry",
                READ_WRITE, false, "john");

        // Check jack and jerry have permission, even with inheritance block
        assertEquals(Access.GRANT, acp.getAccess("john", "ReadWrite"));
        assertEquals(Access.GRANT, acp.getAccess("jack", "ReadWrite"));
        assertEquals(Access.GRANT, acp.getAccess("jerry", "ReadWrite"));

    }

    /**
     * @return
     *
     */
    private ACP getInheritedReadWriteACP() {
        ACP acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl = acp.getOrCreateACL(ACL.INHERITED_ACL);

        acl.add(new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.READ_WRITE, true));
        return acp;

    }

}
