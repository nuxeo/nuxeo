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

public class ConnectWizardPage extends AbstractWizardPage {

    public ConnectWizardPage(WebDriver driver) {
        super(driver);
        IFrameHelper.focusOnConnectFrame(driver);
    }

    public void exitIframe() {
        // XXX
    }

    public String getErrorMessage() {
        WebElement el = findElementWithTimeout(By.xpath("//div[@class=\"errorFeedback\"]"));
        if (el == null) {
            return null;
        }
        return el.getText().trim();
    }

    @Override
    protected String getNextButtonLocator() {
        return BUTTON_LOCATOR.replace("LABEL", "Validate & register");
    }

    @Override
    protected String getPreviousButtonLocator() {
        return BUTTON_LOCATOR.replace("LABEL", "Previous");
    }

    public ConnectWizardPage getLink(String text) {
        ConnectWizardPage wpage = getLink(ConnectWizardPage.class, text);
        if (!driver.getCurrentUrl().contains("/site/connect/")) {
            System.out.println("Oups, we are out of the frame !!!");
            driver.switchTo().frame("connectForm");
            return asPage(ConnectWizardPage.class);
        }
        return wpage;
    }

    public <T extends AbstractPage> T getLink(Class<T> wizardPageClass,
            String text) {
        WebElement link = findElementWithTimeout(By.linkText(text));
        if (link == null) {
            return null;
        }
        link.click();
        return asPage(wizardPageClass);
    }

    public String getTitle2() {
        WebElement title2 = findElementWithTimeout(By.xpath("//h2"));
        if (title2 == null) {
            return null;
        }
        return title2.getText();
    }

}
