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
 * $Id$
 */

package org.nuxeo.common.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

public class TestFileUtils extends TestCase {

    public void testReadFromStreamWithPredefinedData() throws IOException {
        final byte[] data = {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 123, 3,
                3, 4, 5, 2, 13, 34, 5, 56, 31, 34, 5, 65, 3, 4, 34, 3, 4, 34,
                34, 24, 3, 1, 65, 67, 68, 7, 58, 7, 8, 75, 98, 7, 9, 5, 7, 45,
                7, 43, 6, };

        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = FileUtils.readBytes(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    public void testReadFromStreamWithGeneratedData() throws IOException {
        int n = 10000;
        final byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i % 256);
        }
        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = FileUtils.readBytes(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    public void testReadFromStreamWithLongGeneratedData() throws IOException {
        int n = 1000000; // 1M
        final byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i % 256);
        }
        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = FileUtils.readBytes(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    public void testReadFromStreamWithVLongGeneratedData() throws IOException {
        int n = 0xFFFFFF; // 16M
        final byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i % 256);
        }
        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = FileUtils.readBytes(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    public void testGetRessourceFromUrl() throws IOException {

        // testing resources contained in paths with space

        String testFilename = FileUtils.getResourcePathFromContext("test-xmap.xml");
        assertFalse(testFilename.equals(""));

        //TODO: create a temp file with blank
        // the main problem is to access it with a getResource
        // so that the blanks are encoded
        File file = FileUtils.getResourceFileFromContext("test-xmap.xml");
        assertNotNull(file);
    }


    public void testFilePathMethods() {
        String path = "/a/b/c/d.pdf";
        assertEquals("/a/b/c", FileUtils.getParentPath(path));
        assertEquals("pdf", FileUtils.getFileExtension(path));
        assertEquals("d.pdf", FileUtils.getFileName(path));
        assertEquals("d", FileUtils.getFileNameNoExt(path));

        path = "a.gif";
        assertNull(FileUtils.getParentPath(path));
        assertEquals("gif", FileUtils.getFileExtension(path));
        assertEquals("a.gif", FileUtils.getFileName(path));
        assertEquals("a", FileUtils.getFileNameNoExt(path));

        path = "a";
        assertNull(FileUtils.getParentPath(path));
        assertNull(FileUtils.getFileExtension(path));
        assertEquals("a", FileUtils.getFileName(path));
        assertEquals("a", FileUtils.getFileNameNoExt(path));
    }

}
