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
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.forms.RichEditorElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.NoteDocumentBasePage;
import org.openqa.selenium.WebDriver;

/**
 * Form to create a new Note document.
 *
 * @since 5.9.4
 */
public class NoteCreationFormPage extends DublinCoreCreationDocumentFormPage {

    /**
     * @param driver
     */
    public NoteCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public NoteDocumentBasePage createNoteDocument(String title, String description, boolean defineNote,
            String noteContent) {
        fillCreateNoteForm(title, description, defineNote, noteContent);
        create();
        return asPage(NoteDocumentBasePage.class);
    }

    public NoteCreationFormPage fillCreateNoteForm(String title, String description, boolean defineNote,
        String noteContent) {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);

        if (defineNote) {
            RichEditorElement editor = new RichEditorElement(driver, "document_create:nxl_note:nxw_note_editor");
            editor.setInputValue(noteContent);
        }

        return this;
    }
}
