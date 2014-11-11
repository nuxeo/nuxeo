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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;

import junit.framework.TestCase;

public class TestNoteImporter extends TestCase {

    public void test() throws Exception {
        Blob blob;

        // already present
        blob = new ByteArrayBlob(new byte[0], null, "somecharset");
        assertNull(NoteImporter.guessEncoding(blob));

        // no MIME type
        blob = new ByteArrayBlob(new byte[0], null, null);
        assertNull(NoteImporter.guessEncoding(blob));

        // unknown MIME type
        blob = new ByteArrayBlob(new byte[0], "foo", null);
        assertNull(NoteImporter.guessEncoding(blob));

        // MIME type with charset
        blob = new ByteArrayBlob(new byte[] { (byte) 0xe9 },
                "text/plain; charset=iso-8859-1; x=y", null);
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());

        // MIME type but no charset -> autodetect latin1
        blob = new ByteArrayBlob(new byte[] { (byte) 0xe9 }, "text/plain", null);
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());

        // MIME type but no charset -> autodetect utf8
        blob = new ByteArrayBlob(new byte[] { (byte) 0xc3, (byte) 0xa9 },
                "text/plain", null);
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());

        // MIME type with invalid charset -> autodetect
        blob = new ByteArrayBlob(new byte[] { (byte) 0xe9 },
                "text/plain; charset=utf-8; x=y", null);
        assertEquals("\u00e9", NoteImporter.guessEncoding(blob));
        assertEquals("text/plain", blob.getMimeType());
    }

}
