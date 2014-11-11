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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserCreationFormPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserViewTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;

/**
 * Create a user in Nuxeo DM.
 */
public class ITUsersTest extends AbstractTest {

    @Test
    public void testCreateViewDeleteUser() throws Exception {
        String firstname = "firstname";

        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, firstname,
                    "lastname1", "company1", "email1", TEST_PASSWORD, "members");
            // no confirmation message anymore
            // assertEquals(page.getFeedbackMessage(), "User created");
            usersTab = page.getUsersTab(true);
        }

        // search user
        usersTab = usersTab.searchUser(TEST_USERNAME);
        assertTrue(usersTab.isUserFound(TEST_USERNAME));

        // exit admin center and reconnect
        usersTab = usersTab.exitAdminCenter().getAdminCenter().getUsersGroupsHomePage().getUsersTab();

        // user already exists
        page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, "firstname1",
                "lastname1", "company1", "email1", TEST_PASSWORD, "members");
        assertEquals("User already exists", page.getErrorFeedbackMessage());
        // cancel
        usersTab = asPage(UserCreationFormPage.class).cancelCreation();

        // modify a user firstname
        firstname = firstname + "modified";
        usersTab = page.getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.viewUser(TEST_USERNAME).getEditUserTab().editUser(
                firstname, null, "newcompany", null, "Administrators group").backToTheList();

        // search user using its new firstname
        usersTab = usersTab.searchUser(firstname);
        assertTrue(usersTab.isUserFound(TEST_USERNAME));

        // try to login with the new user
        logout();
        login(TEST_USERNAME, TEST_PASSWORD);
        logout();

        // login as admin
        page = login().getAdminCenter().getUsersGroupsHomePage();

        // change password
        usersTab = page.getUsersTab();
        usersTab = usersTab.searchUser(firstname);
        String password = "testmodified";
        UserViewTabSubPage tab = usersTab.viewUser(TEST_USERNAME).getChangePasswordUserTab().changePassword(
                password);

        tab.exitAdminCenter();
        logout();
        // try to login with the new password
        login(TEST_USERNAME, password);
        logout();

        // login as admin
        page = login().getAdminCenter().getUsersGroupsHomePage();

        // delete user
        usersTab = page.getUsersTab();
        usersTab = usersTab.searchUser(firstname);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();

        // search
        usersTab = usersTab.searchUser(TEST_USERNAME);
        assertFalse(usersTab.isUserFound(TEST_USERNAME));

        logout();

        // try to login with a delete user
        loginInvalid(TEST_USERNAME, TEST_PASSWORD);
    }

}
