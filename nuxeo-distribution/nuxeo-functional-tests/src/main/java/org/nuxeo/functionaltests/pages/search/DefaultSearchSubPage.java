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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.functionaltests.pages.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.search.aggregates.CheckBoxAggregateElements;
import org.nuxeo.functionaltests.pages.search.aggregates.Select2AggregateElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;

/**
 * @since 6.0
 */
public class DefaultSearchSubPage extends AbstractSearchSubPage {

    private static final String S2_COLLECTION_XPATH = "//*[@id='s2id_nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_visible_collection_select2']";

    public static final String TREE_PATH_ID = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_ecm_path_treeId";

    @FindBy(id = "s2id_nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_creator_agg")
    protected WebElement authorAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_coverage_agg")
    protected WebElement coverageAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_created_agg")
    protected WebElement createdAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_modified_agg")
    protected WebElement modifiedAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_content_length_agg")
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

    /**
     * @since 7.4
     */
    public Map<String, Integer> getAvailableAuthorAggregate() {
        Select2AggregateElement s2AuthorAggregate = new Select2AggregateElement(driver, authorAggregate, true);
        return s2AuthorAggregate.getAggregates();
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
        assert(path != null && !path.isEmpty() && path.charAt(0) == '/');
        openPathPopupButton.click();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            @Override
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
            AjaxRequestManager a = new AjaxRequestManager(driver);
            a.watchAjaxRequests();
            driver.findElement(By.id(TREE_PATH_ID)).findElement(By.linkText("/")).click();
            a.waitForAjaxRequests();
            return;
        } else {
            AjaxRequestManager a = new AjaxRequestManager(driver);
            a.watchAjaxRequests();
            driver.findElement(By.id(TREE_PATH_ID))
                  .findElement(By.linkText("/"))
                  .findElement(By.xpath(EXPAND_XPATH))
                  .click();
            a.waitForAjaxRequests();
        }
        String[] pathArray = path.substring(1).split("/");
        int i = 0;
        for (; i < pathArray.length - 1; i++) {
            AjaxRequestManager a = new AjaxRequestManager(driver);
            a.watchAjaxRequests();
            driver.findElement(By.id(TREE_PATH_ID))
                  .findElement(By.linkText(pathArray[i]))
                  .findElement(By.xpath(EXPAND_XPATH))
                  .click();
            a.waitForAjaxRequests();
        }
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        driver.findElement(By.id(TREE_PATH_ID)).findElement(By.linkText(pathArray[i])).click();
        a.waitForAjaxRequests();
        AbstractPage.closeFancyBox();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement btn = driver.findElement(By.id("fancybox-overlay"));
                    return !btn.isDisplayed() || !btn.isEnabled();
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
        });
    }

    public void deselectPath(String path) {
        assert(path != null && !path.isEmpty());
        int lastPartIndex = path.lastIndexOf('/');
        String folderName = path.substring(lastPartIndex + 1);
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        findElementWaitUntilEnabledAndClick(selectPathDiv, By.xpath(
                "descendant::label[contains(text(),'" + folderName + "')]/ancestor::span[@class='sticker']/a"));
        a.waitForAjaxRequests();
    }

    /**
     * @since 7.3
     */
    public void selectCollections(final String[] collections) {
        Select2WidgetElement collectionsWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath(S2_COLLECTION_XPATH)), true);
        collectionsWidget.selectValues(collections);
    }

    /**
     * @since 7.3
     */
    public List<String> getSelectedCollections() {
        Select2WidgetElement collectionsWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath(S2_COLLECTION_XPATH)), true);
        List<String> result = new ArrayList<String>();
        for (WebElement el : collectionsWidget.getSelectedValues()) {
            result.add(el.getText());
        }
        return result;
    }
}
