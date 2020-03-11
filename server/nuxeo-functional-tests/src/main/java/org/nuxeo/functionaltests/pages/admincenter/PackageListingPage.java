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

public class PackageListingPage extends AbstractPage {

    public PackageListingPage(WebDriver driver) {
        super(driver);
    }

    public WebElement getPackageLink(String packageId) {
        String xpath = "id('row_" + packageId + "')//a[contains(@class ,'button')]";
        return findElementWithTimeout(By.xpath(xpath), 20 * 1000);
    }

    public WebElement getPackageDownloadLink(String packageId) {
        WebElement link = getPackageLink(packageId);
        if (link != null) {
            if (link.getText().trim().toLowerCase().startsWith("download")) {
                return link;
            }
        }
        return null;
    }

    public WebElement getPackageInstallLink(String packageId) {
        WebElement link = getPackageLink(packageId);
        if (link != null) {
            if (link.getText().trim().toLowerCase().startsWith("install")) {
                return link;
            }
        }
        return null;
    }

    public WebElement download(String packageId) {
        System.out.println(driver.getCurrentUrl());
        WebElement downloadLink = getPackageDownloadLink(packageId);
        if (downloadLink != null) {
            downloadLink.click();
            return getPackageInstallLink(packageId);
        }
        return null;
    }

    public PackageInstallationScreen getInstallationScreen(String packageId) {
        WebElement installLink = getPackageInstallLink(packageId);
        if (installLink == null) {
            return null;
        }
        installLink.click();
        return asPage(PackageInstallationScreen.class);
    }

    public UpdateCenterPage exit() {
        driver.switchTo().defaultContent();
        return asPage(UpdateCenterPage.class);
    }

}
