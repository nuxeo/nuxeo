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
package org.nuxeo.functionaltests.explorer;

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.explorer.pages.AbstractExplorerPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Fragment upload confirmation.
 *
 * @since 11.1
 */
public class UploadConfirmFragment extends AbstractExplorerPage {

    @Required
    @FindBy(xpath = "//input[@name='name']")
    public WebElement nameInput;

    @Required
    @FindBy(xpath = "//input[@name='version']")
    public WebElement versionInput;

    @Required
    @FindBy(xpath = "//input[@id='doImport']")
    public WebElement importButton;

    public UploadConfirmFragment(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        // NOOP
    }

    public void confirmUpload(String newName, String newVersion) {
        if (newName != null) {
            nameInput.clear();
            nameInput.sendKeys(newName);
        }
        if (newVersion != null) {
            versionInput.clear();
            versionInput.sendKeys(newVersion);
        }
        Locator.scrollAndForceClick(importButton);
        assertEquals("Distribution uploaded successfully", driver.findElement(By.xpath("//h1")).getText());
        waitForAsyncWork();
    }

}
