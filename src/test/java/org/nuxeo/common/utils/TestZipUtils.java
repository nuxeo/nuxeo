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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

public class TestZipUtils extends TestCase {

    public void testGetZipContentByFile() throws Exception {
        String path = "test-data/hello.odt";
        File sourceFile = FileUtils.getResourceFileFromContext(path);

        List<String> contentNames = ZipUtils.getEntryNames(sourceFile);
        assertEquals("Number of elements", 9, contentNames.size());

        assertTrue("Contains mimetype file", ZipUtils.hasEntry(sourceFile, "mimetype"));

        InputStream entryContent = ZipUtils.getEntryContentAsStream(sourceFile,
                "mimetype");
        assertEquals("Mimetype content",
                "application/vnd.oasis.opendocument.text",
                FileUtils.read(entryContent));
        // need to close returned InputStream
        entryContent.close();

        // direct access to content - No need to close returned InputStream

        String directString = ZipUtils.getEntryContentAsString(
                sourceFile, "mimetype");
        assertEquals("Mimetype content",
                "application/vnd.oasis.opendocument.text",
                directString);

        byte[] bytes = ZipUtils.getEntryContentAsBytes(sourceFile,
                "mimetype");
        assertEquals("Mimetype file length", 39, bytes.length);
    }

    public void testGetZipContentByByStream() throws Exception {
        String path = "test-data/hello.odt";
        File sourceFile = FileUtils.getResourceFileFromContext(path);
        InputStream stream = new FileInputStream(sourceFile);

        List<String> contentNames = ZipUtils.getEntryNames(stream);
        assertEquals("Number of elements", 9, contentNames.size());

        stream = new FileInputStream(sourceFile);
        assertTrue("Contains mimetype file", ZipUtils.hasEntry(stream,"mimetype"));

        stream = new FileInputStream(sourceFile);
        InputStream entryContent = ZipUtils.getEntryContentAsStream(stream,
                "mimetype");
        assertEquals("Mimetype content",
                "application/vnd.oasis.opendocument.text",
                FileUtils.read(entryContent));
        // need to close returned InputStream
        entryContent.close();

        // direct access to content - No need to close returned InputStream

        stream = new FileInputStream(sourceFile);
        String directString = ZipUtils.getEntryContentAsString(
                stream, "mimetype");
        assertEquals("Mimetype content",
                "application/vnd.oasis.opendocument.text",
                directString);

        stream = new FileInputStream(sourceFile);
        byte[] bytes = ZipUtils.getEntryContentAsBytes(stream,
                "mimetype");
        assertEquals("Mimetype file length", 39, bytes.length);
    }

    public void testGetZipContentBytByURL() throws Exception {
        String path = "test-data/hello.odt";
        File sourceFile = FileUtils.getResourceFileFromContext(path);
        URL url = sourceFile.toURL();

        List<String> contentNames = ZipUtils.getEntryNames(url);
        assertEquals("Number of elements", 9, contentNames.size());

        assertTrue("Contains mimetype file", ZipUtils.hasEntry(url,
                "mimetype"));

        InputStream entryContent = ZipUtils.getEntryContentAsStream(url,
                "mimetype");
        assertEquals("Mimetype content",
                "application/vnd.oasis.opendocument.text",
                FileUtils.read(entryContent));
        // need to close returned InputStream
        entryContent.close();

        // direct access to content - No need to close returned InputStream

        String directString = ZipUtils.getEntryContentAsString(url,
                "mimetype");
        assertEquals("Mimetype content",
                "application/vnd.oasis.opendocument.text",
                directString);

        byte[] bytes = ZipUtils.getEntryContentAsBytes(url,
                "mimetype");
        assertEquals("Mimetype file length", 39, bytes.length);
    }

}
