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
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.forms.RichEditorElement;
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

    public NoteDocumentBasePage createNoteDocument(String title,
            String description, boolean defineNote, String noteContent) {
        titleTextInput.sendKeys(title);
        descriptionTextInput.sendKeys(description);

        if (defineNote) {
            RichEditorElement editor = new RichEditorElement(driver,
                    "document_create:nxl_note:nxw_note");
            editor.insertContent(noteContent);
        }

        create();
        return asPage(NoteDocumentBasePage.class);
    }
}
