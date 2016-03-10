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

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.TestConstants.TEST_NOTE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.functionaltests.Constants.NOTE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_TITLE;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.UsersGroupsHomePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupCreationFormPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupViewTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupsTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserCreationFormPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserViewTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.CommentsTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.HistoryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.RelationTabSubPage;
import org.nuxeo.functionaltests.pages.workspace.WorkspaceRepositoryPage;
import org.openqa.selenium.By;

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
        RestHelper.createGroup("Johns", "Johns group", new String[] { "jdoe", "jsmith", "bree" }, null);
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, NOTE_TYPE, TEST_NOTE_TITLE, "Test Note description");
    }

    @After
    public void after() throws UserNotConnectedException {
        RestHelper.cleanup();
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

    @Test
    public void testCreateSubGroupAdmin() throws Exception {
        try {
            // Create sub-admins group
            GroupsTabSubPage groupsTab = login().getAdminCenter().getUsersGroupsHomePage().getGroupsTab();
            groupsTab = groupsTab.getGroupCreatePage()
                                 .createGroup("sub-admins", null, new String[] { "jdoe" }, null)
                                 .getGroupsTab(true);
            assertTrue(groupsTab.searchGroup("sub-admins").isGroupFound("sub-admins"));

            // Add sub-admins as sub group of administrators
            GroupViewTabSubPage adminGroupViewPage = groupsTab.searchGroup("admi").viewGroup("administrators");
            adminGroupViewPage = adminGroupViewPage.getEditGroupTab().setSubGroups("sub-admins").save();
            Locator.waitForTextPresent(By.id("viewGroupView:viewGroup"), "sub-admins");

            // Check that jdoe is member of sub-admins
            UserViewTabSubPage userPage = adminGroupViewPage.getUsersTab().searchUser("jdo").viewUser("jdoe");
            Locator.findElement(By.linkText("sub-admins"));
            logout();

            // Check member's rights on TEST_WORKSPACE_TITLE / TEST_NOTE_TITLE
            WorkspaceRepositoryPage repository = login("jdoe", "jdoe1").goToWorkspaces().goToRepository();
            Locator.waitUntilElementPresent(By.id("nxw_newDomain_form:nxw_newDomain"));
            DocumentBasePage domainPage = repository.getContentTab().goToDocument("Domain");
            DocumentBasePage workspacesPage = domainPage.getContentTab().goToDocument(WORKSPACES_TITLE);
            Locator.waitUntilElementPresent(By.id("nxw_TAB_WORKSPACE_EDIT_form:nxw_TAB_WORKSPACE_EDIT"));
            Locator.waitUntilElementPresent(By.id("nxw_newWorkspace_form:nxw_newWorkspace"));
            NoteDocumentBasePage notePage = workspacesPage.getContentTab()
                                                          .goToDocument(TEST_WORKSPACE_TITLE)
                                                          .getContentTab()
                                                          .goToDocument(TEST_NOTE_TITLE)
                                                          .asPage(NoteDocumentBasePage.class);
            Locator.waitUntilElementPresent(By.id("nxw_TAB_PUBLISH_form:nxw_TAB_PUBLISH"));
            Locator.waitUntilElementPresent(By.id("nxw_TAB_EDIT_form:nxw_TAB_EDIT"));
            RelationTabSubPage relationPage = notePage.getFilesTab().getRelationTab();
            Locator.waitUntilElementPresent(By.linkText("Add a New Relation"));
            CommentsTabSubPage commentsPage = relationPage.getCommentsTab();
            Locator.waitUntilElementPresent(By.linkText("Add a Comment"));
            HistoryTabSubPage historyPage = commentsPage.getHistoryTab();
            Locator.waitUntilElementPresent(By.linkText("Event Log"));
            Locator.waitUntilElementPresent(By.linkText("Archived Versions"));

            // Check if jdoe can create an user or a group
            UserCreationFormPage userCreatePage = historyPage.getAdminCenter()
                                                             .getUsersGroupsHomePage()
                                                             .getUsersTab()
                                                             .getUserCreatePage();
            Locator.waitUntilElementPresent(By.id("createUserView:createUser:button_save"));
            GroupCreationFormPage groupCreatePage = userCreatePage.getGroupsTab(true).getGroupCreatePage();
            Locator.waitUntilElementPresent(By.id("createGroupView:createGroup:button_save"));
            logout();
        } finally {
            RestHelper.deleteGroup("sub-admins");
        }

    }

}
