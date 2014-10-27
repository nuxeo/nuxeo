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
 * @since 5.9.6
 */
public class SearchPage extends DocumentBasePage {

    public static final String SEARCH_TAB = "SEARCH";

    public static final String DEFAULT_SEARCH = "Default search";

    public static final String NXQL_SEARCH = "NXQL Search";

    public static final String QUICK_SEARCH = "Quick search";

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
        if (!isDefaultSearch()) {
            selectSearch(DEFAULT_SEARCH);
        }
        return asPage(DefaultSearchSubPage.class);
    }

    public NXQLSearchSubPage getNXQLSearch() {
        if (!isNXQLSearch()) {
            selectSearch(NXQL_SEARCH);
        }
        return asPage(NXQLSearchSubPage.class);
    }

    public QuickSearchSubPage getQuickSearch() {
        if (!isQuickSearch()) {
            selectSearch(QUICK_SEARCH);
        }
        return asPage(QuickSearchSubPage.class);
    }

    public SearchResultsSubPage getSearchResultsSubPage() {
        return asPage(SearchResultsSubPage.class);
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

    public void selectSearch(String searchLabel) {
        Select2WidgetElement s2 = new Select2WidgetElement(driver,
                searchFormPanel.findElement(By.id(S2_SEARCH_TYPE_ID)));
        AjaxRequestManager am = new AjaxRequestManager(driver);
        am.watchAjaxRequests();
        s2.selectValue(searchLabel);
        am.waitForAjaxRequests();
    }

    public String getSelectedSearch() {
        Select2WidgetElement s2 = new Select2WidgetElement(driver,
                searchFormPanel.findElement(By.id(S2_SEARCH_TYPE_ID)));
        return s2.getSelectedValue().getText();
    }

    protected boolean isSearchSelected(final String searchType) {
        String selected = getSelectedSearch();
        return selected != null && selected.equals(searchType);
    }

}
