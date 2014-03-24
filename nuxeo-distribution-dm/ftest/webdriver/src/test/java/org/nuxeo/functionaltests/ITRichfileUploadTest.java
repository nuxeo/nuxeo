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
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

/**
 * @since 5.9.3
 */
public class ITRichfileUploadTest extends AbstractTest {

    protected static final String FILES_TAB_ID = "nxw_TAB_FILES_EDIT";

    private static final String NX_UPLOADED_FILES_XPATH = "//div[@class='simpleBox']";

    protected static final String RF_CLEAN_ALL_ID_XPATH = "document_files_edit:upload:clean";

    protected static final String RF_FILE_UPLOAD_INPUT_ID = "document_files_edit:upload:file";

    protected static final String RF_UPLOADED_FILE_ITEMS_XPATH = "//div[@id='document_files_edit:upload:fileItems']//a[@class='rich-fileupload-anc ']";

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
        Locator.findElement(By.id(FILES_TAB_ID)).click();

        // select a file
        final String mockFile1 = getTmpFileToUploadPath("dummy", "test", "txt");
        driver.findElement(By.id(RF_FILE_UPLOAD_INPUT_ID)).sendKeys(mockFile1);
        // check that submit button appears
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check that clear all is visible
        driver.findElement(By.id(RF_CLEAN_ALL_ID_XPATH + "2"));
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

        // do it again but with two files and use clear all
        final String mockFile2 = getTmpFileToUploadPath("dummy", "test", "txt");
        final String mockFile3 = getTmpFileToUploadPath("dummy", "test", "txt");
        driver.findElement(By.id(RF_FILE_UPLOAD_INPUT_ID + "0")).sendKeys(
                mockFile2);
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        driver.findElement(By.id(RF_FILE_UPLOAD_INPUT_ID + "1")).sendKeys(
                mockFile3);
        // check we have 2 items
        clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(2, clearLinks.size());
        // clear all
        driver.findElement(By.id(RF_CLEAN_ALL_ID_XPATH + "2")).click();
        // check submit disappears
        Locator.waitUntilElementNotPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check we have 0 items
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                return driver.findElements(
                        By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH)).size() == 0;
            }
        });

        // upload 2 and submit
        final String mockFile4 = getTmpFileToUploadPath("dummy", "test", "txt");
        final String mockFile5 = getTmpFileToUploadPath("dummy", "test", "txt");
        driver.findElement(By.id(RF_FILE_UPLOAD_INPUT_ID + "2")).sendKeys(
                mockFile4);
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        driver.findElement(By.id(RF_FILE_UPLOAD_INPUT_ID + "3")).sendKeys(
                mockFile5);
        Locator.findElement(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH)).click();
        Locator.waitUntilElementNotPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check we have 2 uploaded files
        List<WebElement> uploadedFiles = driver.findElements(By.xpath(NX_UPLOADED_FILES_XPATH));
        assertEquals(2, uploadedFiles.size());
        // remove the first one
        driver.findElement(
                By.id("document_files_edit:files_input:0:files_delete")).click();
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
        driver.findElement(By.id(RF_FILE_UPLOAD_INPUT_ID)).sendKeys(mockFile6);
        // check that submit button appears
        Locator.waitUntilElementPresent(By.xpath(STORE_UPLOAD_FILE_INPUT_VALUE_XPATH));
        // check that clear all is visible
        driver.findElement(By.id(RF_CLEAN_ALL_ID_XPATH + "2"));
        // check that there is one item with the clear link
        clearLinks = driver.findElements(By.xpath(RF_UPLOADED_FILE_ITEMS_XPATH));
        assertEquals(1, clearLinks.size());
        // clear all
        driver.findElement(By.id(RF_CLEAN_ALL_ID_XPATH + "2")).click();
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
