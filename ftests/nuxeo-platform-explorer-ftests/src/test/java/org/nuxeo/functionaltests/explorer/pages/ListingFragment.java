/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.pages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;

/**
 * Represents a listing fragment on the explorer page.
 *
 * @since 11.1
 */
public class ListingFragment extends AbstractPage {

    @Required
    @FindBy(xpath = "//input[@class='searchFilter']")
    public WebElement searchFilter;

    @FindBy(xpath = "//input[@id='filter-submit-button']")
    public WebElement filterSubmit; // present only in some cases

    @Required
    @FindBy(id = "contentTable")
    public WebElement listingTable;

    @Required
    @FindBy(xpath = "//table[@id='contentTable']//th[contains(@class, 'header')]")
    public WebElement sort;

    public ListingFragment(WebDriver driver) {
        super(driver);
    }

    public WebElement getFirstItem() {
        return listingTable.findElement(By.xpath("./tbody//tr"));
    }

    public List<WebElement> getItems() {
        return listingTable.findElements(By.xpath("./tbody//tr"));
    }

    public WebElement getListingItemLink(WebElement item) {
        return item.findElement(By.xpath(".//a[@class='itemLink']"));
    }

    public String getListingItemDetail(WebElement item) {
        try {
            return item.findElement(By.xpath(".//div[@class='itemDetail']")).getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void navigateToFirstItem() {
        Locator.scrollAndForceClick(getListingItemLink(getFirstItem()));
    }

    class RequestManager {

        protected JavascriptExecutor js;

        protected String id;

        protected String event;

        public RequestManager(WebDriver driver, String id, String event) {
            this.js = (JavascriptExecutor) driver;
            this.id = id;
            this.event = event;
        }

        public void begin() {
            String beginCode = String.format(
                    "window.%s = true; jQuery('#contentTable').bind('%s', () => { window.%s = false; });", id, event,
                    id);
            js.executeScript(beginCode);
        }

        public void waitForEnd() {
            Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
                @Override
                public Boolean apply(WebDriver driver) {
                    String endCode = String.format("return window.%s == false;", id);
                    Boolean res = (Boolean) ((JavascriptExecutor) driver).executeScript(endCode);
                    return res;
                }
            });
        }

    }

    public ListingFragment filterOn(String filterText) {
        // check first if fulltext search, requiring explicit submit
        if ("fulltext-box".contentEquals(searchFilter.getAttribute("id"))) {
            searchFilter.sendKeys(filterText);
            Locator.scrollAndForceClick(filterSubmit);
        } else {
            RequestManager rm = new RequestManager(driver, "nxexplorerFilterOngoing", "filterEnd");
            rm.begin();
            searchFilter.sendKeys(filterText);
            rm.waitForEnd();
        }
        return asPage(ListingFragment.class);
    }

    public ListingFragment toggleSort() {
        RequestManager rm = new RequestManager(driver, "nxexplorerSortOngoing", "sortEnd");
        rm.begin();
        Locator.scrollAndForceClick(sort);
        rm.waitForEnd();
        return asPage(ListingFragment.class);
    }

    public void checkListing(int expectedSize, String firstLinkText, String firstLinkURLEnd, String firstLinkDetail) {
        if (expectedSize >= 0) {
            assertEquals(expectedSize, getItems().size());
        } else {
            assertTrue(getItems().size() > 0);
        }
        WebElement item = getFirstItem();
        WebElement link = getListingItemLink(item);
        assertEquals(firstLinkText, link.getText());
        assertTrue(link.getAttribute("href"), link.getAttribute("href").endsWith(firstLinkURLEnd));
        assertEquals(firstLinkDetail, getListingItemDetail(item));
    }

}
