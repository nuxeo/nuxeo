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
import static org.junit.Assert.fail;

import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.functionaltests.AbstractTest;
import org.openqa.selenium.NoSuchElementException;
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

    @FindBy(className = "current")
    public WebElement currentPlatform;

    @FindBy(className = "currentDistrib")
    public WebElement currentDistrib;

    @FindBy(className = "distrib")
    public WebElement firstPersistedDistrib;

    @FindBy(xpath = "//a[@class='distrib']//span[@class='detail']")
    public WebElement firstPersistedDistribVersion;

    @FindBy(linkText = "Contribute to an Extension")
    public WebElement firstExtensionPoints;

    @FindBy(linkText = "Override a Contribution")
    public WebElement firstContributions;

    @FindBy(linkText = "Search Operations")
    public WebElement firstOperations;

    @FindBy(linkText = "Browse Services")
    public WebElement firstServices;

    public ExplorerHomePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        checkTitle("Nuxeo Platform Explorer");
    }

    public void checkCurrentDistrib() {
        assertEquals("Running Platform".toUpperCase(), currentPlatform.getText());
        assertEquals(String.format("%s%s%s/", AbstractTest.NUXEO_URL, URL, SnapshotManager.DISTRIBUTION_ALIAS_CURRENT),
                currentDistrib.getAttribute("href"));
    }

    public void checkFirstPersistedDistrib(String name, String version) {
        assertEquals(String.format("%s %s", name, version), firstPersistedDistrib.getText());
        assertEquals(version, firstPersistedDistribVersion.getText());
        assertEquals(String.format("%s%s%s-%s/", AbstractTest.NUXEO_URL, URL, name, version),
                firstPersistedDistrib.getAttribute("href"));
    }

    public void checkNoDistrib() {
        try {
            currentDistrib.getText();
            fail("No current distrib should be found");
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            firstPersistedDistrib.getText();
            fail("No distrib should be found");
        } catch (NoSuchElementException e) {
            // ok
        }
    }

}
