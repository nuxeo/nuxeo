/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
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

/**
 * Representation of a Archived versions sub tab page.
 */
public class ArchivedVersionsSubPage extends DocumentBasePage {

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
            trElements = documentVersionsForm.findElement(By.tagName("tbody")).findElements(
                    By.tagName("tr"));
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

        List<WebElement> trElements = documentVersionsForm.findElement(
                By.tagName("tbody")).findElements(By.tagName("tr"));
        for (WebElement trItem : trElements) {
            try {
                trItem.findElement(By.xpath("td[text()=\"" + versionLabel
                        + "\"]"));
                WebElement checkBox = trItem.findElement(By.xpath("td/input[@type=\"checkbox\"]"));
                checkBox.click();
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
     * Checks the ability to execute the action identified by {@code actionId}
     * on selected versions.
     *
     * @param actionId the action id
     * @param canExecute true to check if can execute action identified by
     *            {@code actionId} on selected versions
     */
    public void checkCanExecuteActionOnSelectedVersions(String actionId,
            boolean canExecute) {
        try {
            findElementAndWaitUntilEnabled(
                    By.xpath("//span[@id=\"" + actionId + "\"]/input"),
                    AbstractTest.LOAD_TIMEOUT_SECONDS * 1000,
                    AbstractTest.AJAX_SHORT_TIMEOUT_SECONDS * 1000);
            if (!canExecute) {
                fail(actionId
                        + " action should not be enabled because there is no version selected.");
            }
        } catch (NotFoundException nfe) {
            if (canExecute) {
                nfe.printStackTrace();
                fail(actionId
                        + " action should be enabled because there is at least one version selected.");
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
                archivedVersionsPage = executeActionOnSelectedVersions(
                        DELETE_ACTION_ID, true, ArchivedVersionsSubPage.class,
                        AbstractTest.LOAD_SHORT_TIMEOUT_SECONDS * 1000,
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
                // ignore
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
     * @param waitUntilEnabledTimeout the wait until enabled timeout in
     *            milliseconds
     * @return the page displayed after the action execution
     */
    public <T> T executeActionOnSelectedVersions(String actionId,
            boolean isConfirm, Class<T> pageClass, int findElementTimeout,
            int waitUntilEnabledTimeout) {
        findElementWaitUntilEnabledAndClick(
                By.xpath("//span[@id=\"" + actionId + "\"]/input"),
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
     * Executes the action identified by {@code actionId} on the version with
     * label {@code versionLabel}.
     *
     * @param versionLabel the version label
     * @param actionId the action id
     * @return the page displayed after the action execution
     */
    public DocumentBasePage executeActionOnVersion(String versionLabel,
            String actionId) {

        List<WebElement> trElements = documentVersionsForm.findElement(
                By.tagName("tbody")).findElements(By.tagName("tr"));
        for (WebElement trItem : trElements) {
            try {
                trItem.findElement(By.xpath("td[text()=\"" + versionLabel
                        + "\"]"));
                WebElement actionButton = trItem.findElement(By.xpath("td/span[@id=\""
                        + actionId + "\"]/input"));
                actionButton.click();
                break;
            } catch (NoSuchElementException e) {
                // Go to next line
            }
        }
        return asPage(DocumentBasePage.class);
    }
}
