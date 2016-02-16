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

import static org.nuxeo.ftest.cap.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;
import static org.nuxeo.functionaltests.RestHelper.createDocument;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.HomePage;
import org.nuxeo.functionaltests.pages.search.DefaultSearchSubPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.nuxeo.functionaltests.pages.search.SearchResultsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 6.0
 */
public class ITSearchTabTest extends AbstractTest {

    private final static String WORKSPACE1_TITLE = ITSearchTabTest.class.getSimpleName() + "_WorkspaceTitle1_"
            + new Date().getTime();

    private final static String WORKSPACE2_TITLE = ITSearchTabTest.class.getSimpleName() + "_WorkspaceTitle2_"
            + new Date().getTime();

    private static final String SEARCH_PATH = "/Domain/Workspaces/" + WORKSPACE1_TITLE;

    private static final String MY_FAVORITES_COLLECTION = "My Favorites";

    public final static String[] SUBJECTS = { "Comics", "Religion", "Education" };

    public final static String[] FULL_PATH_SUBJECTS = { "Art/Comics", "Society/Religion", "Society/Education" };

    public final static String COVERAGE = "France";

    public final static String FULL_PATH_COVERAGE = "Europe/France";

    protected static final String SELECT_ALL_SAVED_SEARCHES_BUTTON_ID = "all_saved_searches:all_saved_searches_repeat:0:nxl_document_listing_table_1:listing_ajax_selection_box_with_current_document_header";

    protected static final String PERMANENT_DELETE_SAVED_SEARCHES_BUTTON_ID = "all_saved_searches_buttons:nxw_savedSearchesCurrentSelectionDelete_form:nxw_savedSearchesCurrentSelectionDelete";

    @Before
    public void setup() throws UserNotConnectedException, IOException {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_USERNAME, "lastname1", "company1", "email1",
                "members");
        String wsId = createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE1_TITLE, null);
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE2_TITLE, null);
        RestHelper.addPermission(wsId, TEST_USERNAME, "Everything");

        loginAsTestUser();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        FileDocumentBasePage filePage = createFile(asPage(DocumentBasePage.class), "Test file for ITSearchTabTest",
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

        // test aggregate on deleted user, on user workspace
        DocumentBasePage documentBasePage = login();
        SearchPage searchPage = documentBasePage.goToSearchPage();
        DefaultSearchSubPage searchLayoutSubPage = searchPage.getDefaultSearch();
        Map<String, Integer> authorAggs = searchLayoutSubPage.getAvailableAuthorAggregate();
        boolean testUserFound = false;
        for (Entry<String, Integer> e : authorAggs.entrySet()) {
            if (e.getKey().equals(TEST_USERNAME)) {
                testUserFound = true;
                break;
            }
        }
        assertTrue(testUserFound);

        RestHelper.cleanupDocuments();
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

        // delete saved searches
        documentBasePage.goToHomePage().goToSavedSearches();
        arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.findElementWaitUntilEnabledAndClick(By.id(SELECT_ALL_SAVED_SEARCHES_BUTTON_ID));
        arm.end();

        Locator.findElementWaitUntilEnabledAndClick(By.id(PERMANENT_DELETE_SAVED_SEARCHES_BUTTON_ID));
        driver.switchTo().alert().accept();

        logout();
    }

}
