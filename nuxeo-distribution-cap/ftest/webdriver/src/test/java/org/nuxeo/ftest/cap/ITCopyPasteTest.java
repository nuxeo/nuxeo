/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     guillaume
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test the copy and past feature.
 *
 * @since 5.8
 */
public class ITCopyPasteTest extends AbstractTest {

    private final static String WORKSPACE1_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    private final static String WORKSPACE2_TITLE = "WorkspaceTitle_"
            + new Date().getTime() + "a";

    private final String FILE1_NAME = "testFile1";

    private void prepare() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage;
        DocumentBasePage s = login();

        // Create a new user if not exist
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = s.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, TEST_USERNAME,
                    null, null, TEST_USERNAME, TEST_PASSWORD, "members");
            usersTab = page.getUsersTab(true);
        } // search user usersTab =
        usersTab.searchUser(TEST_USERNAME);
        assertTrue(usersTab.isUserFound(TEST_USERNAME));

        // create a wokspace1 and grant all rights to the test user
        documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE1_TITLE, null);
        AccessRightsSubPage accessRightSubTab = workspacePage.getManageTab().getAccessRightsSubTab();
        // Need Read
        if (!accessRightSubTab.hasPermissionForUser("Read", TEST_USERNAME)) {
            accessRightSubTab.addPermissionForUser(TEST_USERNAME, "Read", true);
        }
        // Create test File 1
        createFile(workspacePage, FILE1_NAME, null, false, null, null, null);

        workspacePage.getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        workspacePage = createWorkspace(documentBasePage, WORKSPACE2_TITLE,
                null);
        accessRightSubTab = workspacePage.getManageTab().getAccessRightsSubTab();
        if (!accessRightSubTab.hasPermissionForUser("Manage everything",
                TEST_USERNAME)) {
            accessRightSubTab.addPermissionForUser(TEST_USERNAME,
                    "Manage everything", true);
        }

        logout();
    }

    /**
     * Copy and paste a simple file.
     *
     * @since 5.8
     */
    @Test
    public void testSimpleCopyAndPaste() throws UserNotConnectedException,
            IOException, ParseException {
        prepare();

        DocumentBasePage documentBasePage;

        // Log as test user and edit the created workdspace
        documentBasePage = loginAsTestUser().getContentTab().goToDocument(
                "Workspaces").getContentTab().goToDocument(WORKSPACE1_TITLE);

        ContentTabSubPage contentTabSubPage = documentBasePage.getContentTab();

        contentTabSubPage.copyByTitle(FILE1_NAME);

        documentBasePage = contentTabSubPage.getHeaderLinks().getNavigationSubPage().goToDocument(
                WORKSPACE2_TITLE);

        contentTabSubPage = documentBasePage.getContentTab();

        contentTabSubPage = contentTabSubPage.paste();

        List<WebElement> docs = contentTabSubPage.getChildDocumentRows();

        assertNotNull(docs);
        assertEquals(docs.size(), 1);
        assertNotNull(docs.get(0).findElement(By.linkText(FILE1_NAME)));

        restoreSate();
    }

    private void restoreSate() throws UserNotConnectedException {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
        DocumentBasePage documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        deleteWorkspace(documentBasePage, WORKSPACE1_TITLE);
        deleteWorkspace(documentBasePage, WORKSPACE2_TITLE);
        logout();
    }

}
