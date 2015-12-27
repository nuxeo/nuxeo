/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.filemanager.service.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

public class TestNoteImporter {

    @Test
    public void test() throws Exception {
        Blob blob;

        // already present
        blob = Blobs.createBlob(new byte[0], null, "somecharset");
        assertNull(NoteImporter.guessEncoding(blob));

        // no MIME type
        blob = Blobs.createBlob(new byte[0]);
        assertNull(NoteImporter.guessEncoding(blob));

        // unknown MIME type
        blob = Blobs.createBlob(new byte[0], "foo");
        assertNull(NoteImporter.guessEncoding(blob));

        // MIME type with charset
        blob = Blobs.createBlob(new byte[] { (byte) 0xe9 }, "text/plain; charset=iso-8859-1; x=y");
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());

        // MIME type but no charset -> autodetect latin1
        blob = Blobs.createBlob(new byte[] { (byte) 0xe9 }, "text/plain");
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());

        // MIME type but no charset -> autodetect utf8
        blob = Blobs.createBlob(new byte[] { (byte) 0xc3, (byte) 0xa9 }, "text/plain");
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());

        // MIME type but no charset -> autodetect utf-16 with BOM
        blob = Blobs.createBlob(new byte[] { (byte) 0xff, (byte) 0xfe, (byte) 0xe9, (byte) 0x00 }, "text/plain");
        String s = NoteImporter.guessEncoding(blob);
        assertEquals("\u00e9", s);
        assertEquals("text/plain", blob.getMimeType());

        // MIME type with invalid charset -> autodetect
        blob = Blobs.createBlob(new byte[] { (byte) 0xe9 }, "text/plain; charset=utf-8; x=y");
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());
    }

}
