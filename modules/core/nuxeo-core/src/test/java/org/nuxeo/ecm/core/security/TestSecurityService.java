/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Georges Racinet
 *     Olivier Grisel
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/SecurityService.xml")
@Deploy("org.nuxeo.ecm.core:OSGI-INF/permissions-contrib.xml")
public class TestSecurityService {

    @Inject
    private SecurityService service;

    @Inject
    private PermissionProvider pp;

    @Test
    public void testGetPermissionsToCheck() {
        List<String> perms = Arrays.asList(service.getPermissionsToCheck(SecurityConstants.READ));
        assertEquals(4, perms.size());
        assertTrue(perms.contains(SecurityConstants.READ));
        assertTrue(perms.contains(SecurityConstants.EVERYTHING));
    }

    @Test
    public void testDefaultPermissions() {
        String[] groups = pp.getPermissionGroups("Read");
        assertNotNull(groups);
        assertEquals(Arrays.asList("ReadRemove", "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadProperties");
        assertNotNull(groups);
        assertEquals(Arrays.asList("Read", "ReadRemove", "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadChildren");
        assertNotNull(groups);
        assertEquals(Arrays.asList("Read", "ReadRemove", "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("Browse");
        assertNotNull(groups);
        assertEquals(Arrays.asList("Read", "ReadProperties", "ReadRemove", "ReadWrite"), Arrays.asList(groups));
    }

    protected List<String> permStrings(List<UserVisiblePermission> perms) {
        return perms.stream().map(UserVisiblePermission::getPermission).collect(Collectors.toList());
    }

    @Test
    public void testDefaultVisiblePermission() {
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        List<String> expected = Arrays.asList("Read", "ReadWrite", "Everything");
        assertEquals(expected, permStrings(orderedVisiblePermissions));

        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));

        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));
    }

    @Test
    // deploy a new atomic permission and a new compound permission
    @Deploy("org.nuxeo.ecm.core.tests:permissions-override1-contrib.xml")
    public void testOverridedPermissions1() {
        // test how previous permissions where affected by the override
        String[] groups = pp.getPermissionGroups("Read");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "ReadRemove", "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadProperties");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "Read", "ReadRemove", "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("ReadChildren");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "Read", "ReadRemove", "ReadWrite"), Arrays.asList(groups));

        groups = pp.getPermissionGroups("Browse");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm", "Read", "ReadProperties", "ReadRemove", "ReadWrite"),
                Arrays.asList(groups));

        // test the new permissions
        groups = pp.getPermissionGroups("CustomCompoundPerm");
        assertEquals(0, groups.length);

        groups = pp.getPermissionGroups("CustomAtomicPerm");
        assertNotNull(groups);
        assertEquals(Arrays.asList("CustomCompoundPerm"), Arrays.asList(groups));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.tests:permissions-override1-contrib.xml")
    public void testOverridedVisiblePermission1() {
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        List<String> expected = Arrays.asList("Read", "CustomCompoundPerm", "ReadWrite", "Everything");

        assertEquals(expected, permStrings(orderedVisiblePermissions));

        // Section is overridden
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));
    }

    @Test
    // deploy a new atomic permission and a new compound permission
    @Deploy("org.nuxeo.ecm.core.tests:permissions-override2-contrib.xml")
    public void testOverriddenPermissions2() {
        // check default permissions where not affected by the override
        testDefaultPermissions();

        // test the new permissions
        String[] groups = pp.getPermissionGroups("CustomCompoundPerm");
        assertEquals(0, groups.length);

        groups = pp.getPermissionGroups("CustomAtomicPerm");
        assertNotNull(groups);
        assertEquals(Arrays.asList("ReadWrite", "Write"), Arrays.asList(groups));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.tests:permissions-override2-contrib.xml")
    public void testOverridedVisiblePermission2() {
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        List<String> expected = Arrays.asList("Write", "Read", "Everything");
        assertEquals(expected, permStrings(orderedVisiblePermissions));

        // custom settings for the Section type
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.tests:permissions-override3-contrib.xml")
    public void testOverridedVisiblePermission3() {
        List<UserVisiblePermission> orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(orderedVisiblePermissions);

        List<String> expected = Arrays.asList("Write", "Read", "Everything");
        assertEquals(expected, permStrings(orderedVisiblePermissions));

        // custom settings for the Section type
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Section");
        assertNotNull(orderedVisiblePermissions);
        assertEquals(Arrays.asList("Write", "Everything", "Read"), permStrings(orderedVisiblePermissions));

        // Workspace falls back to default thus is overridden too
        orderedVisiblePermissions = pp.getUserVisiblePermissionDescriptors("Workspace");
        assertNotNull(orderedVisiblePermissions);

        assertEquals(expected, permStrings(orderedVisiblePermissions));
    }

    @Test
    public void testPermissionsVsDeny() {
        List<UserVisiblePermission> vp = pp.getUserVisiblePermissionDescriptors();
        assertNotNull(vp);

        var writeVP = vp.stream()
                        .filter(p -> "ReadWrite".equals(p.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Permission not found"));
        assertEquals("Write", writeVP.getDenyPermission());
        assertEquals("ReadWrite", writeVP.getPermission());
    }

    @Test
    public void testGetPrincipalsToCheck() {
        NuxeoPrincipal principal = new UserPrincipal("bob", Arrays.asList("vps", "males"), false, false);
        String[] principals = SecurityService.getPrincipalsToCheck(principal);
        assertEquals(4, principals.length);
        assertTrue(Arrays.asList(principals).contains("bob"));
        assertTrue(Arrays.asList(principals).contains("vps"));
        assertTrue(Arrays.asList(principals).contains("males"));
        assertTrue(Arrays.asList(principals).contains(SecurityConstants.EVERYONE));
    }

    @Test
    public void testGetPermissions() {
        List<String> perms = Arrays.asList(pp.getPermissions());
        assertTrue(perms.size() > 10);
        assertTrue(perms.contains(SecurityConstants.READ));
        assertTrue(perms.contains(SecurityConstants.WRITE));
        assertTrue(perms.contains(SecurityConstants.EVERYTHING));
    }

    @Test
    public void testGetSubPermissions() {
        assertEquals(List.of(SecurityConstants.READ_VERSION, SecurityConstants.WRITE_VERSION),
                List.of(pp.getSubPermissions(SecurityConstants.VERSION)));
    }

}
