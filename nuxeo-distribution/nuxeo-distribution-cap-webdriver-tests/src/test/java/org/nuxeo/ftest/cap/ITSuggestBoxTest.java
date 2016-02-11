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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UserViewTabSubPage;
import org.nuxeo.functionaltests.pages.search.QuickSearchSubPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.nuxeo.functionaltests.pages.search.SearchResultsSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test of the suggestbox defined with a select2.
 *
 * @since 6.0
 */
public class ITSuggestBoxTest extends AbstractTest {

    /**
     * Use a workspace title with special characters (NXP-15618).
     */
    private final static String WORKSPACE_TITLE = "#W/o=rk^s+pa>ce'~Title%" + new Date().getTime();

    private static final String USER1_NAME = "user1";

    private static final String USER2_NAME = "user2";

    private static final String FILE_01_NAME = "Test01" + new Date().getTime();

    private static final String FILE_02_NAME = "Test02" + new Date().getTime();

    private static final String FILE_03_NAME = "Test03" + new Date().getTime();

    private static final String XPATH_SUGGESTBOX = "//*[@id='s2id_nxw_suggest_search_box_form:nxw_suggest_search_box_select2']";

    private static final String VALUE_WITH_SPECIALS_CHAR = "h\u00e9h\u00e9";

    @Before
    public void before() {
        RestHelper.createUser(USER1_NAME, USER1_NAME, USER1_NAME, "lastname1", "company1", "email1", "members");
        RestHelper.createUser(USER2_NAME, USER2_NAME, USER2_NAME, "lastname1", "company1", "email1", "members");
        String wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_TITLE,
                "Workspace for Webdriver test.");
        RestHelper.createDocument(wsId, FILE_TYPE, FILE_01_NAME, "Test File description");
        RestHelper.createDocument(wsId, FILE_TYPE, FILE_02_NAME, "Test File description");
        RestHelper.createDocument(wsId, FILE_TYPE, FILE_03_NAME, "Test File description");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void simpleSearchTest() throws Exception {
        // Test a simple search
        login();
        Select2WidgetElement searchElement = new Select2WidgetElement(driver,
                driver.findElement(By.xpath(XPATH_SUGGESTBOX)), true);
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
        searchElement = new Select2WidgetElement(driver, driver.findElement(By.xpath(XPATH_SUGGESTBOX)), true);
        listEntries = searchElement.typeAndGetResult(USER1_NAME);
        assertTrue(listEntries.size() == 2);
        listEntries.get(1).click();
        UserViewTabSubPage userPage = asPage(UserViewTabSubPage.class);
        userPage.checkUserName(USER1_NAME);

        logout();
    }

    @Test
    public void testSuggestBoxRedirect() throws Exception {
        login();
        Select2WidgetElement searchElement = new Select2WidgetElement(driver,
                driver.findElement(By.xpath(XPATH_SUGGESTBOX)), true);
        SearchPage searchPage = searchElement.typeValueAndTypeEnter("Administrator");
        assertTrue(searchPage.isQuickSearch());
        QuickSearchSubPage quickSearchPage = searchPage.getQuickSearch();
        SearchResultsSubPage searchResultsSubPage = searchPage.getSearchResultsSubPage();
        // Tests the results
        assertEquals("Administrator", quickSearchPage.textSearchElement.getAttribute("value"));
        assertEquals(SearchPage.QUICK_SEARCH, searchResultsSubPage.searchViewTitle.getText());
        List<WebElement> listResults = searchResultsSubPage.getListResults();
        assertTrue(listResults.size() > 0);
    }

    @Test
    public void requestEncodingTest() throws Exception {
        login();
        Select2WidgetElement searchElement = new Select2WidgetElement(driver,
                driver.findElement(By.xpath(XPATH_SUGGESTBOX)), true);
        SearchPage searchPage = searchElement.typeValueAndTypeEnter(VALUE_WITH_SPECIALS_CHAR);
        assertTrue(searchPage.isQuickSearch());
        assertEquals(VALUE_WITH_SPECIALS_CHAR, searchPage.getQuickSearch().textSearchElement.getAttribute("value"));
    }
}
