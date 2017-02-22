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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.Function;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Assert;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
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

    /**
     * @since 9.1
     */
    public ContentViewUpperActions getUpperActions() {
        return AbstractTest.getWebFragment(By.className("contentViewUpperActions"), ContentViewUpperActions.class);
    }

    /**
     * @since 9.1
     */
    public ContentViewSelectionActions getSelectionActions() {
        By buttonsId = By.id(String.format("%s_buttons:ajax_selection_buttons", getContentViewType()));
        Locator.waitUntilElementPresent(buttonsId);
        return AbstractTest.getWebFragment(buttonsId, ContentViewSelectionActions.class);
    }

    public PageNavigationControls getPaginationControls() {
        return AbstractTest.getWebFragment(findElement(By.className("pageNavigationControls")),
                PageNavigationControls.class);
    }

    /**
     * @since 9.1
     */
    protected WebElement getFilterInput() {
        String id = getContentViewId() + "_quickFilterForm:nxl_document_content_filter:nxw_search_title";
        return findElement(By.id(id));
    }

    /**
     * @since 9.1
     */
    protected WebElement getFilterButton() {
        String id = getContentViewId() + "_quickFilterForm:submitFilter";
        return findElement(By.id(id));
    }

    /**
     * @since 9.1
     */
    protected WebElement getClearFilterButton() {
        String id = getContentViewId() + "_resetFilterForm:resetFilter";
        return findElement(By.id(id));
    }

    protected String getContentViewId() {
        String id = getId();
        if (id.endsWith("_panel")) {
            return id.substring(0, id.length() - "_panel".length());
        }
        return id;
    }

    /**
     * @return the content view id with cv_* and _\d* removed. This is useful to get the selection actions.
     */
    protected String getContentViewType() {
        return getContentViewId().replaceFirst("^cv_", "").replaceFirst("_\\d*$", "");
    }

    protected WebElement getResultsPanel() {
        String id = getContentViewId() + "_resultsPanel";
        return getElement().findElement(By.id(id));
    }

    public WebElement getActionByTitle(String title) {
        return getUpperActions().getActionByTitle(title);
    }

    public ContentViewElement switchToResultLayout(ResultLayout layout) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        getUpperActions().clickOnActionByTitle(layout.title);
        return reload(id);
    }

    /**
     * Use this method to do navigation actions with reloading of this {@link ContentViewElement}.
     *
     * @return the new content view element
     * @since 9.1
     */
    public <T extends DocumentBasePage> ContentViewElement navigation(Function<PageNavigationControls, T> nav) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        nav.apply(getPaginationControls());
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
     * @since 9.1
     */
    public WebElement getItemWithTitleAndVersion(String title, String version) {
        for (WebElement item : getItems()) {
            String t = item.findElement(By.xpath("td[3]")).getText(); // title
            String v = item.findElement(By.xpath("td[7]")).getText(); // version
            if (t.equals(title) && v.equals(version)) {
                return item;
            }
        }
        throw new NoSuchElementException("No item with title \"" + title + "\" and version " + version);
    }

    /**
     * @since 8.3
     */
    public void clickOnItemTitle(String title) {
        Locator.findElementWaitUntilEnabledAndClick(getResultsPanel(), By.linkText(title));
    }

    /**
     * @since 9.1
     */
    public void clickOnItemTitleAndVersion(String title, String version) {
        if (getResultLayout() == ResultLayout.THUMBNAIL) {
            throw new NoSuchElementException("Click on title and version doesn\"t work with thumbnails.");
        }
        WebElement item = getItemWithTitleAndVersion(title, version);
        Locator.findElementWaitUntilEnabledAndClick(item, By.linkText(title));
    }

    /**
     * @since 9.1
     */
    public void clickOnItemIndex(int index) {
        if (getResultLayout() == ResultLayout.THUMBNAIL) {
            throw new NoSuchElementException("Click on index doesn\"t work with thumbnails.");
        }
        Locator.waitUntilEnabledAndClick(getItems().get(index).findElement(By.xpath("td[3]/div/a[1]")));
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
     * Perform filter on the given string.
     *
     * @param filter the string to filter
     * @since 9.1
     */
    public ContentViewElement filterDocument(String filter) {
        WebElement filterInput = getFilterInput();
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        filterInput.clear();
        filterInput.sendKeys(filter);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        getFilterButton().click();
        arm.end();
        Locator.waitUntilElementPresent(By.id(getClearFilterButton().getAttribute("id")));
        return reload(id);
    }

    /**
     * Clear the current filter and refresh content view.
     *
     * @since 9.1
     */
    public ContentViewElement clearFilter() {
        WebElement clearFilterButton = getClearFilterButton();
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        String clearFilterButtonId = clearFilterButton.getAttribute("id");
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(clearFilterButton);
        arm.end();
        Locator.waitUntilElementNotPresent(By.id(clearFilterButtonId));
        return reload(id);
    }

    /**
     * @since 8.3
     * @deprecated since 9.1 use {@link #selectByTitle(String...)} instead.
     */
    @Deprecated
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
     * @since 9.1
     */
    public ContentViewSelectionActions selectByTitle(String... titles) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        List<WebElement> items = getItems();
        for (WebElement item : items) {
            for (String title : titles) {
                try {
                    item.findElement(By.linkText(title));
                    AjaxRequestManager arm = new AjaxRequestManager(driver);
                    arm.begin();
                    WebElement element = item.findElement(By.xpath(CHECK_BOX_XPATH));
                    assertFalse("Element with title=" + title + " is already selected", element.isSelected());
                    Locator.scrollAndForceClick(element);
                    arm.end();
                    break;
                } catch (NoSuchElementException e) {
                    // next
                }
            }
        }
        return reload(id).getSelectionActions();
    }

    /**
     * @since 9.1
     */
    public ContentViewElement unselectByTitle(String... titles) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        List<WebElement> items = getItems();
        for (WebElement item : items) {
            for (String title : titles) {
                try {
                    item.findElement(By.linkText(title));
                    AjaxRequestManager arm = new AjaxRequestManager(driver);
                    arm.begin();
                    WebElement element = item.findElement(By.xpath(CHECK_BOX_XPATH));
                    assertTrue("Element with title=" + title + " is not selected", element.isSelected());
                    Locator.scrollAndForceClick(element);
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
     * @deprecated since 9.1 use {@link #selectByIndex(int...)}/{@link #unselectByIndex(int...)} instead.
     */
    @Deprecated
    public ContentViewElement checkByIndex(int... indexes) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        for (int i : indexes) {
            arm.watchAjaxRequests();
            getItems().get(i).findElement(By.xpath(CHECK_BOX_XPATH)).click();
            arm.waitForAjaxRequests();
        }
        return reload(id);
    }

    /**
     * @since 9.1
     */
    public ContentViewSelectionActions selectByIndex(int... indexes) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        for (int i : indexes) {
            arm.watchAjaxRequests();
            WebElement element = getItems().get(i).findElement(By.xpath(CHECK_BOX_XPATH));
            assertFalse("Element with id=" + i + " is already selected", element.isSelected());
            Locator.scrollAndForceClick(element);
            arm.waitForAjaxRequests();
        }
        return reload(id).getSelectionActions();
    }

    /**
     * @since 9.1
     */
    public ContentViewElement unselectByIndex(int... indexes) {
        // get id before element is detached from DOM (during next ajax calls)
        String id = getId();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        for (int i : indexes) {
            arm.watchAjaxRequests();
            WebElement element = getItems().get(i).findElement(By.xpath(CHECK_BOX_XPATH));
            assertTrue("Element with id=" + i + " is not selected", element.isSelected());
            Locator.scrollAndForceClick(element);
            arm.waitForAjaxRequests();
        }
        return reload(id);
    }

    /**
     * @since 8.3
     * @deprecated since 9.1 use {@link #selectAll()}/{@link #unselectAll()} instead.
     */
    @Deprecated
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
     * @since 9.1
     */
    public ContentViewSelectionActions selectAll() {
        WebElement selectAll = getResultsPanel().findElement(By.xpath(SELECT_ALL_BUTTON_XPATH));
        assertFalse("Select all Element is already selected", selectAll.isSelected());
        // get id before element is detached from DOM (during next ajax call)
        String id = getId();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.scrollAndForceClick(selectAll);
        arm.end();
        return reload(id).getSelectionActions();
    }

    /**
     * CAUTION You can call this method only after a {@link #selectAll()}.
     * 
     * @since 9.1
     */
    public ContentViewElement unselectAll() {
        WebElement selectAll = getResultsPanel().findElement(By.xpath(SELECT_ALL_BUTTON_XPATH));
        assertTrue("Select all Element is not selected", selectAll.isSelected());
        // get id before element is detached from DOM (during next ajax call)
        String id = getId();
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.scrollAndForceClick(selectAll);
        arm.end();
        return reload(id);
    }

    /**
     * @since 8.3
     * @deprecated since 9.1 use {@link #getUpperActions()} then
     *             {@link ContentViewUpperActions#getActionByTitle(String)} instead. Or use select methods directly.
     */
    @Deprecated
    public WebElement getSelectionActionByTitle(String title) {
        return getUpperActions().getActionByTitle(title);
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
