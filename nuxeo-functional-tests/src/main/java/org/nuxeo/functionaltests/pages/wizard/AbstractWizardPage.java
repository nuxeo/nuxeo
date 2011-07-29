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
 *     Thierry Delprat
 */
package org.nuxeo.functionaltests.pages.wizard;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public abstract class AbstractWizardPage extends AbstractPage {

    protected static final String BUTTON_LOCATOR = "//input[@class=\"glossyButton\" and @value=\"LABEL\"]";

    public AbstractWizardPage(WebDriver driver) {
        super(driver);
    }

    public String getTitle() {
        WebElement title = findElementWithTimeout(By.xpath("//h1"));
        return title.getText().trim();
    }

    protected abstract String getNextButtonLocator();

    protected abstract String getPreviousButtonLocator();

    public <T extends AbstractWizardPage> T next(Class<T> wizardPageClass) {
        WebElement buttonNext = findElementWithTimeout(By.xpath(getNextButtonLocator()));
        buttonNext.click();
        return asPage(wizardPageClass);
    }

    public <T extends AbstractWizardPage> T previous(Class<T> wizardPageClass) {
        WebElement buttonPrev = findElementWithTimeout(By.xpath(getPreviousButtonLocator()));
        buttonPrev.click();
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T nav(Class<T> wizardPageClass,
            String buttonLabel) {
        WebElement button = findNavButton(buttonLabel);
        if (button == null) {
            return null;
        }
        button.click();
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T navByLink(Class<T> wizardPageClass,
            String linkLabel) {
        WebElement link = findElementWithTimeout(By.linkText(linkLabel));
        if (link == null) {
            return null;
        }
        link.click();
        return asPage(wizardPageClass);
    }

    public <T extends AbstractPage> T navById(Class<T> wizardPageClass,
            String buttonId) {
        WebElement button = findElementWithTimeout(By.id(buttonId));
        if (button == null) {
            return null;
        }
        button.click();
        return asPage(wizardPageClass);
    }

    protected WebElement findNavButton(String label) {
        return findElementWithTimeout(By.xpath(BUTTON_LOCATOR.replace("LABEL",
                label)));
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
            return select.getFirstSelectedOption().getValue().equals(value);
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
