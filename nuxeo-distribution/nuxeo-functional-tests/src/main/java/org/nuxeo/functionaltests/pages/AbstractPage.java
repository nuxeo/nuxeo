/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Assert;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;

/**
 * Base functions for all pages.
 */
public abstract class AbstractPage {

    @FindBy(xpath = "//div[@id='nxw_userMenuActions_panel']/ul/li/span")
    public WebElement userServicesForm;

    protected WebDriver driver;

    public AbstractPage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Returns true if corresponding element is found in the test page.
     *
     * @since 5.7
     */
    public boolean hasElement(By by) {
        return Assert.hasElement(by);
    }

    public <T> T get(String url, Class<T> pageClassToProxy) {
        return AbstractTest.get(url, pageClassToProxy);
    }

    public <T> T asPage(Class<T> pageClassToProxy) {
        return AbstractTest.asPage(pageClassToProxy);
    }

    public <T extends WebFragment> T getWebFragment(By by, Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(by, webFragmentClass);
    }

    public <T extends WebFragment> T getWebFragment(WebElement element, Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(element, webFragmentClass);
    }

    /**
     * Returns the error feedback message.
     * <p>
     * If there are more than one error message, always return the last one (not interested by 'Please correct errors'
     * message).
     *
     * @since 5.8
     */
    public String getErrorFeedbackMessage() {
        return getFeedbackMessage("errorFeedback");
    }

    /**
     * Returns the info feedback message.
     * <p>
     * If there are more than one info message, always return the last one.
     *
     * @since 8.3
     */
    public String getInfoFeedbackMessage() {
        return getFeedbackMessage("infoFeedback");
    }

    protected String getFeedbackMessage(String styleClass) {
        String ret = "";
        try {
            List<WebElement> elements = findElementsWithTimeout(
                    By.xpath("//div[contains(@class, '" + styleClass + "')]/div[@class='ambiance-title']"));
            if (elements.size() == 1) {
                ret = elements.get(0).getText();
            } else if (elements.size() > 1) {
                ret = elements.get(elements.size() - 1).getText();
            }
        } catch (NoSuchElementException e) {
            ret = "";
        }
        return ret.trim();
    }

    /**
     * Gets the top bar navigation sub page.
     */
    public HeaderLinksSubPage getHeaderLinks() {
        assertNotNull(userServicesForm);
        return asPage(HeaderLinksSubPage.class);
    }

    /**
     * Returns the fancy box content web element.
     *
     * @since 5.7
     */
    public static WebElement getFancyBoxContent() {
        // make sure the fancybox content is loaded
        WebElement fancyBox = findElementWithTimeout(By.id("fancybox-content"));
        FluentWait<WebDriver> wait = Locator.getFluentWait();
        wait.until(ExpectedConditions.visibilityOf(fancyBox));
        return fancyBox;
    }

    /**
     * Closes current fancy box.
     *
     * @since 8.3
     */
    public static void closeFancyBox() {
        AjaxRequestManager arm = new AjaxRequestManager(AbstractTest.driver);
        arm.begin();
        findElementWaitUntilEnabledAndClick(By.id("fancybox-close"));
        arm.end();
        waitForFancyBoxClosed();
    }

