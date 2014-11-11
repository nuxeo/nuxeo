/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.webapp.note;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestNoteActions extends TestCase {

    protected String simpleNoteWithImageLinks = "<img src=\"http://localhost:8080/nuxeo/nxfile/default/{docId}/files:files/0/file/img.png\" alt=\"\" />"
            + "<p>Another image link</p>"
            + "<img src=\"http://localhost:8080/nuxeo/nxfile/default/{docId}/files:files/1/file/img.png\" alt=\"\" />"
            + "<a href=\"http://localhost:8080/nuxeo/nxpath/default/default-domain/workspaces/testworskspace1/testfolder1/testnote1@view_documents?tabId=&amp;conversationId=0NXMAIN2\">testNote1</a>";

    protected String simpleNoteWithoutImageLinks = "<p>No image link to translate" +
            " here</p>"
            + "<img src=\"http://server:8080/nuxeo/img/img.png\" alt=\"\" />"
            + "<a href=\"http://localhost:8080/nuxeo/nxpath/default/default-domain/workspaces/testworskspace1/testfolder1/testnote1@view_documents?tabId=&amp;conversationId=0NXMAIN2\">testNote1</a>";

    protected NoteActions noteActions;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        noteActions = new NoteActions();
    }

    public void testSimpleNoteWithLink() throws Exception {
        String fromDocId = "live-document-id";
        String note = simpleNoteWithImageLinks.replaceAll("\\{docId\\}",
                fromDocId);
        assertTrue(noteActions.hasImageLinksToTranslate(note));

        String toDocId = "proxy-document-id";
        String translatedNote = noteActions.translateImageLinks(note,
                fromDocId, toDocId);
        String expectedTranslatedNote = simpleNoteWithImageLinks.replaceAll(
                "\\{docId\\}", toDocId);
        assertEquals(expectedTranslatedNote, translatedNote);
    }

    public void testSimpleNoteWithoutLink() throws Exception {
        String fromDocId = "live-document-id";
        String note = simpleNoteWithoutImageLinks.replaceAll("\\{docId\\}",
                fromDocId);
        assertFalse(noteActions.hasImageLinksToTranslate(note));

        String toDocId = "proxy-document-id";
        String translatedNote = noteActions.translateImageLinks(note,
                fromDocId, toDocId);
        String expectedTranslatedNote = simpleNoteWithoutImageLinks.replaceAll(
                "\\{docId\\}", toDocId);
        assertEquals(expectedTranslatedNote, translatedNote);
    }

}
