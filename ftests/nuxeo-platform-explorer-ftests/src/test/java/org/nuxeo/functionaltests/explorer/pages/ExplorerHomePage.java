/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.pages;

import static org.junit.Assert.assertEquals;

import org.nuxeo.apidoc.browse.ApiBrowserConstants;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing home at /site/distribution.
 *
 * @since 11.1
 */
public class ExplorerHomePage extends AbstractExplorerPage {

    public static final String URL = "/site/distribution/";

    @Required
    @FindBy(className = "current")
    public WebElement currentPlatform;

    @Required
    @FindBy(className = "distrib")
    public WebElement currentDistrib;

    @Required
    @FindBy(linkText = "Contribute to an Extension")
    public WebElement currentExtensionPoints;

    @Required
    @FindBy(linkText = "Override a Contribution")
    public WebElement currentContributions;

    @Required
    @FindBy(linkText = "Search Operations")
    public WebElement currentOperations;

    @Required
    @FindBy(linkText = "Browse Services")
    public WebElement currentServices;

    public ExplorerHomePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        checkTitle("Nuxeo Platform Explorer");
        assertEquals("Running Platform".toUpperCase(), currentPlatform.getText());
        assertEquals(
                String.format("%s%s%s/", AbstractTest.NUXEO_URL, URL, ApiBrowserConstants.DISTRIBUTION_ALIAS_CURRENT),
                currentDistrib.getAttribute("href"));
    }

    public void checkPersistedDistrib(String distribId) {
        String xpath = String.format("//a[@class='distrib' and contains(@href, '%s%s')]", URL, distribId);
        WebElement distrib = driver.findElement(By.xpath(xpath));
        clickOn(distrib);
        asPage(DistributionHomePage.class).checkHeader(distribId);
    }

}
