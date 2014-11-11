/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Base functions for all pages.
 */
public abstract class AbstractPage {

    // Timeout for waitUntilURLDifferentFrom in seconds
    public static int URLCHANGE_MAX_WAIT = 10;

    @FindBy(name = "userServicesForm")
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
        boolean present;
        try {
            driver.findElement(by);
            present = true;
        } catch (NoSuchElementException e) {
            present = false;
        }
        return present;
    }

    public <T> T get(String url, Class<T> pageClassToProxy) {
        return AbstractTest.get(url, pageClassToProxy);
    }

    public <T> T asPage(Class<T> pageClassToProxy) {
        return AbstractTest.asPage(pageClassToProxy);
    }

    public <T extends WebFragment> T getWebFragment(By by,
            Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(by, webFragmentClass);
    }

    public <T extends WebFragment> T getWebFragment(WebElement element,
            Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(element, webFragmentClass);
    }

    /**
     * Gets the info feedback message.
     *
     * @return the message if any or an empty string.
     */
    public String getFeedbackMessage() {
        String ret;
        try {
            ret = findElementWithTimeout(
                    By.xpath("//li[@class=\"errorFeedback\"]")).getText();
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
     * Returns the fancy box content web element
     *
     * @since 5.7
     */
    public WebElement getFancyBoxContent() {
        // make sure the fancybox content is loaded
        WebElement fancyBox = findElementWithTimeout(By.id("fancybox-content"));
        WebDriverWait wait = new WebDriverWait(driver,
                AbstractTest.LOAD_TIMEOUT_SECONDS);
        wait.until(ExpectedConditions.visibilityOf(fancyBox));
        return fancyBox;
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public WebElement findElementWithTimeout(By by, int timeout)
            throws NoSuchElementException {
        return AbstractTest.findElementWithTimeout(by, timeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @param parentElement find from the element
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public WebElement findElementWithTimeout(By by, int timeout,
            WebElement parentElement) throws NoSuchElementException {
        return AbstractTest.findElementWithTimeout(by, timeout, parentElement);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by)
            throws NoSuchElementException {
        return AbstractTest.findElementWithTimeout(by);
    }

    /**
     * Finds webelement list using the given method, with a timeout
     */
    public static List<WebElement> findElementsWithTimeout(By by)
            throws NoSuchElementException {
        return AbstractTest.findElementsWithTimeout(by);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param parentElement find from the element
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by,
            WebElement parentElement) throws NoSuchElementException {
        return AbstractTest.findElementWithTimeout(by, parentElement);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     * @param timeout the timeout in milliseconds
     */
    public static void waitUntilEnabled(final WebElement element, int timeout)
            throws NotFoundException {
        AbstractTest.waitUntilEnabled(element, timeout);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     */
    public static void waitUntilEnabled(WebElement element)
            throws NotFoundException {
        AbstractTest.waitUntilEnabled(element);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * {@code findElementTimeout}. Then waits until the element is enabled,
     * with a {@code waitUntilEnabledTimeout}.
     *
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in
     *            milliseconds
     * @return the first matching element on the current page, if found
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static WebElement findElementAndWaitUntilEnabled(By by,
            int findElementTimeout, int waitUntilEnabledTimeout)
            throws NotFoundException {
        return AbstractTest.findElementAndWaitUntilEnabled(by,
                findElementTimeout, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the
     * default timeout. Then waits until the element is enabled, with the
     * default timeout.
     *
     * @param by the locating mechanism
     * @return the first matching element on the current page, if found
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static WebElement findElementAndWaitUntilEnabled(By by)
            throws NotFoundException {
        return AbstractTest.findElementAndWaitUntilEnabled(by);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * {@code findElementTimeout}. Then waits until the element is enabled,
     * with a {@code waitUntilEnabledTimeout}. Then clicks on the element.
     *
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in
     *            milliseconds
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static void findElementWaitUntilEnabledAndClick(By by,
            int findElementTimeout, int waitUntilEnabledTimeout)
            throws NotFoundException {
        AbstractTest.findElementWaitUntilEnabledAndClick(by,
                findElementTimeout, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the
     * default timeout. Then waits until the element is enabled, with the
     * default timeout. Then clicks on the element.
     *
     * @param by the locating mechanism
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static void findElementWaitUntilEnabledAndClick(By by)
            throws NotFoundException {
        AbstractTest.findElementWaitUntilEnabledAndClick(by);
    }

    /**
     * Waits until the URL is different from the one given in parameter, with a
     * timeout.
     *
     * @param url the URL to compare to
     */
    public void waitUntilURLDifferentFrom(String url) {
        final String refurl = url;
        ExpectedCondition<Boolean> urlchanged = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver d) {
                return !d.getCurrentUrl().equals(refurl);
            }
        };
        WebDriverWait wait = new WebDriverWait(driver, URLCHANGE_MAX_WAIT);
        wait.until(urlchanged);
        if (driver.getCurrentUrl().equals(refurl)) {
            System.out.println("Page change failed");
        }
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
}
