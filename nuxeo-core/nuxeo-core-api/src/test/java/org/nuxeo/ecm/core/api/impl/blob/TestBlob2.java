/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestBlob2.java 28196 2007-12-16 17:48:58Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl.blob;

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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// XXX: Second blob testcase -> merge it with the other one ?
@SuppressWarnings({ "IOResourceOpenedButNotSafelyClosed" })
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
        File file = File.createTempFile("~test_content_source", ".txt");
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
        File file = File.createTempFile("~test_content_source", ".txt");
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
            String result = IOUtils.toString(in);
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
