/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestUsersPageProvider extends NXRuntimeTestCase {

    protected static final String PROVIDER_NAME = "users_listing";

    protected PageProviderService ppService;

    protected UserManager userManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        DatabaseHelper.DATABASE.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/directory-config.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/userservice-config.xml");

        ppService = Framework.getService(PageProviderService.class);
        assertNotNull(ppService);

        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);

        initUsers();
    }

    @After
    public void tearDown() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
        super.tearDown();
    }

    protected void initUsers() throws ClientException {
        userManager.createUser(createUser("jdoe"));
        userManager.createUser(createUser("jsmith"));
        userManager.createUser(createUser("bree"));
        userManager.createUser(createUser("lbramard"));
    }

    protected DocumentModel createUser(String userName) throws ClientException {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userName);
        return newUser;
    }

    @Test
    public void testUsersPageProviderAllMode() throws ClientException {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(UsersPageProvider.USERS_LISTING_MODE_PROPERTY,
                UsersPageProvider.ALL_MODE);
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
    public void testUsersPageProviderSearchMode() throws ClientException {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(UsersPageProvider.USERS_LISTING_MODE_PROPERTY,
                UsersPageProvider.SEARCH_ONLY_MODE);
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
    public void testUsersPageProviderTabbedMode() throws ClientException {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(UsersPageProvider.USERS_LISTING_MODE_PROPERTY,
                UsersPageProvider.TABBED_MODE);
        PageProvider<DocumentModel> usersProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "B");
        List<DocumentModel> users = usersProvider.getCurrentPage();
        assertNotNull(users);
        assertEquals(1, users.size());
        DocumentModel user = users.get(0);
        assertEquals("bree", user.getId());
    }

}
