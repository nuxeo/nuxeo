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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.NoteCreationFormPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.NoteSummaryTabSubPage;
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

    @Test
    public void testCreateSimpleNote() throws Exception {
        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a test Workspace
        DocumentBasePage testWorkspacePage = initRepository(defaultDomainPage);

        // Create a Note
        NoteDocumentBasePage noteDocumentPage = createNote(testWorkspacePage,
                NOTE_TITLE, NOTE_DESCRIPTION, true, CONTENT_NOTE);
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();

        // Test the result
        assertEquals(CONTENT_NOTE, noteSummaryPage.getTextBlockContentText());

        // Clean up repository
        cleanRepository(noteDocumentPage);

        logout();
    }

    @Test
    public void testCreateComplexNote() throws Exception {
        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a test Workspace
        DocumentBasePage testWorkspacePage = initRepository(defaultDomainPage);

        // Get the Note creation form
        NoteCreationFormPage noteCreationPage = testWorkspacePage.getContentTab().getDocumentCreatePage(
                "Note", NoteCreationFormPage.class);
        noteCreationPage.titleTextInput.sendKeys(NOTE_TITLE);
        noteCreationPage.descriptionTextInput.sendKeys(NOTE_DESCRIPTION);

        // Get the editor and define the content
        RichEditorElement editor = new RichEditorElement(driver, "document_create:nxl_note:nxw_note");
        editor.clickBoldButton();
        editor.insertContent(CONTENT_NOTE);
        editor.clickBoldButton();
        editor.clickItalicButton();
        editor.insertContent(CONTENT_NOTE_2);
        noteCreationPage.create();

        NoteDocumentBasePage noteDocumentPage = asPage(NoteDocumentBasePage.class);

        // Check the result
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();
        WebElement textBlock = noteSummaryPage.getTextBlockViewField();
        WebElement boldTextContent = textBlock.findElement(By.cssSelector("strong"));
        WebElement italicTextContent = textBlock.findElement(By.cssSelector("em"));

        String expectedText = CONTENT_NOTE.concat(CONTENT_NOTE_2);

        assertEquals(expectedText, textBlock.getText());
        assertEquals(CONTENT_NOTE, boldTextContent.getText());
        assertEquals(CONTENT_NOTE_2, italicTextContent.getText());

        // Clean up repository
        cleanRepository(noteDocumentPage);

        logout();
    }

    @Test
    public void testCreateAndEditNote() throws Exception {
        // Login as Administrator
        DocumentBasePage defaultDomainPage = login();

        // Init repository with a test Workspace
        DocumentBasePage testWorkspacePage = initRepository(defaultDomainPage);

        // Create a Note
        NoteDocumentBasePage noteDocumentPage = createNote(testWorkspacePage,
                NOTE_TITLE, NOTE_DESCRIPTION, true, CONTENT_NOTE);
        // Edit the note
        EditTabSubPage editTab = noteDocumentPage.getEditTab();
        RichEditorElement editor = new RichEditorElement(driver, "document_edit:nxl_note:nxw_note");
        editor.insertContent(CONTENT_NOTE_EDITED);
        editTab.save();

        // Check the result
        String expectedText = String.format("%s%s", CONTENT_NOTE_EDITED, CONTENT_NOTE);
        NoteSummaryTabSubPage noteSummaryPage = noteDocumentPage.getNoteSummaryTab();
        assertEquals(expectedText, noteSummaryPage.getTextBlockContentText());

        // Clean up repository
        cleanRepository(noteDocumentPage);

        logout();
    }
}
