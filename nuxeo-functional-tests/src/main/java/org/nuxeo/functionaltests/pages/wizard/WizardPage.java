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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WizardPage extends AbstractWizardPage {

//    protected static final String NEXT_BUTTON_LOCATOR = "//input[@class=\"glossyButton\" and @value=\"Next step\"]";
//    protected static final String PREV_BUTTON_LOCATOR = "//input[@class=\"glossyButton\" and @value=\"Previous step\"]";
    protected static final String NEXT_BUTTON_LOCATOR = "id('btnNext')";
    protected static final String PREV_BUTTON_LOCATOR = "id('btnPrev')";

    public WizardPage(WebDriver driver) {
        super(driver);
        IFrameHelper.focusOnWizardPage(driver);
    }

    public WizardPage next() {
        return next(WizardPage.class);
    }

    public WizardPage previous() {
        return previous(WizardPage.class);
    }

    @Override
    protected String getNextButtonLocator() {
        return NEXT_BUTTON_LOCATOR;
    }

    @Override
    protected String getPreviousButtonLocator() {
        return PREV_BUTTON_LOCATOR;
    }

    public ConnectWizardPage getConnectPage() {
        driver.switchTo().frame("connectForm");
        return asPage(ConnectWizardPage.class);
    }

    public boolean hasError() {
        return getErrors().size() > 0;
    }

    public List<String> getErrors() {
        List<WebElement> errorsEl = driver.findElements(By.xpath("//div[@class='errBlock']/div[@class='errItem']"));
        List<String> errors = new ArrayList<String>();
        for (WebElement errorEl : errorsEl) {
            if (errorEl.getText() != null && errorEl.getText().length() > 0) {
                errors.add(errorEl.getText());
            }
        }
        return errors;
    }

}
