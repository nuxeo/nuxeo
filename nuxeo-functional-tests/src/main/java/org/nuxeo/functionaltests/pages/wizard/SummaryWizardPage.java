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

import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SummaryWizardPage extends WizardPage {

    private static final int RESTART_TIMEOUT_MINUTES = 10;

    public SummaryWizardPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage restart() {
        nav(WizardPage.class, "Start Nuxeo");
        try {
            findElementWithTimeout(By.id("username"), RESTART_TIMEOUT_MINUTES * 60 * 1000);
            return asPage(LoginPage.class);
        } catch (NoSuchElementException e) {
            String currentUrl = driver.getCurrentUrl();
            throw new NoSuchElementException(String.format("Unable to find login screen after %s minutes, currentUrl=%s", RESTART_TIMEOUT_MINUTES, currentUrl));
        }
    }

    public String getRegistration() {
        WebElement el = findElementWithTimeout(By.id("CLID"));
        if (el!=null) {
            return el.getText();
        }
        return null;
    }

}
