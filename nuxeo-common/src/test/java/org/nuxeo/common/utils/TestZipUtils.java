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
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TestZipUtils {

    @Test
    public void testGetZipContentByFile() throws Exception {
        String path = "test-data/hello.odt";
        File sourceFile = FileUtils.getResourceFileFromContext(path);

        List<String> contentNames = ZipUtils.getEntryNames(sourceFile);
        assertEquals("Number of elements", 9, contentNames.size());

        assertTrue("Contains mimetype file", ZipUtils.hasEntry(sourceFile, "mimetype"));

        InputStream entryContent = ZipUtils.getEntryContentAsStream(sourceFile, "mimetype");
        assertEquals("Mimetype content", "application/vnd.oasis.opendocument.text",
                IOUtils.toString(entryContent, Charsets.UTF_8));
        // need to close returned InputStream
        entryContent.close();

        // direct access to content - No need to close returned InputStream

        String directString = ZipUtils.getEntryContentAsString(sourceFile, "mimetype");
        assertEquals("Mimetype content", "application/vnd.oasis.opendocument.text", directString);

        byte[] bytes = ZipUtils.getEntryContentAsBytes(sourceFile, "mimetype");
        assertEquals("Mimetype file length", 39, bytes.length);
    }

    @Test
    public void testGetZipContentByByStream() throws Exception {
        String path = "test-data/hello.odt";
        File sourceFile = FileUtils.getResourceFileFromContext(path);
        InputStream stream = new FileInputStream(sourceFile);

        List<String> contentNames = ZipUtils.getEntryNames(stream);
        assertEquals("Number of elements", 9, contentNames.size());

        stream = new FileInputStream(sourceFile);
        assertTrue("Contains mimetype file", ZipUtils.hasEntry(stream, "mimetype"));

        stream = new FileInputStream(sourceFile);
        try (InputStream entryContent = ZipUtils.getEntryContentAsStream(stream, "mimetype")) {
            assertEquals("Mimetype content", "application/vnd.oasis.opendocument.text",
                    IOUtils.toString(entryContent, Charsets.UTF_8));
        }

        // direct access to content - No need to close returned InputStream

        stream = new FileInputStream(sourceFile);
        String directString = ZipUtils.getEntryContentAsString(stream, "mimetype");
        assertEquals("Mimetype content", "application/vnd.oasis.opendocument.text", directString);

        stream = new FileInputStream(sourceFile);
        byte[] bytes = ZipUtils.getEntryContentAsBytes(stream, "mimetype");
        assertEquals("Mimetype file length", 39, bytes.length);
    }

    @Test
    public void testGetZipContentBytByURL() throws Exception {
        String path = "test-data/hello.odt";
        File sourceFile = FileUtils.getResourceFileFromContext(path);
        URL url = sourceFile.toURI().toURL();

        List<String> contentNames = ZipUtils.getEntryNames(url);
        assertEquals("Number of elements", 9, contentNames.size());

        assertTrue("Contains mimetype file", ZipUtils.hasEntry(url, "mimetype"));

        try (InputStream entryContent = ZipUtils.getEntryContentAsStream(url, "mimetype")) {
            assertEquals("Mimetype content", "application/vnd.oasis.opendocument.text",
                    IOUtils.toString(entryContent, Charsets.UTF_8));
        }

        // direct access to content - No need to close returned InputStream

        String directString = ZipUtils.getEntryContentAsString(url, "mimetype");
        assertEquals("Mimetype content", "application/vnd.oasis.opendocument.text", directString);

        byte[] bytes = ZipUtils.getEntryContentAsBytes(url, "mimetype");
        assertEquals("Mimetype file length", 39, bytes.length);
    }

}
