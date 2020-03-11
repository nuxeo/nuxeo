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
package org.nuxeo.functionaltests.pages.search;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 6.0
 */
public class SearchPage extends DocumentBasePage {

    public static final String SEARCH_TAB = "SEARCH";

    public static final String DEFAULT_SEARCH = "Faceted Search";

    public static final String NXQL_SEARCH = "NXQL Search";

    public static final String QUICK_SEARCH = "Quick Search";

    private static final String S2_SEARCH_TYPE_ID = "s2id_nxl_gridSearchLayout:nxw_searchesSelector_form:nxw_searchesSelector";

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchForm_panel")
    @Required
    protected WebElement searchFormPanel;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchResults_panel")
    @Required
    protected WebElement searchResultPanel;

    public SearchPage(WebDriver driver) {
        super(driver);
    }

    public DefaultSearchSubPage getDefaultSearch() {
        return getSearch(DEFAULT_SEARCH, DefaultSearchSubPage.class);
    }

    public NXQLSearchSubPage getNXQLSearch() {
        return getSearch(NXQL_SEARCH, NXQLSearchSubPage.class);
    }

    public QuickSearchSubPage getQuickSearch() {
        return getSearch(QUICK_SEARCH, QuickSearchSubPage.class);
    }

    /**
     * @since 8.4
     */
    public <T extends AbstractSearchSubPage> T getSearch(String searchLabel, Class<T> clazz) {
        if (!isSearchSelected(searchLabel)) {
            selectSearch(searchLabel);
        }
        return asPage(clazz);
    }

    public SearchResultsSubPage getSearchResultsSubPage() {
        return asPage(SearchResultsSubPage.class);
    }

    public String getSelectedSearch() {
        Select2WidgetElement s2 = new Select2WidgetElement(driver,
                searchFormPanel.findElement(By.id(S2_SEARCH_TYPE_ID)));
        return s2.getSelectedValue().getText();
    }

    public boolean isDefaultSearch() {
        return isSearchSelected(DEFAULT_SEARCH);
    }

    public boolean isNXQLSearch() {
        return isSearchSelected(NXQL_SEARCH);
    }

    public boolean isQuickSearch() {
        return isSearchSelected(QUICK_SEARCH);
    }

    protected boolean isSearchSelected(final String searchType) {
        String selected = getSelectedSearch();
        return selected != null && selected.equals(searchType);
    }

    public void selectSearch(String searchLabel) {
        Select2WidgetElement s2 = new Select2WidgetElement(driver,
                searchFormPanel.findElement(By.id(S2_SEARCH_TYPE_ID)));
        AjaxRequestManager am = new AjaxRequestManager(driver);
        am.watchAjaxRequests();
        s2.selectValue(searchLabel);
        am.waitForAjaxRequests();
    }

}
