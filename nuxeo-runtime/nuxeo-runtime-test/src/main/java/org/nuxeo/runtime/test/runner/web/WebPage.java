/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class WebPage {

    /**
     * Can be used by tests as default timeouts. This way you can change these
     * values to change all the timeout in the tests. DEFAULT_TIMEOUT is used
     * for regular timeouts (loading an ajax page an ajax dialog etc.) while
     * BIG_TIMEOUT should be used for pages that are slower (like the home of a
     * GWT application)
     */
    public static int DEFAULT_TIMEOUT = 5;

    public static int BIG_TIMEOUT = 15;

    private static final Map<Class<?>, WebPage> pages = new HashMap<>();

    @Inject
    protected Configuration config;

    @Inject
    protected WebDriver driver;

    @Inject
    protected FeaturesRunner runner;

    /**
     * Should be overridden by dynamic page (using ajax) to wait until the page
     * is completely loaded By default nothing is done (page is assumed to be
     * loaded)
     *
     * @return the page itself
     */
    public WebPage ensureLoaded() {
        config.getBrowserFamily().getDriverFactory().waitForAjax(driver);
        return this;

    }

    public Configuration getConfiguration() {
        return config;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public FeaturesRunner getRunner() {
        return runner;
    }

    public void home() {
        driver.get(config.home);
    }

    public void to(String path) {
        if (path.contains("://")) {
            driver.get(path);
        } else {
            try {
                URL url = new URL(new URL(config.home), path);
                driver.navigate().to(url);
            } catch (MalformedURLException e) {
                throw new WebDriverException(e);
            }
        }
    }

    public boolean hasElement(final By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean hasElement(final By by, int timeoutInSeconds) {
        try {
            findElement(by, timeoutInSeconds);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public WebElement findElement(final By by) {
        return driver.findElement(by);
    }

    public List<WebElement> findElements(final By by) {
        return driver.findElements(by);
    }

    public WebElement findElement(final By by, int timeOutInSeconds) {
        return waitUntilElementFound(by, timeOutInSeconds);
    }

    public WebElement waitUntilElementFound(final By by, int timeOutInSeconds) {
        try {
            return findElement(by); // try once first
        } catch (NotFoundException e) {
            return new WebDriverWait(driver, timeOutInSeconds).until(new ExpectedCondition<WebElement>() {
                @Override
                public WebElement apply(WebDriver arg0) {
                    return driver.findElement(by);
                }
            });
        }
    }

    public void waitUntilElementNotFound(final By by, int timeOutInSeconds) {
        try {
            findElement(by); // try once first
            new WebDriverWait(driver, timeOutInSeconds).until(new ExpectedCondition<Boolean>() {
                @Override
                public Boolean apply(WebDriver arg0) {
                    try {
                        driver.findElement(by);
                        return Boolean.FALSE;
                    } catch (NotFoundException e) {
                        return Boolean.TRUE;
                    }
                }
            });
        } catch (NotFoundException e) {
        }
    }

    public <T extends WebPage> T getPage(Class<T> type) {
        return getPage(runner, config, type);
    }

    @SuppressWarnings("unchecked")
    public static <T extends WebPage> T getPage(FeaturesRunner runner, Configuration config, Class<T> type) {
        T page = (T) pages.get(type);
        if (page == null) {
            synchronized (pages) {
                page = (T) pages.get(type);
                if (page != null) {
                    return page;
                }
                page = PageFactory.initElements(config.driver, type);
                runner.getInjector().injectMembers(page);
                pages.put(type, page);
            }
        }
        return (T) page.ensureLoaded(); // this will block until page is loaded
                                        // (if implementation needs this)
    }

    /**
     * Gives access to an attachment retrieved from this web page.
     *
     */
    public Attachment getAttachment() {
        Attachment attachment = ((TakesAttachment) driver).getAttachment();
        Assertions.assertThat(attachment).isNotNull();
        return attachment;
    }

    public static void flushPageCache() {
        synchronized (pages) {
            pages.clear();
        }
    }

}
