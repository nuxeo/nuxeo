/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.impl.blob;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// XXX: Second blob testcase -> merge it with the other one ?
public class TestBlob2 {

    @Test
    public void testStringContentSource() throws Exception {
        Blob blob = new StringBlob("some content", "text/plain", "UTF-8");
        checkBlob(blob);
    }

    @Test
    public void testByteArrayContentSource() throws Exception {
        Blob blob = new ByteArrayBlob("some content".getBytes("UTF-8"), "text/plain", "UTF-8");
        checkBlob(blob);
    }

    @Test
    public void testFileContentSource() throws Exception {
        File file = Framework.createTempFile("~test_content_source", ".txt");
        file.deleteOnExit();
        FileWriter out = new FileWriter(file);
        out.write("some content");
        out.close();

        Blob blob = Blobs.createBlob(file, "text/plain", "UTF-8");
        checkBlob(blob);

        file.delete();
    }

    @Test
    public void testURLContentSource() throws Exception {
        File file = Framework.createTempFile("~test_content_source", ".txt");
        file.deleteOnExit();
        FileWriter out = new FileWriter(file);
        out.write("some content");
        out.close();

        Blob blob = new URLBlob(file.toURI().toURL(), "text/plain", "UTF-8");
        checkBlob(blob);

        file.delete();
    }

    private static void checkBlob(Blob blob) throws IOException {
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals("some content", blob.getString());
        assertTrue(Arrays.equals("some content".getBytes(), blob.getByteArray()));

        try (InputStream in = blob.getStream()) {
            String result = IOUtils.toString(in, UTF_8);
            assertEquals("some content", result);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        blob.transferTo(baos);
        assertEquals("some content", new String(baos.toByteArray()));
    }

    @Test
    public void testEncoding() {
        // TODO
    }

}
