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

    public static enum ResultLayout {
        THUMBNAIL("Thumbnail view"), LISTING("List view");

        private final String title;

        ResultLayout(String title) {
            this.title = title;
        }
    }

    public ContentViewElement(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    protected ContentViewElement reload() {
        return getWebFragment(By.id(getId()), ContentViewElement.class);
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
        AjaxRequestManager a = new AjaxRequestManager(driver);
        a.watchAjaxRequests();
        getActionByTitle(layout.title).click();
        a.waitForAjaxRequests();
        return reload();
    }

    /**
     * @since 8.2
     */
    public List<WebElement> getItems() {
        return getResultsPanel().findElements(By.xpath("(.//form)[1]//tbody//tr"));
    }

    /**
     * @since 8.2
     */
    public void clickOnItemTitle(String title) {
        getResultsPanel().findElement(By.linkText(title)).click();
    }

    /**
     * @since 8.2
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
     * @since 8.2
     */
    public ContentViewElement checkByTitle(String... titles) {
        List<WebElement> items = getItems();
        for (WebElement item : items) {
            for (String title : titles) {
                try {
                    item.findElement(By.linkText(title));
                    WebElement checkBox = item.findElement(By.xpath(CHECK_BOX_XPATH));
                    AjaxRequestManager arm = new AjaxRequestManager(driver);
                    arm.begin();
                    checkBox.click();
                    arm.end();
                    break;
                } catch (NoSuchElementException e) {
                    // next
                }
            }
        }
        return reload();
    }

    /**
     * @since 8.2
     */
    public ContentViewElement checkByIndex(int... indexes) {
        AjaxRequestManager a = new AjaxRequestManager(driver);
        for (int i : indexes) {
            a.watchAjaxRequests();
            getItems().get(i).findElement(By.xpath(CHECK_BOX_XPATH)).click();
            a.waitForAjaxRequests();
        }
        return reload();
    }

    /**
     * @since 8.2
     */
    public ContentViewElement checkAllItems() {
        WebElement selectAll = null;
        try {
            selectAll = getResultsPanel().findElement(By.xpath(SELECT_ALL_BUTTON_XPATH));
        } catch (NoSuchElementException e) {
            // no item
        }
        if (selectAll != null) {
            AjaxRequestManager arm = new AjaxRequestManager(driver);
            arm.begin();
            selectAll.click();
            arm.end();
            return reload();
        }
        return this;
    }

    /**
     * @since 8.2
     */
    public WebElement getSelectionActionByTitle(String title) {
        return getResultsPanel().findElement(By.xpath("//div[contains(@id,'nxw_cvButton_panel')]"))
                                .findElement(By.xpath("//input[@value=\"" + title + "\"]"));
    }

}
