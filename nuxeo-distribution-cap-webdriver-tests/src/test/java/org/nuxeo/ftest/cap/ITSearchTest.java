/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserViewTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test of the suggestbox defined with a select2.
 *
 * @since 5.9.6
 */
public class ITSearchTest extends
        AbstractTest {

    private final static String WORKSPACE_TITLE = "WorkspaceTitle"
            + new Date().getTime();

    private static final String USER1_NAME = "user1";

    private static final String USER2_NAME = "user2";

    private static final String FILE_01_NAME = "Test01"
            + new Date().getTime();

    private static final String FILE_02_NAME = "Test02"
            + new Date().getTime();

    private static final String FILE_03_NAME = "Test03"
            + new Date().getTime();

    private static final String XPATH_SUGGESTBOX = "//*[@id='s2id_nxw_suggest_search_box_form:nxw_suggest_search_box_select2']";

    private static final String VALUE_WITH_SPECIALS_CHAR = "h\u00e9h\u00e9";

    /**
     * Init the data for the tests.
     *
     * @throws Exception
     */
    protected void initTest()
            throws Exception {
        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();
        // Init repository with a File and its archived versions
        DocumentBasePage wsPage = createWorkspace(
                defaultDomainPage,
                WORKSPACE_TITLE,
                "Workspace for Webdriver test.");

        // Create test Files
        FileDocumentBasePage file01Page = createFile(
                wsPage,
                FILE_01_NAME,
                "Test File description",
                false, null, null, null);
        wsPage = file01Page.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);
        FileDocumentBasePage file02Page = createFile(
                wsPage,
                FILE_02_NAME,
                "Test File description",
                false, null, null, null);
        wsPage = file02Page.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);
        FileDocumentBasePage file03Page = createFile(
                wsPage,
                FILE_03_NAME,
                "Test File description",
                false, null, null, null);
        wsPage = file03Page.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);

        // Create test users
        createTestUser(USER1_NAME,
                USER1_NAME);
        createTestUser(USER2_NAME,
                USER2_NAME);
    }

    protected void createTestUser(
            String username, String pswd)
            throws Exception {
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(username);
        if (!usersTab.isUserFound(username)) {
            page = usersTab.getUserCreatePage().createUser(
                    username, username,
                    "lastname1",
                    "company1",
                    "email1", pswd,
                    "members");
            usersTab = page.getUsersTab(true);
        }
        // search user
        usersTab = usersTab.searchUser(username);
        assertTrue(usersTab.isUserFound(username));
        logout();
    }

    @Test
    public void simpleSearchTest()
            throws Exception {
        initTest();

        // Test a simple search
        login();
        Select2WidgetElement searchElement = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath(XPATH_SUGGESTBOX)),
                true);
        List<WebElement> listEntries = searchElement.typeAndGetResult("Tes");
        assertTrue(listEntries.size() > 0);
        searchElement.clearSuggestInput();
        searchElement.clickSelect2Field();
        // Search for file01
        listEntries = searchElement.typeAndGetResult(FILE_01_NAME);
        assertTrue(listEntries.size() == 1);
        listEntries.get(0).click();
        // Check if the page is the detail
        FileDocumentBasePage documentPage = asPage(FileDocumentBasePage.class);
        documentPage.checkDocTitle(FILE_01_NAME);

        // Search a user
        searchElement = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath(XPATH_SUGGESTBOX)),
                true);
        listEntries = searchElement.typeAndGetResult(USER1_NAME);
        assertTrue(listEntries.size() == 2);
        listEntries.get(1).click();
        UserViewTabSubPage userPage = asPage(UserViewTabSubPage.class);
        userPage.checkUserName(USER1_NAME);
    }

    @Test
    public void requestEncodingTest() throws Exception {
        login();
        Select2WidgetElement searchElement = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath(XPATH_SUGGESTBOX)),
                true);
        List<WebElement> listEntries = searchElement.typeAndGetResult(VALUE_WITH_SPECIALS_CHAR);
        assertTrue(listEntries.size() == 0);
        // TODO to complete when the suggestbox will be finalized
    }
}
