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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.functionaltests.pages.search;

import java.util.Map;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.search.aggregates.CheckBoxAggregateElements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;

/**
 * @since 5.9.6
 */
public class DefaultSearchSubPage extends AbstractSearchSubPage {

    public static final String TREE_PATH_ID = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_ecm_path_treeId";

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_coverage_agg")
    protected WebElement coverageAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_created_agg")
    protected WebElement createdAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_modified_agg")
    protected WebElement modifiedAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_common_size_agg")
    protected WebElement sizeAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_subjects_agg")
    protected WebElement subjectsAggregate;

    @FindBy(id = "nxw_ecm_path_openPopup")
    @Required
    protected WebElement openPathPopupButton;

    @FindBy(id = "nxw_ecm_path")
    @Required
    protected WebElement selectPathDiv;

    public static final String PATH_REGEX = "(.*) \\((.*)\\)";

    protected static final String EXPAND_XPATH = "ancestor::div[@class='rf-trn']/span[contains(@class,'rf-trn-hnd')]";

    public DefaultSearchSubPage(WebDriver driver) {
        super(driver);
    }

    public Map<String, Integer> getAvailableCoverageAggregate() {
        return new CheckBoxAggregateElements(coverageAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableCreatedAggregate() {
        return new CheckBoxAggregateElements(createdAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableModifiedAggregate() {
        return new CheckBoxAggregateElements(modifiedAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableSizeAggregate() {
        return new CheckBoxAggregateElements(sizeAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableSubjectsAggregate() {
        return new CheckBoxAggregateElements(subjectsAggregate).getAggregates();
    }

    public WebElement getCoverageAggregate() {
        return coverageAggregate;
    }

    public WebElement getCreatedAggregate() {
        return createdAggregate;
    }

    public WebElement getModifiedAggregate() {
        return modifiedAggregate;
    }

    public WebElement getSizeAggregate() {
        return sizeAggregate;
    }

    public WebElement getSubjectsAggregate() {
        return subjectsAggregate;
    }

    public SearchPage selectCoverageAggregate(String label) {
        new CheckBoxAggregateElements(driver, coverageAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectCreatedAggregate(String label) {
        new CheckBoxAggregateElements(driver, createdAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectModifiedAggregate(String label) {
        new CheckBoxAggregateElements(driver, modifiedAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectSizeAggregate(String label) {
        new CheckBoxAggregateElements(driver, sizeAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectSubjectsAggregate(String label) {
        new CheckBoxAggregateElements(driver, subjectsAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public void selectPath(String path) {
        assert (path != null && !path.isEmpty() && path.charAt(0) == '/');
        openPathPopupButton.click();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement tree = driver.findElement(By.id(TREE_PATH_ID));
                    return tree.isDisplayed();
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
        });
        if (path.length() == 1) {
            driver.findElement(By.id(TREE_PATH_ID)).findElement(
                    By.linkText("/")).click();
            SearchPage.waitForLoading();
            return;
        } else {
            driver.findElement(By.id(TREE_PATH_ID)).findElement(
                    By.linkText("/")).findElement(
                    By.xpath(EXPAND_XPATH)).click();
        }
        SearchPage.waitForLoading();
        String[] pathArray = path.substring(1).split("/");
        int i = 0;
        for (; i < pathArray.length - 1; i++) {
            driver.findElement(By.id(TREE_PATH_ID)).findElement(
                    By.linkText(pathArray[i])).findElement(
                    By.xpath(EXPAND_XPATH)).click();
            SearchPage.waitForLoading();
        }
        driver.findElement(By.id(TREE_PATH_ID)).findElement(
                By.linkText(pathArray[i])).click();
        SearchPage.waitForLoading();
        driver.findElement(By.id("fancybox-close")).click();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement tree = driver.findElement(By.id(TREE_PATH_ID));
                    return !tree.isDisplayed();
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
        });
    }

    public void deselectPath(String path) {
        assert (path != null && !path.isEmpty());
        int lastPartIndex = path.lastIndexOf('/');
        String folderName = path.substring(lastPartIndex + 1);
        WebElement e = selectPathDiv.findElement(By.xpath("descendant::label[contains(text(),'"
                + folderName + "')]/ancestor::span[@class='sticker']/a"));
        e.click();
        SearchPage.waitForLoading();
    }
}
