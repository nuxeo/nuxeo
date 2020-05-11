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

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public abstract class AbstractExplorerPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//div[@class='top-banner']/a")
    public WebElement homeLink;

    public AbstractExplorerPage(WebDriver driver) {
        super(driver);
    }

    public void clickOn(WebElement element) {
        Locator.scrollAndForceClick(element);
    }

    public ExplorerHomePage goHome() {
        clickOn(homeLink);
        return asPage(ExplorerHomePage.class);
    }

    public void checkTitle(String expected) {
        assertEquals(expected, driver.getTitle());
    }

    /**
     * Generic page check to be implemented.
     */
    public abstract void check();

}
