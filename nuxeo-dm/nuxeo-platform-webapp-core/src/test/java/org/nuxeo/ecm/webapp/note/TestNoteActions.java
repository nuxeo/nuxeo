/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.webapp.note;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestNoteActions {

    protected String simpleNoteWithImageLinks = "<img src=\"http://localhost:8080/nuxeo/nxfile/default/{docId}/files:files/0/file/img.png\" alt=\"\" />"
            + "<p>Another image link</p>"
            + "<img src=\"http://localhost:8080/nuxeo/nxfile/default/{docId}/files:files/1/file/img.png\" alt=\"\" />"
            + "<a href=\"http://localhost:8080/nuxeo/nxpath/default/default-domain/workspaces/testworskspace1/testfolder1/testnote1@view_documents?tabId=&amp;conversationId=0NXMAIN2\">testNote1</a>";

    protected String simpleNoteWithoutImageLinks = "<p>No image link to translate"
            + " here</p>"
            + "<img src=\"http://server:8080/nuxeo/img/img.png\" alt=\"\" />"
            + "<a href=\"http://localhost:8080/nuxeo/nxpath/default/default-domain/workspaces/testworskspace1/testfolder1/testnote1@view_documents?tabId=&amp;conversationId=0NXMAIN2\">testNote1</a>";

    protected NoteActions noteActions;

    @Before
    public void setUp() throws Exception {
        noteActions = new NoteActions();
    }

    @Test
    public void testSimpleNoteWithLink() throws Exception {
        String fromDocId = "live-document-id";
        String note = simpleNoteWithImageLinks.replaceAll("\\{docId\\}", fromDocId);
        assertTrue(noteActions.hasImageLinksToTranslate(note));

        String toDocId = "proxy-document-id";
        String translatedNote = noteActions.translateImageLinks(note, fromDocId, toDocId);
        String expectedTranslatedNote = simpleNoteWithImageLinks.replaceAll("\\{docId\\}", toDocId);
        assertEquals(expectedTranslatedNote, translatedNote);
    }

    @Test
    public void testSimpleNoteWithoutLink() throws Exception {
        String fromDocId = "live-document-id";
        String note = simpleNoteWithoutImageLinks.replaceAll("\\{docId\\}", fromDocId);
        assertFalse(noteActions.hasImageLinksToTranslate(note));

        String toDocId = "proxy-document-id";
        String translatedNote = noteActions.translateImageLinks(note, fromDocId, toDocId);
        String expectedTranslatedNote = simpleNoteWithoutImageLinks.replaceAll("\\{docId\\}", toDocId);
        assertEquals(expectedTranslatedNote, translatedNote);
    }

}
