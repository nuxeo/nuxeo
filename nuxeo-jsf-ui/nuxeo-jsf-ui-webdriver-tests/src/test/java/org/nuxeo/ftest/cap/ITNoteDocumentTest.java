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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.TestConstants.TEST_WORKSPACE_URL;
import static org.nuxeo.functionaltests.Constants.NOTE_TYPE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.NoteCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.NoteSummaryTabSubPage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Test document note creation and edition.
 *
 * @since 5.9.4
 */
public class ITNoteDocumentTest extends AbstractTest {

    protected final static String CONTENT_NOTE = "Test of creating a note";

    protected final static String CONTENT_NOTE_2 = "Dummy text for test";

    protected final static String CONTENT_NOTE_EDITED = "Test of editing a note";

    protected final static String NOTE_TITLE = "Note 1";

    protected final static String NOTE_DESCRIPTION = "Description note 1";

    protected static final String NOTE_VERSION_CREATED = "VERSION 0.1";

    protected static final String NOTE_VERSION_EDITED = "VERSION 1.0";

    @Before
    public void before() {
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testCreateSimpleNote() throws Exception {
        login();
        open(TEST_WORKSPACE_URL);
        // Create a Note
        NoteDocumentBasePage noteDocumentPage = asPage(DocumentBasePage.class).createNote(NOTE_TITLE, NOTE_DESCRIPTION,
                true, CONTENT_NOTE);
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();

        // Test the result
        assertEquals(CONTENT_NOTE, noteSummaryPage.getTextBlockContentText());

        logout();
    }

    @Test
    public void testCreateComplexNote() throws Exception {
        login();
        open(TEST_WORKSPACE_URL);

        // Get the Note creation form
        NoteCreationFormPage noteCreationPage = asPage(DocumentBasePage.class).getContentTab().getDocumentCreatePage(
                "Note", NoteCreationFormPage.class);
        noteCreationPage.titleTextInput.sendKeys(NOTE_TITLE);
        noteCreationPage.descriptionTextInput.sendKeys(NOTE_DESCRIPTION);

        // Get the editor and define the content
        RichEditorElement editor = new RichEditorElement(driver, "document_create:nxl_note:nxw_note_editor");
        editor.clickBoldButton();
        editor.setInputValue(CONTENT_NOTE);
        editor.clickBoldButton();
        editor.clickItalicButton();
        editor.setInputValue(CONTENT_NOTE_2);
        noteCreationPage.create();

        NoteDocumentBasePage noteDocumentPage = asPage(NoteDocumentBasePage.class);

        // Check the result
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();
        WebElement textBlock = noteSummaryPage.getTextBlockViewField();
        WebElement boldTextContent = textBlock.findElement(By.cssSelector("strong"));
        WebElement italicTextContent = textBlock.findElement(By.cssSelector("em"));

        String expectedText = CONTENT_NOTE.concat(CONTENT_NOTE_2);

        assertEquals(expectedText, textBlock.getText().trim().replace("\n", ""));
        assertEquals(CONTENT_NOTE, boldTextContent.getText());
        assertEquals(CONTENT_NOTE_2, italicTextContent.getText());

        logout();
    }

    @Test
    public void testCreateAndEditNote() throws Exception {
        login();
        open(TEST_WORKSPACE_URL);

        // Create a Note
        NoteDocumentBasePage noteDocumentPage = asPage(DocumentBasePage.class).createNote(NOTE_TITLE, NOTE_DESCRIPTION,
                true, CONTENT_NOTE);

        // Check version
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();
        assertEquals(NOTE_VERSION_CREATED, noteSummaryPage.getVersionNumberText());

        // Edit the note
        EditTabSubPage editTab = noteDocumentPage.getEditTab();
        RichEditorElement editor = new RichEditorElement(driver, "document_edit:nxl_note:nxw_note_editor");
        editor.setInputValue(CONTENT_NOTE_EDITED);
        editTab.edit(null, null, EditTabSubPage.MAJOR_VERSION_INCREMENT_VALUE);

        // Check the result
        String expectedText = String.format("%s%s", CONTENT_NOTE_EDITED, CONTENT_NOTE);
        assertEquals(expectedText, noteSummaryPage.getTextBlockContentText().replace("\n", ""));
        assertEquals(NOTE_VERSION_EDITED, noteSummaryPage.getVersionNumberText());

        logout();
    }

    /**
     * Tests canceling note creation with a safe edit prompt.
     *
     * @since 8.3
     * @throws Exception
     */
    @Test
    public void testCancelNote() throws UserNotConnectedException {
        try {
            login();
            open(TEST_WORKSPACE_URL);

            // fill the node creation form but do not submit it
            asPage(DocumentBasePage.class).getContentTab()
                                          .getDocumentCreatePage(NOTE_TYPE, NoteCreationFormPage.class)
                                          .fillCreateNoteForm(NOTE_TITLE, NOTE_DESCRIPTION, true, CONTENT_NOTE);

            // go back to the parent folder
            DocumentBasePage.clickOnBreadcrumbElement(driver, TEST_WORKSPACE_TITLE);

            Alert alert = driver.switchTo().alert();
            assertEquals(
                    "This page is asking you to confirm that you want to leave - data you have entered may not be saved.",
                    alert.getText());
            alert.accept();

            // test the result
            asPage(DocumentBasePage.class).checkDocTitle(TEST_WORKSPACE_TITLE);
        } finally {

            logout();
        }

    }
}
