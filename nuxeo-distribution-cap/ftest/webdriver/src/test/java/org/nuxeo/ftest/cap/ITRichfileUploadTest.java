/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * @since 5.9.3
 */
public class ITRichfileUploadTest extends AbstractTest {

    protected static final String FILES_TAB_ID = "nxw_TAB_FILES_EDIT";

    private static final String NX_UPLOADED_FILES_XPATH = "//div[@class='simpleBox']";

    protected static final String RF_CLEAN_ALL_ID_XPATH = "//div[@id='document_files_edit:upload']//span[@class='rf-fu-btn-cnt-clr']";

    protected static final String RF_FILE_UPLOAD_INPUT_XPATH = "//div[@id='document_files_edit:upload']//input[@class='rf-fu-inp']";

    protected static final String RF_UPLOADED_FILE_ITEMS_XPATH = "//div[@class='rf-fu-lst']//a[@class='rf-fu-itm-lnk']";

    protected static final String STORE_UPLOAD_FILE_INPUT_VALUE_XPATH = "//input[@value='Store uploaded files']";

    public final static String TEST_FILE_NAME = "test1";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    @After
    public void tearDown() throws UserNotConnectedException {
        DocumentBasePage documentBasePage = login();
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

    @Test
    public void testRichFileUpload() throws IOException,
            UserNotConnectedException {
        DocumentBasePage documentBasePage = login();

        // Create test File
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        createFile(workspacePage, TEST_FILE_NAME, "Test File description",
                false, null, null, null);

        // Go to Files tab
        Locator.findElementWithTimeoutAndClick(By.id(FILES_TAB_ID));

        // check that clear all is not visible
        assertFalse(Locator.findElementWithTimeout(By.xpath(RF_CLEAN_ALL_ID_XPATH)).isDisplayed());
        // select a file
        final String mockFile1 = getTmpFileToUploadPath("dummy", "test", "txt");
        Locator.findElementWithTimeout(By.xpath(RF_FILE_UPLOAD_INPUT_XPATH)).sendKeys(mockFile1);
        // check that submit button appears
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check that clear all is visible
        assertTrue(driver.findElement(By.xpath(RF_CLEAN_ALL_ID_XPATH)).isDisplayed());
        // check that there is one item with the clear link
        List<WebElement> clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(1, clearLinks.size());

        // we click the clear link
        clearLinks.get(0).click();
        // check the submit button disappears
        Locator.waitUntilElementNotPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check there is not item
        clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(0, clearLinks.size());
        // check that clear all is not visible
        assertFalse(driver.findElement(By.xpath(RF_CLEAN_ALL_ID_XPATH)).isDisplayed());

        // do it again but with two files and use clear all
        final String mockFile2 = getTmpFileToUploadPath("dummy", "test", "txt");
        final String mockFile3 = getTmpFileToUploadPath("dummy", "test", "txt");
        Locator.findElementWithTimeout(By.xpath(RF_FILE_UPLOAD_INPUT_XPATH)).sendKeys(mockFile2);
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        Locator.findElementWithTimeout(By.xpath(RF_FILE_UPLOAD_INPUT_XPATH)).sendKeys(
                mockFile3);
        // check we have 2 items
        clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(2, clearLinks.size());
        // clear all
        driver.findElement(By.xpath(RF_CLEAN_ALL_ID_XPATH)).click();
        // check submit disappears
        Locator.waitUntilElementNotPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check we have 0 items
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                return driver.findElements(
                        By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH)).size() == 0;
            }
        });
        // check that clear all is not visible
        assertFalse(driver.findElement(By.xpath(RF_CLEAN_ALL_ID_XPATH)).isDisplayed());

        // upload 2 and submit
        final String mockFile4 = getTmpFileToUploadPath("dummy", "test", "txt");
        final String mockFile5 = getTmpFileToUploadPath("dummy", "test", "txt");
        Locator.findElementWithTimeout(By.xpath(RF_FILE_UPLOAD_INPUT_XPATH)).sendKeys(
                mockFile4);
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        Locator.findElementWithTimeout(By.xpath(RF_FILE_UPLOAD_INPUT_XPATH)).sendKeys(
                mockFile5);

        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS,
                TimeUnit.MILLISECONDS).ignoring(StaleElementReferenceException.class);
        Function<WebDriver, Boolean> function = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    driver.findElement(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH)).click();
                    return false;
                } catch (NoSuchElementException e) {
                    return true;
                }
            }
        };
        wait.until(function);

        // check we have 2 uploaded files
        List<WebElement> uploadedFiles = driver.findElements(By.xpath(NX_UPLOADED_FILES_XPATH));
        assertEquals(2, uploadedFiles.size());
        // remove the first one
        uploadedFiles.get(0).findElements(By.tagName("a")).get(0).click();
        Alert confirmRemove = driver.switchTo().alert();
        confirmRemove.accept();
        // check we have 1 uploaded file
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                return driver.findElements(By.xpath(NX_UPLOADED_FILES_XPATH)).size() == 1;
            }
        });

        // reselect file and check Clear All and individual clear are still
        // rerendered correctly
        final String mockFile6 = getTmpFileToUploadPath("dummy", "test", "txt");
        Locator.findElementWithTimeout(By.xpath(RF_FILE_UPLOAD_INPUT_XPATH)).sendKeys(mockFile6);
        // check that submit button appears
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check that clear all is visible
        assertTrue(driver.findElement(By.xpath(RF_CLEAN_ALL_ID_XPATH)).isDisplayed());
        // check that there is one item with the clear link
        clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(1, clearLinks.size());
        // clear all
        Locator.findElementWithTimeoutAndClick(By.xpath(RF_CLEAN_ALL_ID_XPATH));
        // check submit disappears
        Locator.waitUntilElementNotPresent(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        // check we have 0 items
        clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(0, clearLinks.size());

        workspacePage.getAdminCenter();

        // Logout
        logout();
    }
}
