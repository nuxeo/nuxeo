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

import static org.junit.Assert.fail;

import java.io.File;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Fragment for both main page and admin page, showing a similar upload form.
 *
 * @since 11.1
 */
public class UploadFragment extends AbstractExplorerPage {

    @Required
    @FindBy(xpath = "//input[@id='archive']")
    public WebElement input;

    @Required
    @FindBy(xpath = "//input[@id='upload']")
    public WebElement upload;

    @Override
    public void check() {
        // NOOP
    }

    public UploadFragment(WebDriver driver) {
        super(driver);
    }

    public void uploadArchive(File file) {
        input.sendKeys(file.getAbsolutePath());
        Locator.scrollAndForceClick(upload);
        waitForAsyncWork();
    }

    public static void checkCanSee() {
        AbstractTest.asPage(UploadFragment.class);
    }

    public static void checkCannotSee() {
        try {
            AbstractTest.driver.findElement(By.xpath("//input[@id='upload']"));
            fail("Should not be able to upload");
        } catch (NoSuchElementException e) {
            // ok
        }
    }

}
