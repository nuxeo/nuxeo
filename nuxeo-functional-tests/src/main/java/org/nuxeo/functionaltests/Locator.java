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
package org.nuxeo.functionaltests;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 *
 *
 * @since 5.9.2
 */
public class Locator {

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by, int timeout)
            throws NoSuchElementException {
        return findElementWithTimeout(by, timeout, null);
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
    public static WebElement findElementWithTimeout(final By by, int timeout,
            final WebElement parentElement) throws NoSuchElementException {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                timeout, TimeUnit.MILLISECONDS).pollingEvery(AbstractTest.POLLING_FREQUENCY_MILLISECONDS,
                TimeUnit.MILLISECONDS);
        try {
            return wait.until(new Function<WebDriver, WebElement>() {
                public WebElement apply(WebDriver driver) {
                    try {
                        if (parentElement == null) {
                            return driver.findElement(by);
                        } else {
                            return parentElement.findElement(by);
                        }
                    } catch (NoSuchElementException e) {
                        return null;
                    }
                }
            });
        } catch (TimeoutException e) {
            throw new NoSuchElementException(String.format(
                    "Couldn't find element '%s' after timeout", by));
        }
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
        return findElementWithTimeout(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000);
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
    public static WebElement findElementWithTimeout(By by,
            WebElement parentElement) throws NoSuchElementException {
        return findElementWithTimeout(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                parentElement);
    }

    public static List<WebElement> findElementsWithTimeout(final By by)
            throws NoSuchElementException {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);
        return wait.until(new Function<WebDriver, List<WebElement>>() {
            public List<WebElement> apply(WebDriver driver) {
                List<WebElement> elements = driver.findElements(by);
                return elements.isEmpty() ? null : elements;
            }
        });
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * {@code findElementTimeout}. Then waits until the element is enabled, with
     * a {@code waitUntilEnabledTimeout}.
     *
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in
     *            milliseconds
     * @return the first matching element on the current page, if found
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static WebElement findElementAndWaitUntilEnabled(final By by,
            final int findElementTimeout, final int waitUntilEnabledTimeout)
            throws NotFoundException {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(AbstractTest.POLLING_FREQUENCY_MILLISECONDS,
                TimeUnit.MILLISECONDS);
        Function<WebDriver, WebElement> function = new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver driver) {
                WebElement element = null;
                try {
                    // Find the element.
                    element = findElementWithTimeout(by, findElementTimeout);

                    // Try to wait until the element is enabled.
                    waitUntilEnabled(element, waitUntilEnabledTimeout);
                } catch (StaleElementReferenceException sere) {
                    AbstractTest.log.debug("StaleElementReferenceException: "
                            + sere.getMessage());
                    return null;
                }
                return element;
            }
        };

        return wait.until(function);

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
        return findElementAndWaitUntilEnabled(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * {@code findElementTimeout}. Then waits until the element is enabled, with
     * a {@code waitUntilEnabledTimeout}. Then clicks on the element.
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

        // Find the element.
        WebElement element = findElementAndWaitUntilEnabled(by,
                findElementTimeout, waitUntilEnabledTimeout);

        element.click();
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
        findElementWaitUntilEnabledAndClick(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     * @param timeout the timeout in milliseconds
     */
    public static void waitUntilEnabled(final WebElement element, int timeout)
            throws NotFoundException {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                timeout, TimeUnit.MILLISECONDS).pollingEvery(AbstractTest.POLLING_FREQUENCY_MILLISECONDS,
                TimeUnit.MILLISECONDS);
        Function<WebDriver, Boolean> function = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                return element.isEnabled();
            }
        };
        try {
            wait.until(function);
        } catch (TimeoutException e) {
            throw new NotFoundException("Element not enabled after timeout: "
                    + element);
        }
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     */
    public static void waitUntilEnabled(WebElement element)
            throws NotFoundException {
        waitUntilEnabled(element, AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Fluent wait for text to be present in the element retrieved with the
     * given method.
     *
     * @since 5.7.3
     */
    public static void waitForTextPresent(By locator, String text) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        wait.until(ExpectedConditions.textToBePresentInElement(locator, text));
    }

    /**
     * Fluent wait for text to be present in the given element.
     *
     * @since 5.7.3
     */
    public static void waitForTextPresent(final WebElement element,
            final String text) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        wait.until((new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    return element.getText().contains(text);
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
        }));
    }

    /**
     * Fluent wait for text to be not present in the given element.
     *
     * @since 5.7.3
     */
    public static void waitForTextNotPresent(final WebElement element,
            final String text) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        wait.until((new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    return !element.getText().contains(text);
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
        }));
    }

    /**
     * Fluent wait for an element to be present, checking every 5 seconds
     *
     * @since 5.7.2
     */
    public static WebElement waitUntilElementPresent(final By locator) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);
        WebElement elt = wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver driver) {
                return driver.findElement(locator);
            }
        });
        return elt;
    }

    /**
     * Fluent wait for an element not to be present, checking every 5 seconds.
     *
     * @since 5.7.2
     */
    public static void waitUntilElementNotPresent(final By locator) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        wait.until((new Function<WebDriver, By>() {
            public By apply(WebDriver driver) {
                try {
                    driver.findElement(locator);
                } catch (NoSuchElementException ex) {
                    // ok
                    return locator;
                }
                return null;
            }
        }));
    }


}
