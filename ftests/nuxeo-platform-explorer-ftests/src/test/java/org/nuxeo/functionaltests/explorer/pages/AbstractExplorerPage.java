/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.pages;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.FluentWait;

/**
 * @since 11.1
 */
public abstract class AbstractExplorerPage extends AbstractPage {

    public static final int LONG_WAIT_TIMEOUT_SECONDS = 60;

    public static final int LONG_POLLING_FREQUENCY_SECONDS = 1;

    @Required
    @FindBy(xpath = "//div[@class='top-banner']/a")
    public WebElement homeLink;

    public AbstractExplorerPage(WebDriver driver) {
        super(driver);
    }

    public void clickOn(WebElement element) {
        Locator.scrollAndForceClick(element);
    }

    public FluentWait<WebDriver> getLongWait() {
        FluentWait<WebDriver> wait = new FluentWait<>(AbstractTest.driver);
        wait.withTimeout(LONG_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .pollingEvery(LONG_POLLING_FREQUENCY_SECONDS, TimeUnit.SECONDS);
        return wait;
    }

    public ExplorerHomePage goHome() {
        clickOn(homeLink);
        return asPage(ExplorerHomePage.class);
    }

    public void checkTitle(String expected) {
        assertEquals(expected, driver.getTitle());
    }

    /**
     * Waits for indexing to be done.
     */
    public void waitForAsyncWork() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timeoutSecond", Integer.valueOf(110));
        parameters.put("refresh", Boolean.TRUE);
        parameters.put("waitForAudit", Boolean.TRUE);
        RestHelper.operation("Elasticsearch.WaitForIndexing", parameters);
    }

    /**
     * Generic page check to be implemented.
     */
    public abstract void check();

}
