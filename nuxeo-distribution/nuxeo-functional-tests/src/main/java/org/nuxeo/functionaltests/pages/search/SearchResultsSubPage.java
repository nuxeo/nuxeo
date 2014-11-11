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

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.6
 */
public class SearchResultsSubPage extends AbstractPage {

    private static final String SEARCH_RESULTS_XPATH = "div[contains(@class,'bubbleBox')]";

    @FindBy(xpath = "//div[@id='nxw_searchContentView']/div/div/div/div/form")
    @Required
    protected WebElement resultForm;

    @Required
    @FindBy(xpath = "//div[@id='nxl_gridSearchLayout:nxw_searchResults_panel']/div/div/h3")
    public WebElement searchViewTitle;

    public SearchResultsSubPage(WebDriver driver) {
        super(driver);
    }

    public int getNumberOfDocumentInCurrentPage() {
        List<WebElement> result = resultForm.findElements(By.xpath(SEARCH_RESULTS_XPATH));
        return result.size();
    }

    /**
     * @return the list of results of the search.
     */
    public List<WebElement> getListResults() {
        try {
            return resultForm.findElements(By.xpath(SEARCH_RESULTS_XPATH));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
