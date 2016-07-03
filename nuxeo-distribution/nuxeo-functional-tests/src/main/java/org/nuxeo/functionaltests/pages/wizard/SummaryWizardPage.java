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
            throw new NoSuchElementException(
                    String.format("Unable to find login screen after %s minutes, currentUrl=%s",
                            RESTART_TIMEOUT_MINUTES, currentUrl),
                    e);
        }
    }

    public String getRegistration() {
        WebElement el = findElementWithTimeout(By.id("CLID"));
        if (el != null) {
            return el.getText();
        }
        return null;
    }

}
