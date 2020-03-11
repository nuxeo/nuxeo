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

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PackageInstallationScreen extends AbstractPage {

    public PackageInstallationScreen(WebDriver driver) {
        super(driver);
    }

    public PackageListingPage start() {
        WebElement start = findElementWithTimeout(By.linkText("Start"));
        if (start != null) {
            start.click();
            WebElement finish = findElementWithTimeout(By.linkText("Finish"));
            if (finish != null) {
                finish.click();
                return asPage(PackageListingPage.class);
            }
        }
        return null;
    }

}
