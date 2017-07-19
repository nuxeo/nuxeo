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

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import com.google.common.base.Function;

public abstract class AbstractWizardPage extends AbstractPage {

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
        WebElement buttonNext = findElementWithTimeout(getNextButtonLocator());
        String URLbefore = driver.getCurrentUrl();
        new Actions(driver).moveToElement(buttonNext).perform();
        buttonNext.click();
        if (function == null) {
            waitUntilURLDifferentFrom(URLbefore);
        } else {
            Locator.waitUntilGivenFunction(function);
        }
        return asPage(wizardPageClass);
    }

    public <T extends AbstractWizardPage> T previous(Class<T> wizardPageClass) {
        return previous(wizardPageClass, null);
    }

    public <T extends AbstractWizardPage> T previous(Class<T> wizardPageClass, Function<WebDriver, Boolean> function) {
        WebElement buttonPrev = findElementWithTimeout(getPreviousButtonLocator());
        String URLbefore = driver.getCurrentUrl();
        buttonPrev.click();
        if (function == null) {
            waitUntilURLDifferentFrom(URLbefore);
        } else {
            Locator.waitUntilGivenFunction(function);
        }
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T nav(Class<T> wizardPageClass, String buttonLabel) {
        return nav(wizardPageClass, buttonLabel, false);
    }

    public <T extends AbstractPage> T nav(Class<T> wizardPageClass, String buttonLabel, Boolean waitForURLChange) {
        WebElement button = findNavButton(buttonLabel);
        if (button == null) {
            return null;
        }
        String URLbefore = driver.getCurrentUrl();
        button.click();
        if (waitForURLChange == true) {
            waitUntilURLDifferentFrom(URLbefore);
        }
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T navByLink(Class<T> wizardPageClass, String linkLabel) {
        return navByLink(wizardPageClass, linkLabel, false);
    }

    public <T extends AbstractPage> T navByLink(Class<T> wizardPageClass, String linkLabel, Boolean waitForURLChange) {
        WebElement link = findElementWithTimeout(By.linkText(linkLabel));
        if (link == null) {
            return null;
        }
        String URLbefore = driver.getCurrentUrl();
        link.click();
        waitUntilURLDifferentFrom(URLbefore);
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T navById(Class<T> wizardPageClass, String buttonId) {
        return navById(wizardPageClass, buttonId, false);
    }

    public <T extends AbstractPage> T navById(Class<T> wizardPageClass, String buttonId, Boolean waitForURLChange) {
        WebElement button = findElementWithTimeout(By.id(buttonId));
        if (button == null) {
            return null;
        }
        String URLbefore = driver.getCurrentUrl();
        button.click();
        if (waitForURLChange == true) {
            waitUntilURLDifferentFrom(URLbefore);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        return asPage(wizardPageClass);
    }

    protected WebElement findNavButton(String label) {
        return findElementWithTimeout(By.xpath(BUTTON_LOCATOR.replace("LABEL", label)));
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
