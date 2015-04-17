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
 *     Maxime Hilaire
 */
package org.nuxeo.ftest.cap;

import java.util.Date;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;
import org.openqa.selenium.By;

/**
 * <p>
 * Test Modifying a workspace description in Nuxeo DM.
 * </p>
 * <p>
 * Requirements: the user TEST_USERNAME is created
 * </p>
 * <ol>
 * <li>loginAs TEST_USERNAME</li>
 * <li>followLink to testWorkspace1</li>
 * <li>addToWorklist the workspace</li>
 * <li>logout</li>
 * </ol>
 * 
 */
public class ITWorkListTest extends AbstractTest {

    private final String WORKSPACE_TITLE = "WorkspaceWorkList_" + new Date().getTime();

    @Test
    public void userCanAddItemToWorklist() throws Exception {

        // As an administrator, check that jsmith is created and has rights
        UsersGroupsBasePage usergroupPage = login().getAdminCenter().getUsersGroupsHomePage();
        UsersTabSubPage page = usergroupPage.getUsersTab().searchUser(TEST_USERNAME);
        if (!page.isUserFound(TEST_USERNAME)) {
            usergroupPage = page.getUserCreatePage().createUser(TEST_USERNAME, "John", "Smith", "Nuxeo",
                    "jsmith@nuxeo.com", TEST_PASSWORD, "members");
        }

        DocumentBasePage documentBasePage = usergroupPage.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        AccessRightsSubPage accessRightSubTab = documentBasePage.getManageTab().getAccessRightsSubTab();
        // Need WriteSecurity (so in practice Manage everything) to edit a
        // Workspace
        if (!accessRightSubTab.hasPermissionForUser("Manage everything", TEST_USERNAME)) {
            accessRightSubTab.grantPermissionForUser("Manage everything", TEST_USERNAME);
        }

        logout();

        // Starting the test for real
        documentBasePage = login(TEST_USERNAME, TEST_PASSWORD).getContentTab().goToDocument("Workspaces");

        // Create a new workspace named 'WorkspaceDescriptionModify_{current
        // time}'
        DocumentBasePage workspacePage = createWorkspace(documentBasePage, WORKSPACE_TITLE, "A workspace description");

        workspacePage = workspacePage.goToDocumentByBreadcrumb("Workspaces");
        
        workspacePage.getContentTab().addToWorkList(WORKSPACE_TITLE);
        
        logout();
        
        documentBasePage = login(TEST_USERNAME, TEST_PASSWORD).getContentTab().goToDocument("Workspaces");
        
        DocumentBasePage.findElementWithTimeout(By.xpath("//div[@id='clipboardCopy']//a[text()='"+ WORKSPACE_TITLE+"']"));

        // Clean up repository
        restoreState();

        // Logout
        logout();

    }

    /**
     * @since 5.9.2
     */
    private void restoreState() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
        DocumentBasePage documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

}
