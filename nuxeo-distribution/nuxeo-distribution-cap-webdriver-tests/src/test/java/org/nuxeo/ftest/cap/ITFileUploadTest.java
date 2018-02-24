/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.FileWidgetElement.InputFileChoice;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.FileEditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test file upload in Nuxeo DM.
 */
public class ITFileUploadTest extends AbstractTest {

    @Test
    public void testFileUpload() throws Exception {

        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a test Workspace
        String wsTitle = "WorkspaceTitle_" + new Date().getTime();
        DocumentBasePage testWorkspacePage = createWorkspace(defaultDomainPage, wsTitle, "");

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage fileDocumentBasePage = createFile(testWorkspacePage, "File title", "File description",
                true, filePrefix, ".txt", "Webdriver test file content.");

        // Check uploaded file name
        String uploadedFileName = fileDocumentBasePage.getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        // Check removal of file
        WebElement sumContent = Locator.findElementWithTimeout(By.xpath("//div[@class=\"content_block\"]"));
        assertNotNull(sumContent);
        String sumContentText = sumContent.getText();
        assertNotNull(sumContentText);
        assertFalse(sumContentText.contains("Drop here"));
        assertTrue(sumContentText.contains(uploadedFileName));

        fileDocumentBasePage.getEditTab();
        FileEditTabSubPage editPage = asPage(FileEditTabSubPage.class);
        editPage.getFileWidgetElement().removeFile();
        fileDocumentBasePage = editPage.save().asPage(FileDocumentBasePage.class);

        sumContent = Locator.findElementWithTimeout(By.xpath("//div[@class=\"content_block\"]"));
        assertNotNull(sumContent);
        sumContentText = sumContent.getText();
        assertNotNull(sumContentText);
        assertTrue(sumContentText.contains("Drop here"));
        assertFalse(sumContentText.contains(uploadedFileName));

        // Clean up repository
        deleteWorkspace(fileDocumentBasePage, wsTitle);

        logout();
    }

    /**
     * Non-regression test for NXP-15638
     *
     * @since 7.1
     */
    @Test
    public void testFileUploadOnValidationError() throws Exception {

        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a test Workspace
        String wsTitle = "WorkspaceTitle_" + new Date().getTime();
        DocumentBasePage testWorkspacePage = createWorkspace(defaultDomainPage, wsTitle, "");

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        // do not fill the title: expect a validation error to occur
        FileCreationFormPage fileCreationFormPage = testWorkspacePage.getContentTab().getDocumentCreatePage("File",
                FileCreationFormPage.class);
        FileCreationFormPage creationPageAfterError = fileCreationFormPage.createFileDocumentWithoutTitle(filePrefix,
                ".txt", "Webdriver test file content.");

        // Check validation error
        assertEquals("Value is required", creationPageAfterError.getTitleMessage());
        assertEquals("Please correct errors", creationPageAfterError.getErrorFeedbackMessage());

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

        // Clean up repository
        deleteWorkspace(asPage(FileDocumentBasePage.class), wsTitle);

        logout();
    }

    /**
     * Non-regression test for NXP-21468
     *
     * @since 10.1
     */
    @Test
    public void testFileUploadOnValidationError2() throws Exception {
        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a test Workspace
        String wsTitle = "WorkspaceTitle_" + new Date().getTime();
        DocumentBasePage testWorkspacePage = createWorkspace(defaultDomainPage, wsTitle, "");

        // Create a File with an uploaded blob
        String filePrefix = "NX-Webdriver-test-";
        FileDocumentBasePage filePage = createFile(testWorkspacePage, "File title", "File description", true,
                filePrefix, ".txt", "Webdriver test file content.");

        // Check uploaded file name
        String uploadedFileName = filePage.getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        // Edit and check the "upload" option without filling a blob
        filePage.getEditTab();
        FileEditTabSubPage fileEditPage = asPage(FileEditTabSubPage.class);
        fileEditPage.getFileWidgetElement().uploadFile(null);
        fileEditPage.save();
        fileEditPage = asPage(FileEditTabSubPage.class);

        // Check validation error
        assertEquals("Empty file", fileEditPage.getSelectedFileErrorMessage());
        assertEquals("Please correct errors", fileEditPage.getErrorFeedbackMessage());

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
