/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

public class TestFileUtils {

    @Test
    public void testReadFromStreamWithPredefinedData() throws IOException {
        final byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 123, 3, 3, 4, 5, 2, 13, 34, 5, 56, 31, 34, 5, 65, 3, 4, 34,
                3, 4, 34, 34, 24, 3, 1, 65, 67, 68, 7, 58, 7, 8, 75, 98, 7, 9, 5, 7, 45, 7, 43, 6, };

        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = IOUtils.toByteArray(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    @Test
    public void testReadFromStreamWithGeneratedData() throws IOException {
        int n = 10000;
        final byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i % 256);
        }
        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = IOUtils.toByteArray(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    @Test
    public void testReadFromStreamWithLongGeneratedData() throws IOException {
        int n = 1000000; // 1M
        final byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i % 256);
        }
        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = IOUtils.toByteArray(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    @Test
    public void testReadFromStreamWithVLongGeneratedData() throws IOException {
        int n = 0xFFFFFF; // 16M
        final byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            data[i] = (byte) (i % 256);
        }
        final InputStream is = new ByteArrayInputStream(data);
        final byte[] readData = IOUtils.toByteArray(is);

        assertEquals(data.length, readData.length);
        assertTrue(Arrays.equals(data, readData));
    }

    @Test
    public void testGetRessourceFromUrl() {
        // testing resources contained in paths with space
        String testFilename = FileUtils.getResourcePathFromContext("test-xmap.xml");
        assertFalse(testFilename.equals(""));

        // TODO: create a temp file with blank
        // the main problem is to access it with a getResource
        // so that the blanks are encoded
        File file = FileUtils.getResourceFileFromContext("test-xmap.xml");
        assertNotNull(file);
    }

    @Test
    public void testFilePathMethods() {
        String path, testPath;
        if (SystemUtils.IS_OS_WINDOWS) {
            path = "\\a\\b\\c\\d.pdf";
            testPath = "\\a\\b\\c";
        } else {
            path = "/a/b/c/d.pdf";
            testPath = "/a/b/c";
        }
        assertEquals(testPath, FileUtils.getParentPath(path));
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

    @Test
    public void testFileContentEquals() {
        assertTrue(FileUtils.areFilesContentEquals(null, null));

        String fileContent1 = "Hello\nWorld";
        String fileContent1b = "Hello\nWorld";
        String fileContent2 = "Hello\r\nWorld";
        String fileContent3 = "Hello \nWorld";

        assertTrue(FileUtils.areFilesContentEquals(fileContent1, fileContent2));
        assertTrue(FileUtils.areFilesContentEquals(fileContent2, fileContent1));
        assertTrue(FileUtils.areFilesContentEquals(fileContent1, fileContent1b));

        assertFalse(FileUtils.areFilesContentEquals(fileContent1, null));
        assertFalse(FileUtils.areFilesContentEquals(fileContent2, fileContent3));
    }

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Test
    public void testGetSafeFilename() {
        assertEquals("my-image.png", FileUtils.getSafeFilename("my-image.png"));
        assertEquals("_", FileUtils.getSafeFilename(".."));
        assertEquals("tmp___2349_876398___foo.png", FileUtils.getSafeFilename("tmp/../2349:876398/*/foo.png"));
        assertEquals("_tmp___2349_876398___foo.png", FileUtils.getSafeFilename("\\tmp\\..\\2349:876398\\*\\foo.png"));
    }

}
