/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package com.nuxeo.functionaltests;

import org.junit.After;
import org.junit.Before;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.dam.DAMAssetPage;
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;
import org.openqa.selenium.By;

/**
 * @since 5.7.3
 */
public abstract class AbstractDAMTest extends AbstractTest {

    @Before
    public void initializeUsers() throws Exception {
        createTestUser("leela", "test", "Write");
        createTestUser("bender", "test", "Read");
    }

    @After
    public void cleanupAssetLibrary()
            throws DocumentBasePage.UserNotConnectedException {
        login();
        driver.findElement(By.linkText("Asset Library")).click();
        asPage(DocumentBasePage.class).getContentTab().removeAllDocuments();
        logout();
    }

    protected DAMPage getDAMPage() {
        Locator.findElementWithTimeout(By.linkText("DAM")).click();
        return asPage(DAMPage.class);
    }

    protected DAMAssetPage getDAMassetPage() {
        return asPage(DAMAssetPage.class);
    }

    /**
     * Creates a test user with given name and password, and give it write
     * rights on Asset Library
     *
     */
    protected void createTestUser(String username, String password, String grantPermission)
            throws DocumentBasePage.UserNotConnectedException {
        UsersGroupsBasePage page;
        DocumentBasePage base = login();
        try {
            UsersTabSubPage usersTab = base.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
            usersTab = usersTab.searchUser(username);
            if (!usersTab.isUserFound(username)) {
                page = usersTab.getUserCreatePage().createUser(username, "",
                        "", "company1", "email1", password, "members");
                page.getUsersTab(true);
                // Go to Workspaces
                DocumentBasePage dm = base.getDocumentManagement();
                driver.findElement(By.linkText("Asset Library")).click();
                DocumentBasePage assetLibraryPage = dm.asPage(DocumentBasePage.class);
                AccessRightsSubPage rightsPage = assetLibraryPage.getManageTab().getAccessRightsSubTab();
                rightsPage.addPermissionForUser(username, grantPermission, true);
            }
        } finally {
            logout();
        }
    }

}