    /**
     * Waits for the fancybox to be fully closed.
     *
     * @since 8.3
     */
    public static void waitForFancyBoxClosed() {
        // make sure the fancybox content is not loaded anymore
        FluentWait<WebDriver> wait = Locator.getFluentWait();
        wait.withTimeout(AbstractTest.AJAX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fancybox-overlay")));
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public WebElement findElementWithTimeout(By by, int timeout) throws NoSuchElementException {
        return Locator.findElementWithTimeout(by, timeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @param parentElement find from the element
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public WebElement findElementWithTimeout(By by, int timeout, WebElement parentElement)
            throws NoSuchElementException {
        return Locator.findElementWithTimeout(by, timeout, parentElement);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a timeout.
     *
     * @param by the locating mechanism
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by) throws NoSuchElementException {
        return Locator.findElementWithTimeout(by);
    }

    /**
     * Finds webelement list using the given method, with a timeout
     */
    public static List<WebElement> findElementsWithTimeout(By by) throws NoSuchElementException {
        return Locator.findElementsWithTimeout(by);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a timeout.
     *
     * @param by the locating mechanism
     * @param parentElement find from the element
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by, WebElement parentElement) throws NoSuchElementException {
        return Locator.findElementWithTimeout(by, parentElement);
    }

    /**
     * Waits until an element is enabled, with a default timeout.
     *
     * @param element the element
     */
    public static void waitUntilEnabled(WebElement element) throws NotFoundException {
        Locator.waitUntilEnabled(element);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     * @param waitUntilEnabledTimeout the timeout in milliseconds
     * @since 8.3
     */
    public static void waitUntilEnabled(WebElement element, int waitUntilEnabledTimeout) throws NotFoundException {
        Locator.waitUntilEnabled(element, waitUntilEnabledTimeout);
    }

    /**
     * Waits until an element is enabled, with a default timeout. Then clicks on the element.
     *
     * @param element the element
     * @since 8.3
     */
    public static void waitUntilEnabledAndClick(WebElement element) throws NotFoundException {
        Locator.waitUntilEnabledAndClick(element);
    }

    /**
     * Waits until an element is enabled, with a timeout. Then clicks on the element.
     *
     * @param element the element
     * @param waitUntilEnabledTimeout the timeout in milliseconds
     * @since 8.3
     */

    public static void waitUntilEnabledAndClick(WebElement element, int waitUntilEnabledTimeout)
            throws NotFoundException {
        Locator.waitUntilEnabledAndClick(element, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a {@code findElementTimeout}. Then waits until
     * the element is enabled, with a {@code waitUntilEnabledTimeout}.
     *
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static WebElement findElementAndWaitUntilEnabled(By by, int findElementTimeout, int waitUntilEnabledTimeout)
            throws NotFoundException {
        return Locator.findElementAndWaitUntilEnabled(by, findElementTimeout, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the default timeout. Then waits until the element
     * is enabled, with the default timeout.
     *
     * @param by the locating mechanism
     * @return the first matching element on the current page, if found
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static WebElement findElementAndWaitUntilEnabled(By by) throws NotFoundException {
        return Locator.findElementAndWaitUntilEnabled(by);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a {@code findElementTimeout}. Then waits until
     * the element is enabled, with a {@code waitUntilEnabledTimeout}. Then clicks on the element.
     *
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @throws NotFoundException if the element is not found or not enabled
     * @deprecated since 8.3, use {@link Locator#findElementWaitUntilEnabledAndClick(WebElement, By, int, int)}
     */
    @Deprecated
    public static void findElementWaitUntilEnabledAndClick(By by, int findElementTimeout, int waitUntilEnabledTimeout)
            throws NotFoundException {
        Locator.findElementWaitUntilEnabledAndClick(by, findElementTimeout, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the default timeout. Then waits until the element
     * is enabled, with the default timeout. Then clicks on the element.
     *
     * @param by the locating mechanism
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static void findElementWaitUntilEnabledAndClick(By by) throws NotFoundException {
        Locator.findElementWaitUntilEnabledAndClick(by);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the default timeout, inside an optional
     * {@code parentElement}. Then waits until the element is enabled, with the default timeout. Then clicks on the
     * element.
     *
     * @param parentElement the parent element (can be null)
     * @param by the locating mechanism
     * @throws NotFoundException if the element is not found or not enabled
     * @since 9.1
     */
    public static void findElementWaitUntilEnabledAndClick(WebElement parentElement, By by) throws NotFoundException {
        Locator.findElementWaitUntilEnabledAndClick(parentElement, by);
    }

    /**
     * Waits until the URL is different from the one given in parameter, with a timeout.
     *
     * @param url the URL to compare to
     */
    public void waitUntilURLDifferentFrom(String url) {
        Locator.waitUntilURLDifferentFrom(url);
    }

    /**
     * Selects item in drop down menu.
     *
     * @since 5.7
     */
    public void selectItemInDropDownMenu(WebElement selector, String optionLabel) {
        Select select = new Select(selector);
        select.selectByVisibleText(optionLabel);
    }

    /**
     * Switch to given frame id.
     *
     * @since 5.7.3
     */
    public WebDriver switchToFrame(String id) {
        driver.switchTo().defaultContent();
        // you are now outside both frames
        return driver.switchTo().frame(id);
    }

    /**
     * Helper method to adapt tests behaviour when ajaxifying tabs.
     *
     * @since 7.10
     */
    public boolean useAjaxTabs() {
        return true;
    }

    protected void clickOnTabIfNotSelected(String tabPanelId, WebElement tabElement) {
        clickOnTabIfNotSelected(tabPanelId, tabElement, useAjaxTabs());
    }

    protected void clickOnTabIfNotSelected(String tabPanelId, WebElement tabElement, boolean useAjax) {
        WebElement selectedTab = findElementWithTimeout(
                By.xpath("//div[@id='" + tabPanelId + "']//li[@class='selected']//a/span"));
        if (!selectedTab.equals(tabElement)) {
            if (useAjax) {
                AjaxRequestManager arm = new AjaxRequestManager(driver);
                arm.begin();
                waitUntilEnabledAndClick(tabElement);
                arm.end();
            } else {
                waitUntilEnabledAndClick(tabElement);
            }
        }
    }

    protected void clickOnTabIfNotSelected(String tabPanelId, String tabElementId) {
        WebElement tabElement = findElementWithTimeout(
                By.xpath("//div[@id='" + tabPanelId + "']//a[contains(@id,'" + tabElementId + "')]/span"));
        clickOnTabIfNotSelected(tabPanelId, tabElement);
    }

}
