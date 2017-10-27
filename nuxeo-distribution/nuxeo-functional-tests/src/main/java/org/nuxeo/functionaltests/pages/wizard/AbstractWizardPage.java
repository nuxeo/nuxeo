/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.functionaltests.pages.wizard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.google.common.base.Function;

public abstract class AbstractWizardPage extends AbstractPage {

    private static final Log log = LogFactory.getLog(AbstractWizardPage.class);

    protected static final String BUTTON_LOCATOR = "//input[@value=\"LABEL\"]";

    public AbstractWizardPage(WebDriver driver) {
        super(driver);
    }

    public String getTitle() {
        WebElement title = findElementWithTimeout(By.xpath("//h1"));
        return title.getText().trim();
    }

    protected abstract By getNextButtonLocator();

    protected abstract By getPreviousButtonLocator();

    public <T extends AbstractWizardPage> T next(Class<T> wizardPageClass) {
        return next(wizardPageClass, null);
    }

    public <T extends AbstractWizardPage> T next(Class<T> wizardPageClass, Function<WebDriver, Boolean> function) {
        return nav(wizardPageClass, getNextButtonLocator(), function);
    }

    public <T extends AbstractWizardPage> T previous(Class<T> wizardPageClass) {
        return previous(wizardPageClass, null);
    }

    public <T extends AbstractWizardPage> T previous(Class<T> wizardPageClass, Function<WebDriver, Boolean> function) {
        return nav(wizardPageClass, getPreviousButtonLocator(), function);
    }

    public <T extends AbstractPage> T nav(Class<T> wizardPageClass, String buttonLabel) {
        return nav(wizardPageClass, buttonLabel, false);
    }

    public <T extends AbstractPage> T nav(Class<T> wizardPageClass, String buttonLabel, Boolean waitForURLChange) {
        return nav(wizardPageClass, getNavButtonLocator(buttonLabel), waitForURLChange);
    }

    /**
     * @since 9.3
     */
    protected <T extends AbstractPage> T nav(Class<T> wizardPageClass, By selector, Boolean waitForURLChange) {
        return nav(wizardPageClass, selector, waitForURLChange ? null : (Function<WebDriver, Boolean>) input -> true);
    }

    /**
     * @since 9.3
     */
    protected <T extends AbstractPage> T nav(Class<T> wizardPageClass, By selector,
            Function<WebDriver, Boolean> function) {
        WebElement action = findElementWithTimeout(selector);
        String URLbefore = driver.getCurrentUrl();
        Locator.waitUntilEnabledAndClick(action);

        if (function == null) {
            waitUntilURLDifferentFrom(URLbefore);
        } else {
            Locator.waitUntilGivenFunction(function);
        }
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T navByLink(Class<T> wizardPageClass, String linkLabel) {
        return navByLink(wizardPageClass, linkLabel, false);
    }

    public <T extends AbstractPage> T navByLink(Class<T> wizardPageClass, String linkLabel, Boolean waitForURLChange) {
        return nav(wizardPageClass, By.linkText(linkLabel), waitForURLChange);
    }

    public <T extends AbstractPage> T navById(Class<T> wizardPageClass, String buttonId) {
        return navById(wizardPageClass, buttonId, false);
    }

    public <T extends AbstractPage> T navById(Class<T> wizardPageClass, String buttonId, Boolean waitForURLChange) {
        return nav(wizardPageClass, By.id(buttonId), waitForURLChange);
    }

    protected By getNavButtonLocator(String label) {
        return By.xpath(BUTTON_LOCATOR.replace("LABEL", label));
    }

    public boolean fillInput(String name, String value) {
        WebElement element = findElementWithTimeout(By.name(name));
        if (element != null) {
            element.clear();
            element.sendKeys(value);
            return true;
        }
        return false;
    }

    public boolean selectOption(String name, String value) {
        WebElement element = findElementWithTimeout(By.name(name));
        if (element != null) {
            Select select = new Select(element);
            select.selectByValue(value);
            return select.getFirstSelectedOption().getAttribute("value").equals(value);
        }
        return false;
    }

    public boolean selectOptionWithReload(String name, String value) {

        WebElement element = findElementWithTimeout(By.name(name));
        if (element != null) {
            Select select = new Select(element);
            select.selectByValue(value);
            // page is reload, need to fetch element again
            element = findElementWithTimeout(By.name(name));
            select = new Select(element);
            return select.getFirstSelectedOption().getAttribute("value").equals(value);
        }
        return false;
    }

    public boolean clearInput(String name) {
        WebElement element = findElementWithTimeout(By.name(name));
        if (element != null) {
            element.clear();
            return true;
        }
        return false;
    }

}
