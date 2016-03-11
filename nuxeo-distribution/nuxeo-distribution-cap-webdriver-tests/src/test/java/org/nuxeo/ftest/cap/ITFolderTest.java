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
 *     Kevin Leturc
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ftest.cap.TestConstants.TEST_FOLDER_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.functionaltests.Constants.FOLDER_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.FolderDocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.FolderEditTabSubPage;

/**
 * Test Folder functions.
 *
 * @since 8.2
 */
public class ITFolderTest extends AbstractTest {

    public static final String FOLDER_DESCRIPTION = "copyright Nuxeo 2007";

    public static final String FOLDER_LANGUAGE = "English (United Kingdom)";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, "John", "Smith", "Nuxeo", "jsmith@nuxeo.com", "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FOLDER_TYPE, TEST_FOLDER_TITLE, null);
        RestHelper.addPermission(TEST_WORKSPACE_PATH, TEST_USERNAME, "Everything");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void changeFolderMetadata() throws Exception {
        // Edit folder metadata
        FolderEditTabSubPage folderEditPage = loginAsTestUser().goToWorkspaces()
                                                               .goToDocumentWorkspaces()
                                                               .getContentTab()
                                                               .goToDocument(TEST_WORKSPACE_TITLE)
                                                               .getContentTab()
                                                               .goToDocument(TEST_FOLDER_TITLE)
                                                               .getEditTab(FolderEditTabSubPage.class);
        FolderDocumentBasePage folderContentPage = folderEditPage.setDescription(FOLDER_DESCRIPTION)
                                                                 .setLanguage(FOLDER_LANGUAGE)
                                                                 .save();
        assertEquals(FOLDER_DESCRIPTION,
                driver.findElementById("document_header_layout_form:nxl_document_header:nxw_header_description")
                      .getText());

        folderEditPage = folderContentPage.getEditTab();
        assertEquals(FOLDER_DESCRIPTION, folderEditPage.getDescription());
        assertEquals(FOLDER_LANGUAGE, folderEditPage.getLanguage());

    }

}
