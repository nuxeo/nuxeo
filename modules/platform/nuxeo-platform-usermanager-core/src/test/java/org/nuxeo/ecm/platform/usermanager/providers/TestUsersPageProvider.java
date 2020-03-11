/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.ecm.platform.usermanager.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManagerTestCase;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUsersPageProvider extends UserManagerTestCase {

    protected static final String PROVIDER_NAME = "users_listing";

    @Inject
    protected PageProviderService ppService;

    @Before
    public void initUsers() {
        userManager.createUser(createUser("jdoe"));
        userManager.createUser(createUser("jsmith"));
        userManager.createUser(createUser("bree"));
        userManager.createUser(createUser("lbramard"));
    }

    @After
    public void cleanUsers() {
        userManager.deleteUser("jdoe");
        userManager.deleteUser("jsmith");
        userManager.deleteUser("bree");
        userManager.deleteUser("lbramard");
    }

    protected DocumentModel createUser(String userName) {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userName);
        return newUser;
    }

    @Test
    public void testUsersPageProviderAllMode() {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(UsersPageProvider.USERS_LISTING_MODE_PROPERTY, UsersPageProvider.ALL_MODE);
        PageProvider<DocumentModel> usersProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "");
        List<DocumentModel> users = usersProvider.getCurrentPage();
        assertNotNull(users);
        assertEquals(6, users.size());

        DocumentModel user = users.get(0);
        assertEquals("Administrator", user.getId());
        user = users.get(1);
        assertEquals("bree", user.getId());
        user = users.get(2);
        assertEquals("Guest", user.getId());
        user = users.get(3);
        assertEquals("jdoe", user.getId());
        user = users.get(4);
        assertEquals("jsmith", user.getId());
        user = users.get(5);
        assertEquals("lbramard", user.getId());
    }

    @Test
    public void testUsersPageProviderSearchMode() {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(UsersPageProvider.USERS_LISTING_MODE_PROPERTY, UsersPageProvider.SEARCH_ONLY_MODE);
        PageProvider<DocumentModel> usersProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "j");
        List<DocumentModel> users = usersProvider.getCurrentPage();
        assertNotNull(users);
        assertEquals(2, users.size());
        DocumentModel user = users.get(0);
        assertEquals("jdoe", user.getId());
        user = users.get(1);
        assertEquals("jsmith", user.getId());
    }

    @Test
    public void testUsersPageProviderTabbedMode() {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(UsersPageProvider.USERS_LISTING_MODE_PROPERTY, UsersPageProvider.TABBED_MODE);
        PageProvider<DocumentModel> usersProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "B");
        List<DocumentModel> users = usersProvider.getCurrentPage();
        assertNotNull(users);
        assertEquals(1, users.size());
        DocumentModel user = users.get(0);
        assertEquals("bree", user.getId());
    }

}
