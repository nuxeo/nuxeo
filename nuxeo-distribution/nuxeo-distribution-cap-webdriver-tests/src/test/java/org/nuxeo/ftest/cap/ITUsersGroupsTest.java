/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.UsersGroupsHomePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupsTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.openqa.selenium.By;

import static org.junit.Assert.assertTrue;

/**
 * Users & Groups search & rights tests.
 *
 * @since 8.2
 */
public class ITUsersGroupsTest extends AbstractTest {

    @Before
    public void before() throws UserNotConnectedException {
        RestHelper.createUser("jdoe", "jdoe1", "John", "Doe", "Nuxeo", "dev@null", null);
        RestHelper.createUser("jsmith", "jsmith1", "Jim", "Smith", "Nuxeo", "dev@null", null);
        RestHelper.createUser("bree", "bree1", "Bree", "Van de Kaamp", "Nuxeo", "dev@null", null);
        RestHelper.createUser("lbramard", "lbramard1", "Lucien", "Bramard", "Nuxeo", "dev@null", null);

        GroupsTabSubPage groupsTab = login().getAdminCenter().getUsersGroupsHomePage().getGroupsTab();
        groupsTab.getGroupCreatePage().createGroup("Johns", null, new String[] { "jdoe", "jsmith", "bree" }, null);
        logout();
    }

    @After
    public void after() throws UserNotConnectedException {
        RestHelper.cleanup();

        GroupsTabSubPage groupsTab = login().getAdminCenter().getUsersGroupsHomePage().getGroupsTab();
        groupsTab.searchGroup("Johns").viewGroup("Johns").deleteGroup();
        logout();
    }

    @Test
    public void testSearchUsersGroupsFromHome() throws Exception {
        UsersGroupsHomePage page = getLoginPage().login("bree", "bree1", HomePage.class).goToUsersGroupsHomePage();
        UsersTabSubPage usersPage = page.getUsersTab().searchUser("j");
        assertTrue(usersPage.isUserFound("jdoe"));
        assertTrue(usersPage.isUserFound("jsmith"));
        usersPage.searchUser("foo");
        Locator.waitForTextPresent(By.id("usersListingView:users_listing"), "No user matches the entered criteria.");

        GroupsTabSubPage groupsPage = asPage(UsersGroupsHomePage.class).getGroupsTab().searchGroup("j");
        assertTrue(groupsPage.isGroupFound("Johns"));
        groupsPage.searchGroup("foo");
        Locator.waitForTextPresent(By.id("groupsListingView:groups_listing"), "No group matches the entered criteria.");
        logout();
    }


}
