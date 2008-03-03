/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestBlob2.java 28196 2007-12-16 17:48:58Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.*;
import java.util.Arrays;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.Blob;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// XXX: Second blob testcase -> merge it with the other one ?
@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
public class TestBlob2 extends TestCase {

    public void testStringContentSource() throws Exception {
        Blob blob = new StringBlob("some content", "text/plain", "UTF-8");
        checkBlob(blob);
    }

    public void testByteArrayContentSource() throws Exception {
        Blob blob = new ByteArrayBlob("some content".getBytes(), "text/plain", "UTF-8");
        checkBlob(blob);
    }

    public void testFileContentSource() throws Exception {
        File file = File.createTempFile("~test_content_source", ".txt");
        file.deleteOnExit();
        FileWriter out = new FileWriter(file);
        out.write("some content");
        out.close();

        Blob blob = new FileBlob(file, "text/plain", "UTF-8");
        checkBlob(blob);

        file.delete();
    }

    public void testURLContentSource() throws Exception {
        File file = File.createTempFile("~test_content_source", ".txt");
        file.deleteOnExit();
        FileWriter out = new FileWriter(file);
        out.write("some content");
        out.close();

        Blob blob = new URLBlob(file.toURL(), "text/plain", "UTF-8");
        checkBlob(blob);

        file.delete();
    }

    private static void checkBlob(Blob blob) throws IOException {
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals("some content", blob.getString());
        assertTrue(Arrays.equals("some content".getBytes(), blob.getByteArray()));

        InputStream in = blob.getStream();
        String result = StreamBlob.readString(new InputStreamReader(in));
        assertEquals("some content", result);
        in.close();

        Reader reader = blob.getReader();
        result = StreamBlob.readString(reader);
        assertEquals("some content", result);
        reader.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        blob.transferTo(baos);
        assertEquals("some content", new String(baos.toByteArray()));
        StringWriter sw = new StringWriter();
        blob.transferTo(sw);
        assertEquals("some content", sw.toString());
    }

    public void testEncoding() {
        // TODO
    }

}
