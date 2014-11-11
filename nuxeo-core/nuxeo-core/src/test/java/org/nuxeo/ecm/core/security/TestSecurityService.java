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
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/permissions-contrib.xml");
        service = NXCore.getSecurityService();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    // TODO: Make this test independent of the permissions-contrib.xml file.
    @Test
    public void testGetPermissionsToCheck() {
        List<String> perms = Arrays.asList(service.getPermissionsToCheck(SecurityConstants.READ));
        assertEquals(4, perms.size());
        assertTrue(perms.contains(SecurityConstants.READ));
        assertTrue(perms.contains(SecurityConstants.EVERYTHING));
    }

    @Test
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

    @Test
    public void testDefaultVisiblePermission() throws Exception {
        PermissionProvider pp = service.getPermissionProvider();

        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        assertEquals(
                Arrays.asList("Read", "ReadWrite", "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));

        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(
                Arrays.asList("Read", "ReadWrite", "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));

        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(
                Arrays.asList("Read", "ReadWrite", "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));
    }

    @Test
    public void testOverridedPermissions1() throws Exception {
        // deploy a new atomic permission and a new compound permission
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override1-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();

        // test how previous permissions where affected by the override
        String[] groups = pp.getPermissionGroups("Read");
        assertNotNull(groups);
        assertEquals(
                Arrays.asList("CustomCompoundPerm", "ReadRemove", "ReadWrite"),
                Arrays.asList(groups));

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

    @Test
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

        assertEquals(Arrays.asList("Read", "CustomCompoundPerm", "ReadWrite",
                "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Read", "CustomCompoundPerm", "ReadWrite",
                "ReadRemove", "Everything"),
                permStrings(orderedVisiblePermissions));
    }

    @Test
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

    @Test
    public void testOverridedVisiblePermission2() throws Exception {
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override2-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Write", "Read", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));

        // custom settings for the Section type
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Write", "Read", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Write", "Read", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));
    }

    @Test
    public void testOverridedVisiblePermission3() throws Exception {
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "permissions-override3-contrib.xml");

        PermissionProvider pp = service.getPermissionProvider();
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Write", "Read", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));

        // custom settings for the Section type
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(
                Arrays.asList("Write", "ReadRemove", "Everything", "Read"),
                permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(Arrays.asList("Write", "Read", "ReadRemove",
                "Everything"), permStrings(orderedVisiblePermissions));
    }

    @Test
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

    @Test
    public void testGetPrincipalsToCheck() {
        NuxeoPrincipal principal = new UserPrincipal("bob", Arrays.asList(
                "vps", "males"), false, false);
        String[] principals = SecurityService.getPrincipalsToCheck(principal);
        assertEquals(4, principals.length);
        assertTrue(Arrays.asList(principals).contains("bob"));
        assertTrue(Arrays.asList(principals).contains("vps"));
        assertTrue(Arrays.asList(principals).contains("males"));
        assertTrue(Arrays.asList(principals).contains(
                SecurityConstants.EVERYONE));
    }

}
