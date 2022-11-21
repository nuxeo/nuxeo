/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.functionaltests.Constants.COLLECTION_TYPE;
import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Assert;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.ScreenshotTaker;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.contentView.ContentViewElement.ResultLayout;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.search.DefaultSearchSubPage;
import org.nuxeo.functionaltests.pages.search.QuickSearchSubPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.nuxeo.functionaltests.pages.search.SearchResultsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 6.0
 */
public class ITSearchTabTest extends AbstractTest {

    private final static String WORKSPACE1_TITLE = ITSearchTabTest.class.getSimpleName() + "_WorkspaceTitle1_"
            + new Date().getTime();

    private final static String WORKSPACE2_TITLE = ITSearchTabTest.class.getSimpleName() + "_WorkspaceTitle2_"
            + new Date().getTime();

    private static final String SEARCH_PATH = "/Domain/Workspaces/" + WORKSPACE1_TITLE;

    public final static String[] SUBJECTS = { "Comics", "Religion", "Education" };

    public final static String[] FULL_PATH_SUBJECTS = { "Art/Comics", "Society/Religion", "Society/Education" };

    public final static String COVERAGE = "France";

    public final static String FULL_PATH_COVERAGE = "Europe/France";

    protected static final String SELECT_ALL_SAVED_SEARCHES_BUTTON_ID = "all_saved_searches:all_saved_searches_repeat:0:nxl_document_listing_table_1:listing_ajax_selection_box_with_current_document_header";

    protected static final String PERMANENT_DELETE_SAVED_SEARCHES_BUTTON_ID = "all_saved_searches_buttons:nxw_savedSearchesCurrentSelectionDelete_form:nxw_savedSearchesCurrentSelectionDelete";

    static final Log log = LogFactory.getLog(AbstractTest.class);

