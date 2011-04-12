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
