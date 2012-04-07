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
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;

/**
 * Test file upload in Nuxeo DM.
 */
public class ITFileUpload extends AbstractTest {

    @Test
    public void testFileUpload() throws Exception {
        // Get the the content tab and go to the existing Workspace root folder
        DocumentBasePage documentBasePage = login().getContentTab().goToDocument(
                "Workspaces");

        // create a new workspace in there named workspace1
        WorkspaceFormPage workspaceCreationFormPage = documentBasePage.getWorkspacesContentTab().getWorkspaceCreatePage();
        DocumentBasePage workspacePage = workspaceCreationFormPage.createNewWorkspace(
                "workspace1", "a workspace description");

        // create a new File document by clicking on New and filling file
        FileCreationFormPage fileFormPage = workspacePage.getContentTab().getDocumentCreatePage(
                "File", FileCreationFormPage.class);

        // Fill the form and upload the file
        // get a file location from resources
        // String fileToUpload = getFileFromResource("filetoupload.txt");
        // String fileName =
        // fileToUpload.substring(fileToUpload.lastIndexOf(File.separator) + 1);

        // TODO: fix file upload when test ran by maven (jar instead of file
        // issue)
        // FileDocumentBasePage fileDocumentBasePage =
        // fileFormPage.createFileDocument(
        // "file title", "file description", fileToUpload);
        FileDocumentBasePage fileDocumentBasePage = fileFormPage.createFileDocument(
                "file title", "file description", null);

        // String uploadedFileName =
        // fileDocumentBasePage.getFileSummaryTab().getMainContentFileText();
        // assertTrue("The uploaded file name " + uploadedFileName
        // + " didn't match the updated file name",
        // uploadedFileName.contains(fileName));

        // cleaning
        documentBasePage = fileDocumentBasePage.getNavigationSubPage().goToDocument(
                "Workspaces");
        documentBasePage = documentBasePage.getContentTab().removeDocument(
                "workspace1");
        // disconnect
        logout();
    }

    protected String getFileFromResource(String filePath)
            throws URISyntaxException {

        // TODO: fix file upload when coming from a jar
        // File file = new File(
        // Thread.currentThread().getContextClassLoader().getResource(
        // filePath).toURI());
        // assertTrue(file.exists());
        //
        // return file.getAbsolutePath();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL fileUrl = cl.getResource(filePath);
        assertEquals("file", fileUrl.getProtocol());
        return fileUrl.getPath();
    }

}
