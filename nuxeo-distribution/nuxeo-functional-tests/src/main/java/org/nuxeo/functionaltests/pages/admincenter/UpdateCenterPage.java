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

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UpdateCenterPage extends AdminCenterBasePage {

    @Required
    @FindBy(linkText = "Packages from Nuxeo Marketplace")
    WebElement packagesFromNuxeoMarketPlaceLink;

    @Required
    @FindBy(linkText = "Nuxeo Studio")
    WebElement packagesFromNuxeoStudioLink;

    public UpdateCenterPage(WebDriver driver) {
        super(driver);
    }

    protected static void wait(int nbSeconds) {
        try {
            Thread.sleep(nbSeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public PackageListingPage getPackageListingPage() {
        boolean iframeFound = IFrameHelper.focusOnWEIFrame(driver);
        assert(iframeFound);
        WebElement body = findElementWithTimeout(By.tagName("body")); // wait for IFrame Body
        assert(body != null);
        PackageListingPage page = asPage(PackageListingPage.class);
        WebElement listing = findElementWithTimeout(By.xpath("//table[@class='packageListing']"));
        assert(listing != null);
        return page;
    }

    public UpdateCenterPage getPackagesFromNuxeoMarketPlace() {

        packagesFromNuxeoMarketPlaceLink.click();
        wait(1);
        return asPage(UpdateCenterPage.class);
    }

    public UpdateCenterPage getPackagesFromNuxeoStudio() {
        packagesFromNuxeoStudioLink.click();
        wait(1);
        return asPage(UpdateCenterPage.class);
    }

    public boolean removePlatformFilterOnMarketPlacePage() {
        WebElement chk = findElementWithTimeout(By.xpath("(.//*/input[@type='checkbox'])[2]"));
        if (chk == null) {
            return false;
        }
        if ("true".equals(chk.getAttribute("checked"))) {
            chk.click();
            wait(2);
        }
        return true;
    }
}