    @Before
    public void setup() throws UserNotConnectedException, IOException {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_USERNAME, "lastname1", "company1", "email1",
                "members");
        String wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE1_TITLE);
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE2_TITLE);
        RestHelper.addPermission(wsId, TEST_USERNAME, "Everything");

        RestHelper.createDocument(WORKSPACES_PATH + WORKSPACE1_TITLE + "/", COLLECTION_TYPE, "Test Collection");

        loginAsTestUser();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        FileDocumentBasePage filePage = asPage(DocumentBasePage.class).createFile("Test file for ITSearchTabTest",
                "Test File description", false, null, null, null);
        EditTabSubPage editTabSubPage = filePage.getEditTab();

        Select2WidgetElement subjectsWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']")),
                true);
        subjectsWidget.selectValues(SUBJECTS);

        Select2WidgetElement coverageWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_1_select2']")),
                false);
        coverageWidget.selectValue(COVERAGE);
        editTabSubPage.save();

        logout();
    }

    @After
    public void after() throws UserNotConnectedException {
        RestHelper.cleanupUsers();
        RestHelper.cleanupGroups();
        try {
            // test aggregate on deleted user, on user workspace
            DocumentBasePage documentBasePage = login();
            SearchPage searchPage = documentBasePage.goToSearchPage();
            DefaultSearchSubPage searchLayoutSubPage = searchPage.getDefaultSearch();
            Map<String, Integer> authorAggs = searchLayoutSubPage.getAvailableAuthorAggregate();
            // NXP-19617: take screenshot to help understanding potential randoms
            ScreenshotTaker taker = new ScreenshotTaker();
            File screenShot = taker.takeScreenshot(driver, "ITSearchTabTest-after-authorAggs-");
            log.warn("Screenshot taken for author '" + TEST_USERNAME + "' and aggs='" + authorAggs + "': "
                    + screenShot.getAbsolutePath());
            boolean testUserFound = false;
            for (Entry<String, Integer> e : authorAggs.entrySet()) {
                if (e.getKey().equals(TEST_USERNAME)) {
                    testUserFound = true;
                    break;
                }
            }
            assertTrue(testUserFound);
        } finally {
            RestHelper.cleanupDocuments();
        }
    }

    protected void saveSearch(String title) {
        String saveAsPath = "//input[contains(@id, 'nxw_saveSearch_link')]";
        assertEquals(1, driver.findElements(By.xpath(saveAsPath)).size());
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.findElementWaitUntilEnabledAndClick(By.xpath(saveAsPath));
        arm.end();

        WebElement fancybox = Locator.findElementWithTimeout(By.id("nxw_saveSearch_after_view_box"));
        fancybox.findElement(By.xpath(".//input[@type='text']")).sendKeys(title);
        arm.begin();
        Locator.findElementWaitUntilEnabledAndClick(fancybox, By.xpath(".//input[@value='Save']"));
        arm.end();
    }

    protected void deleteSavedSearches(DocumentBasePage page) {
        page.goToHomePage().goToSavedSearches();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.findElementWaitUntilEnabledAndClick(By.id(SELECT_ALL_SAVED_SEARCHES_BUTTON_ID));
        arm.end();

        Locator.findElementWaitUntilEnabledAndClick(By.id(PERMANENT_DELETE_SAVED_SEARCHES_BUTTON_ID));
        driver.switchTo().alert().accept();
    }

    protected void checkURL(String searchName) {
        assertTrue("URL is " + driver.getCurrentUrl(),
                driver.getCurrentUrl().contains("search?contentViewName=" + searchName));
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
        assertEquals(2, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        searchLayoutSubPage = searchPage.getDefaultSearch();
        searchLayoutSubPage.deselectPath(SEARCH_PATH);
        searchPage = searchLayoutSubPage.filter();
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        assertEquals(nbCurrentDoc, resultPanelSubPage.getNumberOfDocumentInCurrentPage());

        // Test Collections Widget
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        searchLayoutSubPage.selectCollections(new String[] { "Test Collection" });
        searchPage = searchLayoutSubPage.filter();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        assertEquals(0, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        List<String> selectedCollections = searchLayoutSubPage.getSelectedCollections();
        assertEquals(1, selectedCollections.size());
        assertEquals("Test Collection", selectedCollections.get(0));

        // save this search
        String ssTitle = "Test Saved Search " + new Date().getTime();
        saveSearch(ssTitle);
        SearchPage sp = asPage(SearchPage.class);
        assertEquals(ssTitle, sp.getSelectedSearch());

        // check that search was saved in home
        sp.goToHomePage().goToSavedSearches();
        assertEquals(1, driver.findElements(By.linkText(ssTitle)).size());

        // navigate to it
        Locator.findElementWaitUntilEnabledAndClick(By.linkText(ssTitle));
        // check home tab context is ok
        HomePage hp = asPage(HomePage.class);
        assertTrue(hp.isMainTabSelected(hp.homePageLink));

        deleteSavedSearches(hp);
        logout();
    }

    /*
     * NXP-19898
     */
    @Test
    public void testResultColumnSimpleSearch() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = loginAsTestUser();

        // Run a quick search
        SearchPage searchPage = documentBasePage.goToSearchPage();
        QuickSearchSubPage quickSearch = searchPage.getQuickSearch();
        quickSearch.filter();

        // switch to listing
        SearchResultsSubPage resultSubPage = searchPage.getSearchResultsSubPage();
        ContentViewElement contentView = resultSubPage.getContentView().switchToResultLayout(ResultLayout.LISTING);

        // add column
        WebElement addColumn = contentView.getActionByTitle("Edit Result Columns");
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        addColumn.click();
        arm.end();
        WebElement fancybox = Locator.findElementWithTimeout(By.id("fancybox-content"));
        WebElement listShuttle = fancybox.findElement(By.className("listShuttleTable"));
        listShuttle.findElement(
                By.xpath(".//td[@class=\"listShuttleSelectElements\"]//option[@value=\"contributors\"]")).click();
        listShuttle.findElement(By.xpath(
                ".//td[@class=\"listShuttleSelectionActions\"]/a[contains(@id, 'nxw_template_addToSelection')]"))
                   .click();
        arm.begin();
        Locator.findElementWaitUntilEnabledAndClick(fancybox, By.xpath(".//input[@value='Save']"));
        arm.end();

        // save this search
        String ssTitle = "Test Saved Search " + new Date().getTime();
        saveSearch(ssTitle);
        checkURL("simple_search");

        // get default search
        searchPage.getDefaultSearch();
        checkURL("default_search");
        // get saved search
        quickSearch = searchPage.getSearch(ssTitle, QuickSearchSubPage.class);
        checkURL("simple_search");
        resultSubPage = searchPage.getSearchResultsSubPage();

        assertEquals(ResultLayout.LISTING, resultSubPage.getContentView().getResultLayout());
        Assert.hasElement(By.xpath("//span[@class=\"colHeader\" && text()='Contributors'"));

        // delete saved searches
        deleteSavedSearches(documentBasePage);
        logout();

    }

    /**
     * Non-regression test for NXP-21937, NXP-22976 and NXP-23110 use cases.
     */
    @Test
    public void testSavedSearchSelection() throws UserNotConnectedException, IOException {
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

        // Test fulltext result and save it
        searchLayoutSubPage.getFullTextElement().setInputValue("ITSearchTabTest");
        searchLayoutSubPage.filter();
        String ssTitle1 = "TestSearch1 " + new Date().getTime();
        saveSearch(ssTitle1);

        searchLayoutSubPage = asPage(SearchPage.class).getDefaultSearch();
        searchLayoutSubPage.getFullTextElement().setInputValue("foo");
        searchLayoutSubPage.filter();
        String ssTitle2 = "TestSearch2 " + new Date().getTime();
        saveSearch(ssTitle2);

        // reproduce issue by logging out first and then switching between searches
        logout();
        searchPage = loginAsTestUser().goToSearchPage();
        // first saved search
        DefaultSearchSubPage saved1 = searchPage.getSearch(ssTitle1, DefaultSearchSubPage.class);
        assertEquals(ssTitle1, asPage(SearchPage.class).getSelectedSearch());
        assertEquals("ITSearchTabTest", saved1.getFullTextElement().getInputValue());
        SearchResultsSubPage resultSubPage1 = searchPage.getSearchResultsSubPage();
        assertEquals(ssTitle1, resultSubPage1.getSearchViewTitle());
        assertEquals(4, resultSubPage1.getNumberOfDocumentInCurrentPage());
        checkURL("default_search");
        // second saved search
        DefaultSearchSubPage saved2 = searchPage.getSearch(ssTitle2, DefaultSearchSubPage.class);
        assertEquals(ssTitle2, asPage(SearchPage.class).getSelectedSearch());
        assertEquals("foo", saved2.getFullTextElement().getInputValue());
        SearchResultsSubPage resultSubPage2 = searchPage.getSearchResultsSubPage();
        assertEquals(ssTitle2, resultSubPage2.getSearchViewTitle());
        assertEquals(0, resultSubPage2.getNumberOfDocumentInCurrentPage());
        checkURL("default_search");
        // switch again
        saved1 = searchPage.getSearch(ssTitle1, DefaultSearchSubPage.class);
        assertEquals(ssTitle1, asPage(SearchPage.class).getSelectedSearch());
        assertEquals("ITSearchTabTest", saved1.getFullTextElement().getInputValue());
        resultSubPage1 = searchPage.getSearchResultsSubPage();
        assertEquals(ssTitle1, resultSubPage1.getSearchViewTitle());
        assertEquals(4, resultSubPage1.getNumberOfDocumentInCurrentPage());
        checkURL("default_search");
        // NXP-23110: check clicking on main tab still displays same search
        asPage(SearchPage.class).goToSearchPage();
        assertEquals(ssTitle1, asPage(SearchPage.class).getSelectedSearch());
        assertEquals("ITSearchTabTest", saved1.getFullTextElement().getInputValue());
        resultSubPage1 = searchPage.getSearchResultsSubPage();
        assertEquals(ssTitle1, resultSubPage1.getSearchViewTitle());
        assertEquals(4, resultSubPage1.getNumberOfDocumentInCurrentPage());
        checkURL("default_search");

        deleteSavedSearches(asPage(SearchPage.class));
        logout();
    }

    /**
     * Non-regression test for NXP-22784 use case.
     */
    @Test
    public void testSearchWithUpdatedPermissionsOnRootDocument() throws UserNotConnectedException {

        String testuser = "testuser";
        String testgroup = "testgroup";

        RestHelper.createUser(testuser, testuser);
        RestHelper.createGroup(testgroup, testgroup, new String[] { testuser }, null);
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, "permissions");
        RestHelper.createDocument(WORKSPACES_PATH + "permissions", FILE_TYPE, "test");
        RestHelper.addPermission("/", testgroup, SecurityConstants.EVERYTHING);

        DocumentBasePage documentBasePage = login();
        SearchPage searchPage = documentBasePage.goToSearchPage();
        DefaultSearchSubPage searchLayoutSubPage = searchPage.getDefaultSearch();
        SearchResultsSubPage resultPanelSubPage = searchPage.getSearchResultsSubPage();
        searchLayoutSubPage.selectPath("/Domain/Workspaces/permissions");
        searchLayoutSubPage.filter();
        assertEquals(1, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        logout();

        searchPage = login(testuser, testuser).goToSearchPage();
        resultPanelSubPage = searchPage.getSearchResultsSubPage();
        searchLayoutSubPage = searchPage.getDefaultSearch();
        searchLayoutSubPage.selectPath("/Domain/Workspaces/permissions");
        searchLayoutSubPage.filter();
        assertEquals(1, resultPanelSubPage.getNumberOfDocumentInCurrentPage());
        logout();

        RestHelper.removePermissions("/", testgroup);
    }

    /**
     * Non-regression test for NXP-22976 use case.
     */
    @Test
    public void testSearchURLUpdateOnSelection() throws UserNotConnectedException {
        loginAsTestUser().goToSearchPage();
        checkURL("default_search");
        asPage(SearchPage.class).getQuickSearch();
        checkURL("simple_search");
        asPage(SearchPage.class).getNXQLSearch();
        checkURL("nxql_search");
        asPage(SearchPage.class).getDefaultSearch();
        checkURL("default_search");
        logout();
    }

}
