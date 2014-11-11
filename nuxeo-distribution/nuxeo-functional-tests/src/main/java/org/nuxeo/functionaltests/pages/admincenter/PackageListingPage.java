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
        WebElement link =  getPackageLink(packageId);
        if (link!=null) {
            if (link.getText().trim().toLowerCase().startsWith("download")) {
                return link;
            }
        }
        return null;
    }

    public WebElement getPackageInstallLink(String packageId) {
        WebElement link =  getPackageLink(packageId);
        if (link!=null) {
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
