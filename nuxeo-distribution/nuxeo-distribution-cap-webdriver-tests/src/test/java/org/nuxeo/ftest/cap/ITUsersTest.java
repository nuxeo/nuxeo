/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.profile.ProfilePage;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserCreationFormPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserViewTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.openqa.selenium.By;

/**
 * Create a user in Nuxeo DM.
 */
@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITUsersTest extends AbstractTest {

    @After
    public void after() {
        RestHelper.deleteUser("jsmith");
    }

    @Test
    @Ignore
    public void testInviteUser() throws Exception {

        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        String username = TEST_USERNAME + System.currentTimeMillis();

        usersTab.getUserCreatePage().inviteUser(username, username, "lastname1", "company1", "email1", "members");

        // Need few seconds to display the search view after
        AbstractPage.findElementWithTimeout(By.id("usersListingView:searchForm:searchText"));

        // search user
        usersTab = usersTab.searchUser(username);
        assertFalse(usersTab.isUserFound(username));
    }

    @Test
    public void userCanChangeItsOwnPassword() throws Exception {
        String firstname = "firstname";

        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        String username = "jsmith";
        usersTab = usersTab.searchUser(username);
        if (!usersTab.isUserFound(username)) {
            page = usersTab.getUserCreatePage().createUser(username, firstname, "lastname1", "company1", "email1",
                    TEST_PASSWORD, "members");
            // no confirmation message anymore
            // assertEquals(page.getFeedbackMessage(), "User created");
            usersTab = page.getUsersTab(true);
        }

        logout();

        // Change the user password
        String newPassword = "newpwd";
        ProfilePage profilePage = login(username, TEST_PASSWORD).getUserHome().goToProfile();
        profilePage.getChangePasswordUserTab().changePassword(TEST_PASSWORD, newPassword);
        logout();

        login(username, newPassword).getUserHome().goToProfile();
        logout();

    }

    @Test
    public void testCreateViewDeleteUser() throws Exception {
        String firstname = "firstname";

        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, firstname, "lastname1", "company1", "email1",
                    TEST_PASSWORD, "members");
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
        page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, "firstname1", "lastname1", "company1", "email1",
                TEST_PASSWORD, "members");
        assertEquals("User already exists.", page.getErrorFeedbackMessage());
        // cancel
        usersTab = asPage(UserCreationFormPage.class).cancelCreation();

        // modify a user firstname
        firstname = firstname + "modified";
        usersTab = page.getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.viewUser(TEST_USERNAME)
                           .getEditUserTab()
                           .editUser(firstname, null, "newcompany", null, "Administrators group")
                           .backToTheList();

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
        UserViewTabSubPage tab = usersTab.viewUser(TEST_USERNAME).getChangePasswordUserTab().changePassword(password);

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
