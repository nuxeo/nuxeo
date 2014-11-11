/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.nuxeo.functionaltests.pages.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.tabs.UsersTabSubPage;

/**
 * Create a user in Nuxeo DM.
 */
public class ITUsers extends AbstractTest {

    @Test
    public void testCreateViewDeleteUser() throws Exception {
        String username = "test_create_view_delete_user";
        String password = "test";
        String firstname = "firstname";

        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = login().getHeaderLinks().goToUserManagementPage().getUsersTab();
        usersTab = usersTab.searchUser(username);
        if (!usersTab.isUserFound(username)) {
            page = usersTab.getUserCreatePage().createUser(username, firstname,
                    "lastname1", "company1", "email1", password, "members");
            assertEquals(page.getFeedbackMessage(), "User created");
            usersTab = page.getUsersTab(true);
        }

        // search user
        usersTab = usersTab.searchUser(username);
        assertTrue(usersTab.isUserFound(username));

        // user already exists
        page = usersTab.getUserCreatePage().createUser(username, "firstname1",
                "lastname1", "company1", "email1", "test_user1", "members");
        assertEquals(page.getFeedbackMessage(), "User already exists");

        // modify a user firstname
        firstname = firstname + "modified";
        usersTab = page.getHeaderLinks().goToUserManagementPage().getUsersTab();
        usersTab = usersTab.viewUser(username).getEditUserTab().editUser(
                firstname, null, "newcompany", null, "administrators").getUsersTab(
                true);

        // search user using its new firstname
        usersTab = usersTab.searchUser(firstname);
        assertTrue(usersTab.isUserFound(username));

        // try to login with the new user
        usersTab.getHeaderLinks().logout();
        DocumentBasePage homepage = login(username, password);
        homepage.getHeaderLinks().logout();

        // login as admin
        usersTab = login().getHeaderLinks().goToUserManagementPage().getUsersTab();

        // change password
        usersTab = page.getHeaderLinks().goToUserManagementPage().getUsersTab();
        usersTab = usersTab.searchUser(firstname);
        password = "testmodified";
        usersTab = usersTab.viewUser(username).getChangePasswordUserTab().changePassword(
                password).getHeaderLinks().goToUserManagementPage().getUsersTab(
                true);

        // try to login with the new password
        usersTab.getHeaderLinks().logout();
        homepage = login(username, password);
        homepage.getHeaderLinks().logout();

        // login as admin
        usersTab = login().getHeaderLinks().goToUserManagementPage().getUsersTab();

        // delete user
        usersTab = page.getHeaderLinks().goToUserManagementPage().getUsersTab();
        usersTab = usersTab.searchUser(firstname);
        usersTab = usersTab.viewUser(username).deleteUser();

        // search
        usersTab = usersTab.searchUser(username);
        assertFalse(usersTab.isUserFound(username));

        usersTab.getHeaderLinks().logout();

        // try to login with a delete user
        LoginPage login = loginInvalid(username, password);
    }

}
