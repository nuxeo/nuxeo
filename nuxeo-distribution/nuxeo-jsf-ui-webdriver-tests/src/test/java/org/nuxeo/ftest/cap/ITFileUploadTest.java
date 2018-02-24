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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 *     Yannis JULIENNE
 */
package org.nuxeo.ftest.cap;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.FileWidgetElement.InputFileChoice;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.FileEditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.nuxeo.functionaltests.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test file upload in Nuxeo DM.
 */
public class ITFileUploadTest extends AbstractTest {

    protected static final String WORKSPACE_TITLE = ITFileUploadTest.class.getSimpleName() + "_WorkspaceTitle_"
            + new Date().getTime();

    protected static String wsId;

    @Before
    public void before() {
        wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
        wsId = null;
    }

    @Test
    public void testFileUpload() throws Exception {
        login();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage fileDocumentBasePage = asPage(DocumentBasePage.class).createFile("File title",
                "File description", true, filePrefix, ".txt", "Webdriver test file content.");

        // Check uploaded file name
        String uploadedFileName = fileDocumentBasePage.getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        // Check removal of file
        WebElement sumContent = Locator.findElementWithTimeout(By.xpath("//div[@class=\"content_block\"]"));
        assertNotNull(sumContent);
        String sumContentText = sumContent.getText();
        assertNotNull(sumContentText);
        assertFalse(sumContentText.contains("Drop files here"));
        assertTrue(sumContentText.contains(uploadedFileName));

        FileEditTabSubPage editPage = fileDocumentBasePage.getEditTab(FileEditTabSubPage.class);
        editPage.getFileWidgetElement().removeFile();
        fileDocumentBasePage = editPage.save().asPage(FileDocumentBasePage.class);

        sumContent = Locator.findElementWithTimeout(By.xpath("//div[@class=\"content_block\"]"));
        assertNotNull(sumContent);
        sumContentText = sumContent.getText();
        assertNotNull(sumContentText);
        assertTrue(sumContentText.contains("Drop files here"));
        assertFalse(sumContentText.contains(uploadedFileName));

        logout();
    }

    /**
     * Non-regression test for NXP-15638
     *
     * @since 7.1
     */
    @Test
    public void testFileUploadOnValidationError() throws Exception {
        login();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        // do not fill the title: expect a validation error to occur
        FileCreationFormPage fileCreationFormPage = asPage(DocumentBasePage.class).getContentTab()
                                                                                  .getDocumentCreatePage("File",
                                                                                          FileCreationFormPage.class);
        FileCreationFormPage creationPageAfterError = fileCreationFormPage.createFileDocumentWithoutTitle(filePrefix,
                ".txt", "Webdriver test file content.");

        // Check validation error
        assertEquals("Value is required.", creationPageAfterError.getTitleMessage());
        assertEquals("Please correct errors.", creationPageAfterError.getErrorFeedbackMessage());

        // Check file is still there and filename is present
        assertEquals(InputFileChoice.tempKeep.name(), creationPageAfterError.getSelectedOption());
        String filename = creationPageAfterError.getSelectedFilename();
        assertNotNull(filename);
        assertTrue("Wrong uploaded file name '" + filename + "', expected it to contain '" + filePrefix + "'",
                filename.contains(filePrefix));

        creationPageAfterError.createDocument("File title", null);
        // Check uploaded file name
        String uploadedFileName = asPage(FileDocumentBasePage.class).getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        logout();
    }

    /**
     * Non-regression test for NXP-21468
     *
     * @since 10.1
     */
    @Test
    public void testFileUploadOnValidationError2() throws Exception {
        login();
        open(String.format(NXDOC_URL_FORMAT, wsId));

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = asPage(DocumentBasePage.class).createFile("File title", "File description",
                true, filePrefix, ".txt", "Webdriver test file content.");

        // Check uploaded file name
        String uploadedFileName = filePage.getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        // Edit and check the "upload" option without filling a blob
        FileEditTabSubPage fileEditPage = filePage.getEditTab(FileEditTabSubPage.class);
        fileEditPage.getFileWidgetElement().uploadFile(null);
        fileEditPage.save();
        fileEditPage = asPage(FileEditTabSubPage.class);

        // Check validation error
        assertEquals("Empty file", fileEditPage.getSelectedFileErrorMessage());
        assertEquals("Please correct errors.", fileEditPage.getErrorFeedbackMessage());

        // Check file is still there and filename is present
        assertEquals(InputFileChoice.upload.name(), fileEditPage.getSelectedOption());
        assertTrue(fileEditPage.getFileWidgetElement().hasChoice(InputFileChoice.keep));
        String filename = fileEditPage.getSelectedFilename();
        assertNotNull(filename);
        assertTrue("Wrong uploaded file name '" + filename + "', expected it to contain '" + filePrefix + "'",
                filename.contains(filePrefix));

        // check the "keep" option again
        fileEditPage.getFileWidgetElement().keepFile();
        fileEditPage.save();

        // Check uploaded file name again
        uploadedFileName = asPage(FileDocumentBasePage.class).getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        logout();
    }

}
