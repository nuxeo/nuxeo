/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.2
 */
public class TestIWorkToPDF extends BaseConverterTest {

    @Test
    public void testiWorkConverter() throws Exception {
        testiWorkConverter("test-docs/hello.pages");
        testiWorkConverter("test-docs/hello.key");
        testiWorkConverter("test-docs/hello.numbers");
    }

    public void testiWorkConverter(String blobPath) throws Exception {
        String converterName = cs.getConverterName("application/vnd.apple.iwork", "application/pdf");
        assertEquals("iwork2pdf", converterName);

        checkConverterAvailability(converterName);

        BlobHolder pagesBH = getBlobFromPath(blobPath);
        pagesBH.getBlob().setMimeType("application/vnd.apple.iwork");

        BlobHolder result = cs.convert(converterName, pagesBH, null);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        File pdfFile = Framework.createTempFile("testingPDFConverter", ".pdf");
        try {
            result.getBlob().transferTo(pdfFile);
            String text = DocumentUTUtils.readPdfText(pdfFile).toLowerCase();
            assertTrue(text.contains("hello"));
        } finally {
            pdfFile.delete();
        }
    }

    @Test
    public void testHTMLConverter() throws Exception {
        String converterName = cs.getConverterName("application/vnd.apple.pages", "text/html");
        assertEquals("iwork2html", converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("pdftohtml");

        BlobHolder pagesBH = getBlobFromPath("test-docs/hello.pages");
        pagesBH.getBlob().setMimeType("application/vnd.apple.pages");
        BlobHolder result = cs.convert(converterName, pagesBH, null);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(2, blobs.size());

        Blob mainBlob = result.getBlob();
        assertEquals("index.html", mainBlob.getFilename());

        Blob subBlob = blobs.get(1);
        assertTrue(subBlob.getFilename().startsWith("index001"));

        String htmlContent = mainBlob.getString();
        assertTrue(htmlContent.contains("hello"));
    }

    @Test(expected = ConversionException.class)
    public void testPagesWithoutPreviewConverter() throws Exception {
        String converterName = cs.getConverterName("application/vnd.apple.pages", "application/pdf");
        assertEquals("iwork2pdf", converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("pdftohtml");

        BlobHolder pagesBH = getBlobFromPath("test-docs/hello-without-preview.pages");
        pagesBH.getBlob().setMimeType("application/vnd.apple.pages");
        cs.convert(converterName, pagesBH, null);
        fail("pdf preview isn't available");
    }
}
