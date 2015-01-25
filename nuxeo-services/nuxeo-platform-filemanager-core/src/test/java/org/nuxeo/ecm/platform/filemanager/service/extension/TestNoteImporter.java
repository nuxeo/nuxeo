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

        // MIME type with invalid charset -> autodetect
        blob = Blobs.createBlob(new byte[] { (byte) 0xe9 }, "text/plain; charset=utf-8; x=y");
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());
    }

}
