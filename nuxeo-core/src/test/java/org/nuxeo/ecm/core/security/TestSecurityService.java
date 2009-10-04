/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.CoreUTConstants;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class TestSecurityService extends NXRuntimeTestCase {

    private SecurityService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/permissions-contrib.xml");
        service = NXCore.getSecurityService();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    // TODO: Make this test independent of the permissions-contrib.xml file.
    public void testGetPermissionsToCheck() {
        List<String> perms = Arrays.asList(service.getPermissionsToCheck(SecurityConstants.READ));
        assertEquals(4, perms.size());
        assertTrue(perms.contains(SecurityConstants.READ));
        assertTrue(perms.contains(SecurityConstants.EVERYTHING));
    }

    public void testDefaultPermissions() {
        PermissionProvider pp = service.getPermissionProvider();

        String[] groups = pp.getPermissionGroups("Read");
        assertNotNull(groups);
        assertEquals(Arrays.asList("ReadRemove", "ReadWrite"),
                Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadProperties");
        assertNotNull(groups);
        assertEquals(Arrays.asList("Read", "ReadRemove", "ReadWrite"),
                Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadChildren");
        assertNotNull(groups);
        assertEquals(Arrays.asList("Read", "ReadRemove", "ReadWrite"),
                Arrays.asList(groups));

        groups = pp.getPermissionGroups("Browse");
        assertNotNull(groups);
        assertEquals(Arrays.asList("Read", "ReadProperties", "ReadRemove",
                "ReadWrite"), Arrays.asList(groups));
    }

    protected List<String> permStrings(List<UserVisiblePermission> perms) {
        List<String> list = new ArrayList<String>(perms.size());
        for (UserVisiblePermission perm : perms) {
            list.add(perm.getPermission());
        }
        return list;
    }

    public void testDefaultVisiblePermission() throws Exception {
        PermissionProvider pp = service.getPermissionProvider();

        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "ReadWrite", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));

        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "ReadWrite", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));

        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "ReadWrite", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));
    }

    public void testOverridedPermissions1() throws Exception {
        // deploy a new atomic permission and a new compound permission
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override1-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();

        // test how previous permissions where affected by the override
        String[] groups = pp.getPermissionGroups("Read");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "ReadRemove",
                "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadProperties");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "Read", "ReadRemove",
                "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadChildren");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "Read", "ReadRemove",
                "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("Browse");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "Read",
                "ReadProperties", "ReadRemove", "ReadWrite"),
                Arrays.asList(groups));

        // test the new permissions
        groups = pp.getPermissionGroups("CustomCompoundPerm");
        assertNull(groups);

        groups = pp.getPermissionGroups("CustomAtomicPerm");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm"), Arrays.asList(groups));
    }

    public void testOverridedVisiblePermission1() throws Exception {
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override1-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "CustomCompoundPerm", "ReadWrite",
                "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));

        // Section is overridden
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "Everything"),
                permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "CustomCompoundPerm", "ReadWrite",
                "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));
    }

    public void testOverriddenPermissions2() throws Exception {
        // deploy a new atomic permission and a new compound permission
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override2-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();

        // check default permissions where not affected by the override
        testDefaultPermissions();

        // test the new permissions
        String[] groups = pp.getPermissionGroups("CustomCompoundPerm");
        assertNull(groups);

        groups = pp.getPermissionGroups("CustomAtomicPerm");
        assertNotNull(groups);
        assertEquals(Arrays.asList("ReadWrite", "Write"), Arrays.asList(groups));
    }

    public void testOverridedVisiblePermission2() throws Exception {
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override2-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        assertEquals(
                Arrays.asList("Write", "Read", "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));

        // custom settings for the Section type
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read"),
                permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(
                Arrays.asList("Write", "Read", "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));
    }

    public void testPermissionsVsDeny() throws Exception {
        PermissionProvider pp = service.getPermissionProvider();
        List<UserVisiblePermission> vp = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(vp);

        UserVisiblePermission deleteVP = null;
        for (UserVisiblePermission uvp : vp) {
            if (uvp.getId().equals("ReadRemove")) {
                deleteVP = uvp;
                break;
            }
        }
        assertNotNull(deleteVP);
        assertEquals("Remove", deleteVP.getDenyPermission());
        assertEquals("ReadRemove", deleteVP.getPermission());
    }

    public void testGetPrincipalsToCheck() {
        NuxeoPrincipal principal = new UserPrincipal("bob",
                Arrays.asList("vps", "males"));
        String[] principals = SecurityService.getPrincipalsToCheck(principal);
        assertEquals(4, principals.length);
        assertTrue(Arrays.asList(principals).contains("bob"));
        assertTrue(Arrays.asList(principals).contains("vps"));
        assertTrue(Arrays.asList(principals).contains("males"));
        assertTrue(Arrays.asList(principals).contains(SecurityConstants.EVERYONE));
    }

}
