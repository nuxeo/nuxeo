/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Clock;
import org.openqa.selenium.support.ui.SystemClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Representation of a Archived versions sub tab page.
 */
public class ArchivedVersionsSubPage extends DocumentBasePage {

    private static final Log log = LogFactory.getLog(ArchivedVersionsSubPage.class);

    private static final String DELETE_ACTION_ID = "CURRENT_VERSION_SELECTION_DELETE";

    private static final String VIEW_VERSION_ACTION_ID = "VIEW_VERSION";

    private static final String RESTORE_VERSION_ACTION_ID = "RESTORE_VERSION";

    @Required
    @FindBy(id = "document_versions")
    WebElement documentVersions;

    @FindBy(id = "document_versions_form")
    WebElement documentVersionsForm;

    public ArchivedVersionsSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Gets the version labels.
     *
     * @return the version labels
     */
    public List<String> getVersionLabels() {
        List<String> versionLabels = new ArrayList<String>();
        List<WebElement> trElements;
        try {
            trElements = documentVersionsForm.findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
        } catch (NoSuchElementException nsee) {
            return versionLabels;
        }
        for (WebElement trItem : trElements) {
            try {
                WebElement versionLabel = trItem.findElement(By.xpath("td[2]"));
                versionLabels.add(versionLabel.getText());
            } catch (NoSuchElementException e) {
                // Go to next line
            }
        }
        return versionLabels;
    }

    /**
     * Selects a version given its label.
     *
     * @param versionLabel the version label
     * @return the archived versions sub page
     */
    public ArchivedVersionsSubPage selectVersion(String versionLabel) {

        List<WebElement> trElements = documentVersionsForm.findElement(By.tagName("tbody"))
                                                          .findElements(By.tagName("tr"));
        for (WebElement trItem : trElements) {
            try {
                trItem.findElement(By.xpath("td[text()=\"" + versionLabel + "\"]"));
                WebElement checkBox = trItem.findElement(By.xpath("td/input[@type=\"checkbox\"]"));
                Locator.waitUntilEnabledAndClick(checkBox);
                break;
            } catch (NoSuchElementException e) {
                // Go to next line
            }
        }
        return asPage(ArchivedVersionsSubPage.class);
    }

    /**
     * Checks the ability to remove selected versions.
     *
     * @param canRemove true to check if can remove selected versions
     */
    public void checkCanRemoveSelectedVersions(boolean canRemove) {
        checkCanExecuteActionOnSelectedVersions(DELETE_ACTION_ID, canRemove);
    }

    /**
     * Checks the ability to execute the action identified by {@code actionId} on selected versions.
     *
     * @param actionId the action id
     * @param canExecute true to check if can execute action identified by {@code actionId} on selected versions
     */
    public void checkCanExecuteActionOnSelectedVersions(String actionId, boolean canExecute) {
        try {
            findElementAndWaitUntilEnabled(By.xpath("//span[@id=\"" + actionId + "\"]/input"),
                    AbstractTest.LOAD_TIMEOUT_SECONDS * 1000, AbstractTest.AJAX_SHORT_TIMEOUT_SECONDS * 1000);
            if (!canExecute) {
                fail(actionId + " action should not be enabled because there is no version selected.");
            }
        } catch (NotFoundException nfe) {
            if (canExecute) {
                log.error(nfe, nfe);
                fail(actionId + " action should be enabled because there is at least one version selected.");
            }
        }
    }

    /**
     * Removes the selected versions.
     *
     * @return the archived versions sub page
     */
    public ArchivedVersionsSubPage removeSelectedVersions() {

        ArchivedVersionsSubPage archivedVersionsPage = null;
        // As accepting the Delete confirm alert randomly fails to reload the
        // page, we need to repeat the Delete action until it is really taken
        // into account, ie. the "Delete" button is not displayed any more nor
        // enabled.
        Clock clock = new SystemClock();
        long end = clock.laterBy(AbstractTest.LOAD_TIMEOUT_SECONDS * 1000);
        while (clock.isNowBefore(end)) {
            try {
                archivedVersionsPage = executeActionOnSelectedVersions(DELETE_ACTION_ID, true,
                        ArchivedVersionsSubPage.class, AbstractTest.LOAD_SHORT_TIMEOUT_SECONDS * 1000,
                        AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
            } catch (NotFoundException nfe) {
                if (archivedVersionsPage == null) {
                    break;
                }
                return archivedVersionsPage;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new WebDriverException("Couldn't remove selected versions");
    }

    /**
     * Executes the action identified by {@code actionId} on selected versions.
     *
     * @param <T> the generic type of the page to return
     * @param actionId the action id
     * @param isConfirm true if the action needs a javascript confirm
     * @param pageClass the class of the page to return
     * @param findElementTimeout the find element timeout in milliseconds
     * @param waitUntilEnabledTimeout the wait until enabled timeout in milliseconds
     * @return the page displayed after the action execution
     */
    public <T> T executeActionOnSelectedVersions(String actionId, boolean isConfirm, Class<T> pageClass,
            int findElementTimeout, int waitUntilEnabledTimeout) {
        Locator.findElementWaitUntilEnabledAndClick(null, By.xpath("//span[@id=\"" + actionId + "\"]/input"),
                findElementTimeout, waitUntilEnabledTimeout);
        if (isConfirm) {
            Alert alert = driver.switchTo().alert();
            assertEquals("Delete selected document(s)?", alert.getText());
            alert.accept();
        }
        return asPage(pageClass);
    }

    /**
     * Views the version with label {@code versionLabel}.
     *
     * @param versionLabel the version label
     * @return the version page
     */
    public DocumentBasePage viewVersion(String versionLabel) {
        return executeActionOnVersion(versionLabel, VIEW_VERSION_ACTION_ID);
    }

    /**
     * Restores the version with label {@code versionLabel}.
     *
     * @param versionLabel the version label
     * @return the restored version page
     */
    public DocumentBasePage restoreVersion(String versionLabel) {
        return executeActionOnVersion(versionLabel, RESTORE_VERSION_ACTION_ID);
    }

    /**
     * Executes the action identified by {@code actionId} on the version with label {@code versionLabel}.
     *
     * @param versionLabel the version label
     * @param actionId the action id
     * @return the page displayed after the action execution
     */
    public DocumentBasePage executeActionOnVersion(String versionLabel, String actionId) {

        List<WebElement> trElements = documentVersionsForm.findElement(By.tagName("tbody"))
                                                          .findElements(By.tagName("tr"));
        for (WebElement trItem : trElements) {
            try {
                trItem.findElement(By.xpath("td[text()=\"" + versionLabel + "\"]"));
                WebElement actionButton = trItem.findElement(By.xpath("td/span[@id=\"" + actionId + "\"]/input"));
                Locator.waitUntilEnabledAndClick(actionButton);
                break;
            } catch (NoSuchElementException e) {
                // Go to next line
            }
        }
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 8.3
     */
    public String getDocumentVersionsText() {
        return documentVersions.getText();
    }
}
