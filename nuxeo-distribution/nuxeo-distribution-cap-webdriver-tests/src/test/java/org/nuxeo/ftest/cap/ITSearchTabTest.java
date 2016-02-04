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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.search.DefaultSearchSubPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.nuxeo.functionaltests.pages.search.SearchResultsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.PermissionsSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 6.0
 */
public class ITSearchTabTest extends AbstractTest {

    private final static String WORKSPACE1_TITLE = "WorkspaceTitle1_" + new Date().getTime();

    private final static String WORKSPACE2_TITLE = "WorkspaceTitle2_" + new Date().getTime();

    private static final String SEARCH_PATH = "/Domain/Workspaces/" + WORKSPACE1_TITLE;

    private static final String MY_FAVORITES_COLLECTION = "My Favorites";

    public final static String[] SUBJECTS = { "Comics", "Religion", "Education" };

    public final static String[] FULL_PATH_SUBJECTS = { "Art/Comics", "Society/Religion", "Society/Education" };

    public final static String COVERAGE = "France";

    public final static String FULL_PATH_COVERAGE = "Europe/France";

    @Before
    public void setup() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage;

        DocumentBasePage s = login();

        // Create a new user if not exist
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = s.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, TEST_USERNAME, "lastname1", "company1",
                    "email1", TEST_PASSWORD, "members");
            usersTab = page.getUsersTab(true);
        } // search user usersTab =
        usersTab.searchUser(TEST_USERNAME);
        assertTrue(usersTab.isUserFound(TEST_USERNAME));

        // create 2 workspaces and grant all rights to the test user
        documentBasePage = usersTab.exitAdminCenter()
                                   .getHeaderLinks()
                                   .getNavigationSubPage()
                                   .goToDocument("Workspaces");
        createTestWorkspace(documentBasePage, WORKSPACE1_TITLE, true);
        createTestWorkspace(documentBasePage, WORKSPACE2_TITLE, false);
        logout();
    }

    protected void createTestWorkspace(DocumentBasePage documentBasePage, String title, boolean createTestFile)
            throws IOException {
        DocumentBasePage workspacePage = createWorkspace(documentBasePage, title, null);
        if (createTestFile) {
            PermissionsSubPage permissionsSubPage = workspacePage.getPermissionsTab();
            // Need WriteSecurity (so in practice Manage everything) to edit a
            // Workspace
            if (!permissionsSubPage.hasPermissionForUser("Manage everything", TEST_USERNAME)) {
                permissionsSubPage.grantPermissionForUser("Manage everything", TEST_USERNAME);
            }
            // Create test File
            FileDocumentBasePage filePage = createFile(workspacePage, "Test file", "Test File description", false,
                    null, null, null);
            EditTabSubPage editTabSubPage = filePage.getEditTab();

            Select2WidgetElement subjectsWidget = new Select2WidgetElement(
                    driver,
                    driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']")),
                    true);
            subjectsWidget.selectValues(SUBJECTS);

            Select2WidgetElement coverageWidget = new Select2WidgetElement(
                    driver,
                    driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_1_select2']")),
                    false);
            coverageWidget.selectValue(COVERAGE);
            editTabSubPage.save();
        }
    }

    @Test
    public void testSearch() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = loginAsTestUser();

        // navigate to saved searches tab to init cache (non regression test for NXP-18624)
        HomePage home = documentBasePage.goToHomePage().goToSavedSearches();
        // no searches
        assertEquals(1, driver.findElements(By.className("emptyResult")).size());

        SearchPage searchPage = home.goToSearchPage();
        SearchResultsSubPage resultPanelSubPage = searchPage.getSearchResultsSubPage();
        final int nbCurrentDoc = resultPanelSubPage.getNumberOfDocumentInCurrentPage();
        assertTrue(nbCurrentDoc > 1);
        DefaultSearchSubPage searchLayoutSubPage = searchPage.getDefaultSearch();

        // Test aggregates
        Map<String, Integer> coverageAgg = searchLayoutSubPage.getAvailableCoverageAggregate();
        assertEquals(1, coverageAgg.size());
        assertEquals(new Integer(1), coverageAgg.get(FULL_PATH_COVERAGE));
        Map<String, Integer> subjectsAgg = searchLayoutSubPage.getAvailableSubjectsAggregate();
        assertEquals(3, subjectsAgg.size());
        for (String subject : FULL_PATH_SUBJECTS) {
            assertEquals(new Integer(1), subjectsAgg.get(subject));
        }
        // Select and unselect France
        searchPage = searchLayoutSubPage.selectCoverageAggregate(FULL_PATH_COVERAGE);
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        assertEquals(1, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        searchPage = searchLayoutSubPage.selectCoverageAggregate(FULL_PATH_COVERAGE);
        assertEquals(nbCurrentDoc, resultPanelSubPage.getNumberOfDocumentInCurrentPage());

        // Test select path widget
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        searchLayoutSubPage.selectPath(SEARCH_PATH);
        searchPage = searchLayoutSubPage.filter();
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        assertEquals(1, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        searchLayoutSubPage = searchPage.getDefaultSearch();
        searchLayoutSubPage.deselectPath(SEARCH_PATH);
        searchPage = searchLayoutSubPage.filter();
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        assertEquals(nbCurrentDoc, resultPanelSubPage.getNumberOfDocumentInCurrentPage());

        // Test Collections Widget
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        searchLayoutSubPage.selectCollections(new String[] { MY_FAVORITES_COLLECTION });
        searchPage = searchLayoutSubPage.filter();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        assertEquals(0, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        List<String> selectedCollections = searchLayoutSubPage.getSelectedCollections();
        assertEquals(1, selectedCollections.size());
        assertEquals(MY_FAVORITES_COLLECTION, selectedCollections.get(0));

        // save this search
        String saveAsPath = "//input[contains(@id, 'nxw_saveSearch_link')]";
        assertEquals(1, driver.findElements(By.xpath(saveAsPath)).size());
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        driver.findElement(By.xpath(saveAsPath)).click();
        arm.end();

        WebElement fancybox = Locator.findElementWithTimeout(By.id("nxw_saveSearch_after_view_box"));
        String ssTitle = "Test Saved Search " + new Date().getTime();
        fancybox.findElement(By.xpath(".//input[@type='text']")).sendKeys(ssTitle);
        arm.begin();
        fancybox.findElement(By.xpath(".//input[@value='Save']")).click();
        arm.end();

        SearchPage sp = asPage(SearchPage.class);
        assertEquals(ssTitle, sp.getSelectedSearch());

        // check that search was saved in home
        sp.goToHomePage().goToSavedSearches();
        assertEquals(1, driver.findElements(By.linkText(ssTitle)).size());

        logout();
        tearDown();

        // test aggregate on deleted user
        documentBasePage = login();
        searchPage = documentBasePage.goToSearchPage();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        Map<String, Integer> authorAggs = searchLayoutSubPage.getAvailableAuthorAggregate();
        boolean testUserFound = false;
        for (Entry<String, Integer> e : authorAggs.entrySet()) {
            if (e.getKey().equals(TEST_USERNAME)) {
                testUserFound = true;
                break;
            }
        }
        assertTrue(testUserFound);
    }

    public void tearDown() throws UserNotConnectedException {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
        DocumentBasePage documentBasePage = usersTab.exitAdminCenter()
                                                    .getHeaderLinks()
                                                    .getNavigationSubPage()
                                                    .goToDocument("Workspaces");
        deleteWorkspace(documentBasePage, WORKSPACE1_TITLE);
        deleteWorkspace(documentBasePage, WORKSPACE2_TITLE);
        logout();
    }
}
