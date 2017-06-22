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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;

/**
 * Helper class providing find and wait methods with or without timeout. When requiring timeout, the polling frequency
 * is every 100 milliseconds if not specified.
 *
 * @since 5.9.2
 */
public class Locator {

    private static final Log log = LogFactory.getLog(Locator.class);

    // Timeout for waitUntilURLDifferentFrom in seconds
    public static int URLCHANGE_MAX_WAIT = 30;

    public static WebElement findElement(By by) {
        return AbstractTest.driver.findElement(by);
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
        return findElementAndWaitUntilEnabled(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
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
    public static WebElement findElementAndWaitUntilEnabled(final By by, final int findElementTimeout,
            final int waitUntilEnabledTimeout) throws NotFoundException {
        return findElementAndWaitUntilEnabled(null, by, findElementTimeout, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a {@code findElementTimeout}, inside an optional
     * {@code parentElement}. Then waits until the element is enabled, with a {@code waitUntilEnabledTimeout}.
     *
     * @param parentElement the parent element (can be null)
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @return the first matching element on the current page, if found, with optional parent element
     * @throws NotFoundException if the element is not found or not enabled
     * @since 8.3
     */
    public static WebElement findElementAndWaitUntilEnabled(WebElement parentElement, final By by,
            final int findElementTimeout, final int waitUntilEnabledTimeout) throws NotFoundException {
        Wait<WebDriver> wait = getFluentWait();
        Function<WebDriver, WebElement> function = new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                WebElement element = null;
                try {
                    // Find the element.
                    element = findElementWithTimeout(by, findElementTimeout, parentElement);

                    // Try to wait until the element is enabled.
                    waitUntilEnabled(element, waitUntilEnabledTimeout);
                } catch (StaleElementReferenceException sere) {
                    AbstractTest.log.debug("StaleElementReferenceException: " + sere.getMessage());
                    return null;
                }
                return element;
            }
        };

        return wait.until(function);

    }

    public static List<WebElement> findElementsWithTimeout(final By by) throws NoSuchElementException {
        FluentWait<WebDriver> wait = getFluentWait();
        wait.ignoring(NoSuchElementException.class);
        return wait.until(new Function<WebDriver, List<WebElement>>() {
            @Override
            public List<WebElement> apply(WebDriver driver) {
                List<WebElement> elements = driver.findElements(by);
                return elements.isEmpty() ? null : elements;
            }
        });
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the default timeout. Then waits until the element
     * is enabled, with the default timeout. Then clicks on the element.
     *
     * @param by the locating mechanism
     * @throws NotFoundException if the element is not found or not enabled
     */
    public static void findElementWaitUntilEnabledAndClick(By by) throws NotFoundException {
        findElementWaitUntilEnabledAndClick(null, by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with the default timeout, inside an optional
     * {@code parentElement}. Then waits until the element is enabled, with the default timeout. Then clicks on the
     * element.
     *
     * @param parentElement the parent element (can be null)
     * @param by the locating mechanism
     * @throws NotFoundException if the element is not found or not enabled
     * @since 8.3
     */
    public static void findElementWaitUntilEnabledAndClick(WebElement parentElement, By by) throws NotFoundException {
        findElementWaitUntilEnabledAndClick(parentElement, by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a timeout.
     *
     * @param by the locating mechanism
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by) throws NoSuchElementException {
        return findElementWithTimeout(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Checks if a corresponding elements is present, with a timeout.
     *
     * @param by the locating mechanism
     * @return true if element exists, false otherwise
     */
    public static boolean hasElementWithTimeout(By by) {
        try {
            return findElementWithTimeout(by) != null;
        } catch (NoSuchElementException nsee) {
            return false;
        }
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by, int timeout) throws NoSuchElementException {
        return findElementWithTimeout(by, timeout, null);
    }

    /**
     * Checks if a corresponding elements is present, with a timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return true if element exists, false otherwise
     */
    public static boolean hasElementWithTimeout(By by, int timeout) {
        try {
            return findElementWithTimeout(by, timeout) != null;
        } catch (NoSuchElementException nsee) {
            return false;
        }
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
    public static WebElement findElementWithTimeout(final By by, int timeout, final WebElement parentElement)
            throws NoSuchElementException {
        FluentWait<WebDriver> wait = getFluentWait();
        wait.withTimeout(timeout, TimeUnit.MILLISECONDS).ignoring(StaleElementReferenceException.class);
        try {
            return wait.until(new Function<WebDriver, WebElement>() {
                @Override
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
            throw new NoSuchElementException(String.format("Couldn't find element '%s' after timeout", by));
        }
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
    public static WebElement findElementWithTimeout(By by, WebElement parentElement) throws NoSuchElementException {
        return findElementWithTimeout(by, AbstractTest.LOAD_TIMEOUT_SECONDS * 1000, parentElement);
    }

    public static FluentWait<WebDriver> getFluentWait() {
        FluentWait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver);
        wait.withTimeout(AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .pollingEvery(AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS);
        return wait;
    }

    /**
     * Fluent wait for text to be not present in the given element.
     *
     * @since 5.7.3
     */
    public static void waitForTextNotPresent(final WebElement element, final String text) {
        Wait<WebDriver> wait = getFluentWait();
        wait.until((new Function<WebDriver, Boolean>() {
            @Override
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
     * Fluent wait for text to be present in the element retrieved with the given method.
     *
     * @since 5.7.3
     */
    public static void waitForTextPresent(By locator, String text) {
        Wait<WebDriver> wait = getFluentWait();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Fluent wait for text to be present in the given element.
     *
     * @since 5.7.3
     */
    public static void waitForTextPresent(final WebElement element, final String text) {
        Wait<WebDriver> wait = getFluentWait();
        wait.until((new Function<WebDriver, Boolean>() {
            @Override
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
     * Finds the first {@link WebElement} using the given method, with a {@code findElementTimeout}. Then waits until
     * the element is enabled, with a {@code waitUntilEnabledTimeout}. Scroll to it, then clicks on the element.
     *
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @throws NotFoundException if the element is not found or not enabled
     * @deprecated since 8.3, use {@link #findElementWaitUntilEnabledAndClick(WebElement, By)}
     */
    @Deprecated
    public static void findElementWaitUntilEnabledAndClick(final By by, final int findElementTimeout,
            final int waitUntilEnabledTimeout) throws NotFoundException {
        findElementWaitUntilEnabledAndClick(null, by, findElementTimeout, waitUntilEnabledTimeout);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a {@code findElementTimeout}, inside an optional
     * {@code parentElement}. Then waits until the element is enabled, with a {@code waitUntilEnabledTimeout}. Scroll to
     * it, then clicks on the element.
     *
     * @param parentElement the parent element (can be null)
     * @param by the locating mechanism
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @throws NotFoundException if the element is not found or not enabled
     * @since 8.3
     */
    public static void findElementWaitUntilEnabledAndClick(WebElement parentElement, final By by,
            final int findElementTimeout, final int waitUntilEnabledTimeout) throws NotFoundException {
        WebElement element = findElementAndWaitUntilEnabled(parentElement, by, findElementTimeout,
                waitUntilEnabledTimeout);
        waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return scrollAndForceClick(element);
            }
        }, StaleElementReferenceException.class);
    }

    /**
     * Waits until the element is enabled, with a default timeout. Then clicks on the element.
     *
     * @param element the element
     * @throws NotFoundException if the element is not found or not enabled
     * @since 8.3
     */
    public static void waitUntilEnabledAndClick(final WebElement element) throws NotFoundException {
        waitUntilEnabledAndClick(element, AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Waits until the element is enabled, with a {@code waitUntilEnabledTimeout}. Scroll to it, then clicks on the
     * element.
     *
     * @param element the element
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @throws NotFoundException if the element is not found or not enabled
     * @since 8.3
     */
    public static void waitUntilEnabledAndClick(final WebElement element, final int waitUntilEnabledTimeout)
            throws NotFoundException {
        waitUntilEnabled(element, waitUntilEnabledTimeout);
        waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return scrollAndForceClick(element);
            }
        }, StaleElementReferenceException.class);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a {@code findElementTimeout}. Then clicks on the
     * element.
     *
     * @param by the locating mechanism
     * @throws NotFoundException if the element is not found or not enabled
     * @since 5.9.4
     */
    public static void findElementWithTimeoutAndClick(final By by) throws NotFoundException {

        waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                // Find the element.
                WebElement element = findElementWithTimeout(by);

                element.click();
                return true;
            }
        }, StaleElementReferenceException.class);
    }

    /**
     * Fluent wait for an element not to be present, checking every 100 ms.
     *
     * @since 5.7.2
     */
    public static void waitUntilElementNotPresent(final By locator) {
        Wait<WebDriver> wait = getFluentWait();
        wait.until((new Function<WebDriver, By>() {
            @Override
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

    /**
     * Fluent wait for an element to be present, checking every 100 ms.
     *
     * @since 5.7.2
     */
    public static void waitUntilElementPresent(final By locator) {
        FluentWait<WebDriver> wait = getFluentWait();
        wait.ignoring(NoSuchElementException.class);
        wait.until(new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                return driver.findElement(locator);
            }
        });
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     */
    public static void waitUntilEnabled(WebElement element) throws NotFoundException {
        waitUntilEnabled(element, AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     * @param timeout the timeout in milliseconds
     */
    public static void waitUntilEnabled(final WebElement element, int timeout) throws NotFoundException {
        FluentWait<WebDriver> wait = getFluentWait();
        wait.withTimeout(timeout, TimeUnit.MILLISECONDS);
        Function<WebDriver, Boolean> function = new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return element.isEnabled();
            }
        };
        try {
            wait.until(function);
        } catch (TimeoutException e) {
            throw new NotFoundException("Element not enabled after timeout: " + element);
        }
    }

    /**
     * Fluent wait on a the given function, checking every 100 ms.
     *
     * @param function
     * @since 5.9.2
     */
    public static void waitUntilGivenFunction(Function<WebDriver, Boolean> function) {
        waitUntilGivenFunctionIgnoring(function, null);
    }

    /**
     * Fluent wait on a the given function, checking every 100 ms.
     *
     * @param function
     * @param ignoredExceptions the types of exceptions to ignore.
     * @since 5.9.2
     */
    @SafeVarargs
    public static <K extends java.lang.Throwable> void waitUntilGivenFunctionIgnoreAll(
            Function<WebDriver, Boolean> function, java.lang.Class<? extends K>... ignoredExceptions) {
        FluentWait<WebDriver> wait = getFluentWait();
        if (ignoredExceptions != null) {
            if (ignoredExceptions.length == 1) {
                wait.ignoring(ignoredExceptions[0]);
            } else {
                wait.ignoreAll(Arrays.asList(ignoredExceptions));
            }

        }
        wait.until(function);
    }

    /**
     * Fluent wait on a the given function, checking every 100 ms.
     *
     * @param function
     * @param ignoredException the type of exception to ignore.
     * @since 5.9.2
     */
    public static <K extends java.lang.Throwable> void waitUntilGivenFunctionIgnoring(
            Function<WebDriver, Boolean> function, java.lang.Class<? extends K> ignoredException) {
        FluentWait<WebDriver> wait = getFluentWait();
        if (ignoredException != null) {
            wait.ignoring(ignoredException);
        }
        wait.until(function);
    }

    /**
     * Waits until the URL contains the string given in parameter, with a timeout.
     *
     * @param string the string that is to be contained
     * @since 5.9.2
     */
    public static void waitUntilURLContains(String string) {
        waitUntilURLContainsOrNot(string, true);
    }

    /**
     * @since 5.9.2
     */
    private static void waitUntilURLContainsOrNot(String string, final boolean contain) {
        final String refurl = string;
        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver d) {
                String currentUrl = d.getCurrentUrl();
                boolean result = !(currentUrl.contains(refurl) ^ contain);
                if (!result) {
                    AbstractTest.log.debug("currentUrl is : " + currentUrl);
                    AbstractTest.log.debug((contain ? "It should contains : " : "It should not contains : ") + refurl);
                }
                return result;
            }
        };
        WebDriverWait wait = new WebDriverWait(AbstractTest.driver, URLCHANGE_MAX_WAIT);
        wait.until(condition);
    }

    /**
     * Waits until the URL is different from the one given in parameter, with a timeout.
     *
     * @param url the URL to compare to
     */
    public static void waitUntilURLDifferentFrom(String url) {
        final String refurl = url;
        ExpectedCondition<Boolean> urlchanged = d -> {
            String currentUrl = d.getCurrentUrl();
            AbstractTest.log.debug("currentUrl is still: " + currentUrl);
            return !currentUrl.equals(refurl);
        };
        WebDriverWait wait = new WebDriverWait(AbstractTest.driver, URLCHANGE_MAX_WAIT);
        wait.until(urlchanged);
        if (AbstractTest.driver.getCurrentUrl().equals(refurl)) {
            log.warn("Page change failed");
        }
    }

    /**
     * Waits until the URL does not contain the string given in parameter, with a timeout.
     *
     * @param string the string that is not to be contained
     * @since 5.9.2
     */
    public static void waitUntilURLNotContain(String string) {
        waitUntilURLContainsOrNot(string, false);
    }

    /**
     * Return parent element with given tag name.
     * <p>
     * Throws a {@link NoSuchElementException} error if no element found.
     *
     * @since 7.3
     */
    public static WebElement findParentTag(WebElement elt, String tagName) {
        try {
            By parentBy = By.xpath("..");
            WebElement p = elt.findElement(parentBy);
            while (p != null) {
                if (tagName.equals(p.getTagName())) {
                    return p;
                }
                p = p.findElement(parentBy);
            }
        } catch (InvalidSelectorException e) {
        }
        throw new NoSuchElementException(String.format("No parent element found with tag %s.", tagName));
    }

    /**
     * Scrolls to the element in the view: allows to safely click on it afterwards.
     *
     * @param executor the javascript executor, usually {@link WebDriver}
     * @param element the element to scroll to
     * @since 8.3
     */
    public static final void scrollToElement(WebElement element) {
        ((JavascriptExecutor) AbstractTest.driver).executeScript("arguments[0].scrollIntoView(false);", element);
    }

    /**
     * Forces a click on an element, to workaround non-effective clicks in miscellaneous situations, after having
     * scrolled to it.
     *
     * @param executor the javascript executor, usually {@link WebDriver}
     * @param element the element to scroll to
     * @return true if element is clickable
     * @since 8.3
     */
    public static final boolean scrollAndForceClick(WebElement element) {
        JavascriptExecutor executor = (JavascriptExecutor) AbstractTest.driver;
        scrollToElement(element);
        try {
            // forced click to workaround non-effective clicks in miscellaneous situations
            executor.executeScript("arguments[0].click();", element);
            return true;
        } catch (WebDriverException e) {
            if (e.getMessage().contains("Element is not clickable at point")) {
                log.debug("Element is not clickable yet");
                return false;
            }
            throw e;
        }
    }

}
