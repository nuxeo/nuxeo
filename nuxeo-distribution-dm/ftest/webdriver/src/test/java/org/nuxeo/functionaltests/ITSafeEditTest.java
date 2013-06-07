/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Guillaume Renard
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Safe Edit feature tests.
 *
 * @since 5.7.1
 */
public class ITSafeEditTest extends AbstractTest {

    /**
     * Convenient class to access localstorage of the browser.
     *
     * @since 5.7.1
     */
    public class LocalStorage {
        private final JavascriptExecutor js;

        public LocalStorage(WebDriver webDriver) {
            this.js = (JavascriptExecutor) webDriver;
        }

        public void clearLocalStorage() {
            js.executeScript(String.format("window.localStorage.clear();"));
        }

        public String getItemFromLocalStorage(String key) {
            return (String) js.executeScript(String.format(
                    "return window.localStorage.getItem('%s');", key));
        }

        public String getKeyFromLocalStorage(int key) {
            return (String) js.executeScript(String.format(
                    "return window.localStorage.key('%s');", key));
        }

        public Long getLocalStorageLength() {
            return (Long) js.executeScript("return window.localStorage.length;");
        }

        public boolean isItemPresentInLocalStorage(String item) {
            return !(js.executeScript(String.format(
                    "return window.localStorage.getItem('%s');", item)) == null);
        }

        public void removeItemFromLocalStorage(String item) {
            js.executeScript(String.format(
                    "window.localStorage.removeItem('%s');", item));
        }

        public void setItemInLocalStorage(String item, String value) {
            js.executeScript(String.format(
                    "window.localStorage.setItem('%s','%s');", item, value));
        }

    }

    private static final Log log = LogFactory.getLog(AbstractTest.class);

    // private final static String USERNAME = "jdoe";

    // private final static String PASSWORD = "test";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    private final static String NEW_WORKSPACE_TITLE = "newWorkspaceName";

    private final static String DESCRIPTION_ELT_ID = "document_edit:nxl_heading:nxw_description";

    private final static String TITLE_ELT_ID = "document_edit:nxl_heading:nxw_title";

    private final static String INITIAL_DESCRIPTION = "workspaceDescription";

    private final static String CONFIRM_RESTORE_YES_ELT_ID = "confirmRestoreYes";

    private final static String CONFIRM_RESTORE_SPAN_ELT_ID = "confirmRestore";

    /**
     * workaround to by pass the popup windows which is supposed to prevent the
     * user from leaving the page with unsaved modification.
     *
     * @since 5.7.1
     */
    private void byPassLeavePagePopup() {
        ((JavascriptExecutor) driver).executeScript("window.onbeforeunload = function(e){};");
        ((JavascriptExecutor) driver).executeScript("jQuery(window).unbind('unload');");
    }

    /**
     * This methods checks that once a simple html input is changed within a
     * page, the new value is stored in the browser local storage in case of
     * accidental loose (crash, freeze, network failure). The value can then be
     * restored from the local storage when re-editing the page afterwards.
     *
     * @since 5.7.1
     *
     */
    @Test
    public void testAutoSaveOnChangeAndRestore() throws Exception {
        DocumentBasePage documentBasePage;
        WebElement descriptionElt, titleElt;

        DocumentBasePage s = login();

        documentBasePage = s.getContentTab().goToDocument("Workspaces");

        // Create a new workspace named 'WorkspaceDescriptionModify_{current
        // time}'
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, INITIAL_DESCRIPTION);

        workspacePage.getEditTab();
        LocalStorage localStorage = new LocalStorage(driver);

        String currentDocumentId = getCurrentDocumentId();

        descriptionElt = driver.findElement(By.name(DESCRIPTION_ELT_ID));
        titleElt = driver.findElement(By.name(TITLE_ELT_ID));
        log.debug("1 - " + localStorage.getLocalStorageLength());

        // We change the value of the title
        titleElt.click();
        titleElt.sendKeys(Keys.chord(Keys.CONTROL, "a") + Keys.DELETE
                + NEW_WORKSPACE_TITLE);
        // weird thing in webdriver: we need to call clear on an input of the
        // form to fire an onchange event
        descriptionElt.click();
        descriptionElt.clear();
        log.debug("2 - " + localStorage.getLocalStorageLength());

        // Now must have something saved in the localstorage
        String lsItem = localStorage.getItemFromLocalStorage(currentDocumentId);
        final String lookupString = "\"" + TITLE_ELT_ID + "\":\""
                + NEW_WORKSPACE_TITLE + "\"";

        assertTrue(lsItem != null && lsItem.length() > 0);
        assertTrue(lsItem.contains(lookupString));

        try {
            documentBasePage.getContentTab();
            // Should never occur
            fail("There are unsaved modifications pending and the page can only be left after clicking \"Leave this page\"");
        } catch (UnhandledAlertException e) {
            // Expected behavior
            // The following is a workaround to by pass the popup windows which
            // is supposed to prevent the user from leaving the page with
            // unsaved modification
            log.debug("3 - " + localStorage.getLocalStorageLength());
            byPassLeavePagePopup();
            log.debug("4 - " + localStorage.getLocalStorageLength());
        }

        // We leave the page and get back to it. Since we didn't save the title
        // must be the initial one.
        documentBasePage.getContentTab();
        documentBasePage.getEditTab();
        localStorage = new LocalStorage(driver);
        titleElt = driver.findElement(By.name(TITLE_ELT_ID));
        String titleEltValue = titleElt.getAttribute("value");
        assertTrue(titleEltValue.equals(WORKSPACE_TITLE));
        log.debug("5 - " + localStorage.getLocalStorageLength());

        // We must find in the localstorage an entry matching the previous
        // document which contains the title we edited
        lsItem = localStorage.getItemFromLocalStorage(currentDocumentId);
        assertTrue(lsItem.contains(lookupString));
        log.debug("6 - " + localStorage.getLocalStorageLength());

        // We must find the status message asking if we want to restore previous
        // unchanged data and make sure it is visible
        WebElement confirmRestore = driver.findElement(By.id(CONFIRM_RESTORE_SPAN_ELT_ID));
        final String confirmRestoreCssDisplayValue = confirmRestore.getCssValue("display");
        assertTrue(!confirmRestoreCssDisplayValue.equals("none"));

        // Let's restore
        WebElement confirmRestoreYes = driver.findElement(By.id(CONFIRM_RESTORE_YES_ELT_ID));
        confirmRestoreYes.click();

        // We check that the title value has actually been restored
        titleElt = driver.findElement(By.name(TITLE_ELT_ID));
        titleEltValue = titleElt.getAttribute("value");
        assertTrue(titleEltValue.equals(NEW_WORKSPACE_TITLE));

        // We delete the created workspace
        byPassLeavePagePopup();
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

}
