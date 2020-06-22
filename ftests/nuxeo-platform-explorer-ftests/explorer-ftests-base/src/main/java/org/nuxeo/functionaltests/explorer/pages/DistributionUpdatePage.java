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

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Update page for a distribution.
 *
 * @since 11.2
 */
public class DistributionUpdatePage extends AbstractExplorerPage {

    @Required
    @FindBy(xpath = "//input[@name='" + NuxeoArtifact.TITLE_PROPERTY_PATH + "']")
    public WebElement title;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_NAME + "']")
    public WebElement name;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_VERSION + "']")
    public WebElement version;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_KEY + "']")
    public WebElement key;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_RELEASED + "']")
    public WebElement released;

    @Required
    @FindBy(xpath = "//textarea[@name='" + DistributionSnapshot.PROP_ALIASES + "']")
    public WebElement aliases;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_LATEST_LTS + "']")
    public WebElement latestLTS;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_LATEST_FT + "']")
    public WebElement latestFT;

    @Required
    @FindBy(xpath = "//input[@name='" + DistributionSnapshot.PROP_HIDE + "']")
    public WebElement hidden;

    @Required
    @FindBy(xpath = "//input[@type='submit']")
    public WebElement updateButton;

    public DistributionUpdatePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        checkTitle("Update Distribution");
    }

    public void checkStringValue(String expected, WebElement element) {
        assertEquals(expected, element.getAttribute("value"));
    }

    public void checkCheckBoxValue(boolean expected, WebElement element) {
        assertEquals(expected, element.isSelected());
    }

    public void updateString(WebElement element, String value) {
        element.clear();
        element.sendKeys(value);
    }

    public void updateCheckBox(WebElement element, boolean value) {
        if ((element.isSelected() && !value) || (!element.isSelected() && value)) {
            element.click();
        }
    }

    public void submit() {
        clickOn(updateButton);
    }

}
