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

package org.nuxeo.functionaltests.pages.admincenter;

import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SystemHomePage extends AdminCenterBasePage {

    public static final String HOST_SUBTAB = "Host";

    public static final String SETUP_SUBTAB = "Setup";

    private static final int RESTART_TIMEOUT_MINUTES = 10;

    public SystemHomePage(WebDriver driver) {
        super(driver);
    }

    public LoginPage restart() {
        if (!HOST_SUBTAB.equals(getSelectedSubTab())) {
            selectSubTab(HOST_SUBTAB);
        }
        By restartLocator = By.xpath("//input[@type='submit' and @value='Restart server']");
        findElementWaitUntilEnabledAndClick(restartLocator);

        findElementWithTimeout(By.id("username"), RESTART_TIMEOUT_MINUTES * 60 * 1000);
        return asPage(LoginPage.class);
    }

    public boolean setConfig(String id, String value) {
        if (!SETUP_SUBTAB.equals(getSelectedSubTab())) {
            selectSubTab(SETUP_SUBTAB);
        }
        WebElement input = findElementWithTimeout(By.xpath("//td[@id='" + id + "']/input"));
        if (input != null) {
            input.sendKeys(value);
            return true;
        }
        return false;
    }

    public String getConfig(String id) {
        if (!SETUP_SUBTAB.equals(getSelectedSubTab())) {
            selectSubTab(SETUP_SUBTAB);
        }
        WebElement input = findElementWithTimeout(By.xpath("//td[@id='" + id + "']/input"));
        if (input != null) {
            return input.getAttribute("value");
        }
        return null;
    }

}
