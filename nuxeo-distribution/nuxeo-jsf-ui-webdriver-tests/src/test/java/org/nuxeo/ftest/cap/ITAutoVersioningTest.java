/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Constants;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.NoteSummaryTabSubPage;

/**
 * Functional tests for auto versioning
 *
 * @since 9.1
 */
public class ITAutoVersioningTest extends AbstractTest {

    protected final static String DOC_CONTENT = "Initial content";

    protected final static String DOC_CONTENT_EDITED = "Edited content";

    protected final static String NOTE_TITLE = "Note 1";

    protected final static String NOTE_DESC = "Description note 1";

    protected final static String FILE_TITLE = "File 1";

    protected final static String FILE_DESC = "Description file 1";

    protected static final String INITIAL_VERSION = "VERSION 0.0";

    protected static final String UPDATED_VERSION = "VERSION 0.1+";

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, null, null, null, null, "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.addPermission(WORKSPACES_PATH, TEST_USERNAME, SecurityConstants.EVERYTHING);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    @Ignore("The test always fails as in JSF a VersioningOption is always sent, whether we selected one or not")
    public void testCreateAndEditNoteWithAutoVersioning() throws Exception {

        login();
        open(TEST_WORKSPACE_URL);

        // Create a Note
        NoteDocumentBasePage noteDocumentPage = asPage(DocumentBasePage.class).createNote(NOTE_TITLE, NOTE_DESC, true,
                DOC_CONTENT);

        // Check version
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();
        assertEquals(INITIAL_VERSION, noteSummaryPage.getVersionNumberText());

        // Edit the note
        EditTabSubPage editTab = noteDocumentPage.getEditTab();
        RichEditorElement editor = new RichEditorElement(driver, "document_edit:nxl_note:nxw_note_editor");
        editor.setInputValue(DOC_CONTENT_EDITED);
        editTab.edit(null, null, null);

        // Check the result => The minor version should have been incremented
        assertEquals(UPDATED_VERSION, noteSummaryPage.getVersionNumberText());

        logout();
    }

    @Test
    public void testCreateAndEditFileWithAutoVersioning() throws Exception {

        login();
        open(TEST_WORKSPACE_URL);

        // Create a File
        FileDocumentBasePage fileDocumentPage = asPage(DocumentBasePage.class).createFile(FILE_TITLE, FILE_DESC, false,
                "", ".txt", DOC_CONTENT);

        // Check version
        assertEquals(INITIAL_VERSION, fileDocumentPage.getSummaryTab().getVersionNumberText());

        // Edit the file
        fileDocumentPage.getEditTab().edit("New title", "New description", null);

        // Check the result => The minor version should not have changed
        assertEquals(INITIAL_VERSION, fileDocumentPage.getSummaryTab().getVersionNumberText());

        logout();

        loginAsTestUser();
        open(String.format(Constants.NXPATH_URL_FORMAT, WORKSPACES_PATH + TEST_WORKSPACE_TITLE + "/" + FILE_TITLE));

        // Re-Edit the file with a different user
        fileDocumentPage = asPage(FileDocumentBasePage.class);
        fileDocumentPage.getEditTab().edit("New title by test user", "New description by test user", null);

        // Check the result => The minor version should have incremented
        assertEquals(UPDATED_VERSION, fileDocumentPage.getSummaryTab().getVersionNumberText());

        logout();

    }

}
