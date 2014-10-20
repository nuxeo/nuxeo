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
package org.nuxeo.functionaltests.pages;

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Sub page containing the results of a search.
 *
 * @since 5.9.6
 */
public class SearchResultsSubPage extends AbstractPage {

    private static final String SEARCH_RESULTS_XPATH = "//div[@class='bubbleBox bubbleListing ']";

    @Required
    @FindBy(xpath = "//div[@id='nxl_gridSearchLayout:nxw_searchResults_panel']/div/div/h3")
    public WebElement searchViewTitle;

    public SearchResultsSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * @return the list of results of the search.
     */
    public List<WebElement> getListResults() {
        try {
            return driver.findElements(By.xpath(SEARCH_RESULTS_XPATH));
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
