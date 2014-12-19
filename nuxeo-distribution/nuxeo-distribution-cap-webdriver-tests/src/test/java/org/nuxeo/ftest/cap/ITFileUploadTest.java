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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;

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

        // Clean up repository
        deleteWorkspace(fileDocumentBasePage, wsTitle);

        // Logout
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

        // Check file is still there and filename is present
        assertEquals("tempKeep", creationPageAfterError.getSelectedOption());
        String filename = creationPageAfterError.getSelectedFilename();
        assertNotNull(filename);
        assertTrue("Wrong uploaded file name '" + filename + "', expected it to contain '" + filePrefix + "'",
                filename.contains(filePrefix));

        creationPageAfterError.titleTextInput.sendKeys("File title");
        creationPageAfterError.create();
        FileDocumentBasePage fileDocumentBasePage = asPage(FileDocumentBasePage.class);
        // Check uploaded file name
        String uploadedFileName = fileDocumentBasePage.getFileSummaryTab().getMainContentFileText();
        assertTrue("Wrong uploaded file name '" + uploadedFileName + "', expected it to contain '" + filePrefix + "'",
                uploadedFileName.contains(filePrefix));

        // Clean up repository
        deleteWorkspace(fileDocumentBasePage, wsTitle);

        // Logout
        logout();
    }

}
