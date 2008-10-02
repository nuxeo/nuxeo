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
 *     Florent Guillaume
 *
 * $Id: TestUserService.java 28010 2007-12-07 19:23:44Z fguillaume $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.nuxeo.ecm.platform.usermanager.UserManager.MatchType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 *
 */
public class TestUserService extends NXRuntimeTestCase {

    UserManager userManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.usermanager",
                "OSGI-INF/UserService.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-userservice-config.xml");
        userManager = Framework.getService(UserManager.class);
    }

    public void testGetUserManagerFromFramework() throws Exception {
        assertNotNull(userManager);
    }

    public void testConfig() throws Exception {
        FakeUserManagerImpl fum = (FakeUserManagerImpl) userManager;
        assertEquals("search_only", userManager.getUserListingMode());
        assertEquals("search_oh_yeah", userManager.getGroupListingMode());
        assertEquals("tehroot", fum.rootLogin);
        assertEquals("defgr", userManager.getDefaultGroup());
        assertEquals("name", userManager.getUserSortField());
        assertEquals("sn", fum.groupSortField);
        assertEquals("somedir", fum.userDirectoryName);
        assertEquals("mail", fum.userEmailField);
        // append mode:
        assertEquals(new HashSet<String>(Arrays.asList("first", "last",
                "username", "firstName", "lastName", "email")), fum.getUserSearchFields());
        assertEquals(MatchType.SUBSTRING, fum.userSearchFields.get("username"));
        assertEquals(MatchType.SUBSTRING, fum.userSearchFields.get("firstName"));
        assertEquals(MatchType.SUBSTRING, fum.userSearchFields.get("lastName"));
        assertEquals(MatchType.SUBSTRING, fum.userSearchFields.get("first"));
        assertEquals(MatchType.EXACT, fum.userSearchFields.get("last"));
        assertEquals(MatchType.SUBSTRING, fum.userSearchFields.get("email"));
        assertEquals("somegroupdir", fum.groupDirectoryName);
        assertEquals("members", fum.groupMembersField);
        assertEquals("subg", fum.groupSubGroupsField);
        assertEquals("parentg", fum.groupParentGroupsField);
        // anonymous user
        Map<String, String> props = new HashMap<String, String>();
        props.put("first", "Anonymous");
        props.put("last", "Coward");
        props.put(UserManager.ANONYMOUS_USER_ID_KEY, "Guest");
        assertEquals(props, fum.anonymousUser);
    }

    public void testOverride() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-userservice-override-config.xml");
        FakeUserManagerImpl fum = (FakeUserManagerImpl) userManager;
        assertEquals("bob", fum.rootLogin);
        assertEquals("id", userManager.getUserSortField());
        // the rest should be unchanged
        assertEquals("search_only", userManager.getUserListingMode());
        assertEquals("search_oh_yeah", userManager.getGroupListingMode());
        assertEquals("defgr", userManager.getDefaultGroup());
        assertEquals("sn", fum.groupSortField);
        // anonymous user removed
        assertNull(fum.anonymousUser);
    }

    public void testValidatePassword() throws Exception {
        FakeUserManagerImpl fum = (FakeUserManagerImpl) userManager;
        assertTrue(fum.validatePassword(""));
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-userservice-override-config.xml");
        fum = (FakeUserManagerImpl) userManager;
        assertFalse(fum.validatePassword(""));
        assertFalse(fum.validatePassword("azerty"));
        assertFalse(fum.validatePassword("az\u00e9rtyyy"));
        assertFalse(fum.validatePassword("aZeRtYuIoP"));
        assertFalse(fum.validatePassword("aZ1\u00e9R2tY3"));
        assertFalse(fum.validatePassword("aZE1RTY2"));
        assertTrue(fum.validatePassword("aZ1eR2tY3"));
    }
}
