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

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 6.0
 */
public class SearchResultsSubPage extends AbstractPage {

    private static final String SEARCH_RESULTS_XPATH = "//div[contains(@class,'bubbleBox')]";

    private static final String CONTENT_VIEW_XPATH = "//div[@id='nxw_searchContentView']//div[contains(@id, 'nxw_searchContentView_panel')]";

    @Required
    @FindBy(xpath = "//div[@id='nxw_searchContentView']//div[contains(@id, 'nxw_searchContentView_resultsPanel')]/form")
    protected WebElement resultForm;

    @Required
    @FindBy(xpath = "//div[@id='nxl_gridSearchLayout:nxw_searchResults_panel']/div/div/h3")
    public WebElement searchViewTitle;

    public SearchResultsSubPage(WebDriver driver) {
        super(driver);
    }

    public int getNumberOfDocumentInCurrentPage() {
        return getContentView().getItems().size();
    }

    /**
     * @return the list of results of the search.
     */
    public List<WebElement> getListResults() {
        try {
            return getContentView().getItems();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * @return the content view of results
     * @since 8.4
     */
    public ContentViewElement getContentView() {
        return AbstractTest.getWebFragment(By.xpath(CONTENT_VIEW_XPATH), ContentViewElement.class);
    }

}
