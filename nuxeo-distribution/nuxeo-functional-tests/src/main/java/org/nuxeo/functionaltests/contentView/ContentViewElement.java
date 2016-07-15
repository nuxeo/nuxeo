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
 *     Nelson Silva
 */
package org.nuxeo.functionaltests.contentView;

import java.util.List;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Assert;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a content view element.
 *
 * @since 7.1
 */
public class ContentViewElement extends WebFragmentImpl {

    private static final String SELECT_ALL_BUTTON_XPATH = "//input[@type=\"checkbox\" and @title=\"Select all / deselect all\"]";

    private static final String CHECK_BOX_XPATH = "td/input[@type=\"checkbox\"]";

    public enum ResultLayout {
        THUMBNAIL("Thumbnail view"), LISTING("List view");

        private final String title;

        ResultLayout(String title) {
            this.title = title;
        }
    }

    public ContentViewElement(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    protected ContentViewElement reload(String id) {
        return getWebFragment(By.id(id), ContentViewElement.class);
    }

    protected String getContentViewId() {
        String id = getId();
        if (id.endsWith("_panel")) {
            return id.substring(0, id.length() - "_panel".length());
        }
        return id;
    }

    protected WebElement getResultsPanel() {
        String id = getContentViewId() + "_resultsPanel";
        return getElement().findElement(By.id(id));
    }

    public WebElement getActionByTitle(String title) {
        return getElement().findElement(By.className("contentViewUpperActions"))
                           .findElement(By.xpath("//img[@alt=\"" + title + "\"]"));
    }

    public ContentViewElement switchToResultLayout(ResultLayout layout) {
        // get id before element is detached from DOM (during next ajax call)
        String id = getId();
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        getActionByTitle(layout.title).click();
        a.waitForAjaxRequests();
        return reload(id);
    }

    /**
     * @since 8.3
     */
    public List<WebElement> getItems() {
        ResultLayout layout = getResultLayout();
        switch (layout) {
        case THUMBNAIL:
            return getResultsPanel().findElements(By.xpath(".//div[contains(@class,'bubbleBox')]"));
        case LISTING:
        default:
            return getResultsPanel().findElements(By.xpath("(.//form)[1]//tbody//tr"));
        }
    }

    /**
     * @since 8.3
     */
    public void clickOnItemTitle(String title) {
        Locator.findElementWaitUntilEnabledAndClick(getResultsPanel(), By.linkText(title));
    }

    /**
     * @since 8.3
     */
    public boolean hasItem(String title) {
        try {
            WebElement element = getResultsPanel().findElement(By.linkText(title));
            return element != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @since 8.3
     */
    public ContentViewElement checkByTitle(String... titles) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        List<WebElement> items = getItems();
        for (WebElement item : items) {
            for (String title : titles) {
                try {
                    item.findElement(By.linkText(title));
                    AjaxRequestManager arm = new AjaxRequestManager(driver);
                    arm.begin();
                    Locator.findElementWaitUntilEnabledAndClick(item, By.xpath(CHECK_BOX_XPATH));
                    arm.end();
                    break;
                } catch (NoSuchElementException e) {
                    // next
                }
            }
        }
        return reload(id);
    }

    /**
     * @since 8.3
     */
    public ContentViewElement checkByIndex(int... indexes) {
        // get id before element is detached from DOM (during next ajax call)
        String id = getId();
        AjaxRequestManager a = new AjaxRequestManager(driver);
        for (int i : indexes) {
            a.watchAjaxRequests();
            getItems().get(i).findElement(By.xpath(CHECK_BOX_XPATH)).click();
            a.waitForAjaxRequests();
        }
        return reload(id);
    }

    /**
     * @since 8.3
     */
    public ContentViewElement checkAllItems() {
        WebElement selectAll = null;
        try {
            selectAll = getResultsPanel().findElement(By.xpath(SELECT_ALL_BUTTON_XPATH));
        } catch (NoSuchElementException e) {
            // no item
        }
        if (selectAll != null) {
            // get id before element is detached from DOM (during next ajax call)
            String id = getId();
            AjaxRequestManager arm = new AjaxRequestManager(driver);
            arm.begin();
            Locator.scrollAndForceClick(selectAll);
            arm.end();
            return reload(id);
        }
        return this;
    }

    /**
     * @since 8.3
     */
    public WebElement getSelectionActionByTitle(String title) {
        return getResultsPanel().findElement(By.xpath("//div[contains(@id,'nxw_cvButton_panel')]"))
                                .findElement(By.xpath("//input[@value=\"" + title + "\"]"));
    }

    /**
     * @since 8.4
     */
    public ResultLayout getResultLayout() {
        WebElement element = getElement();
        WebElement resultsPanel = getResultsPanel();
        String resultLayoutSelected = ".//span[@class=\"resultLayoutSelection selected\"]/*/img[@alt=\"%s\"]";
        if (Assert.hasChild(element, By.xpath(String.format(resultLayoutSelected, ResultLayout.THUMBNAIL.title)))
                || Assert.hasChild(resultsPanel, By.xpath(".//div[contains(@class,'bubbleBox')]"))) {
            return ResultLayout.THUMBNAIL;
        } else if (Assert.hasChild(element, By.xpath(String.format(resultLayoutSelected, ResultLayout.LISTING.title)))
                || Assert.hasChild(resultsPanel, By.xpath(".//table[@class='dataOutput']"))) {
            return ResultLayout.LISTING;
        }
        throw new IllegalStateException("Content view is not listing nor thumbnail.");
    }

}
