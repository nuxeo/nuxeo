/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

public class TestOOoConvert extends BaseConverterTest {

    protected static final String ODT_MT = "application/vnd.oasis.opendocument.text";

    @Test
    public void testOfficeConverter() throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);

        BlobHolder bh = getBlobFromPath("data/testMe.html", "text/html");
        String converterName = cs.getConverterName(bh.getBlob().getMimeType(), ODT_MT);
        assertEquals("any2odt", converterName);

        if (cs.isConverterAvailable(converterName).isAvailable()) {
            BlobHolder result = cs.convert(converterName, bh, null);
            File odtFile = Framework.createTempFile("htmlfile", "odt");
            result.getBlob().transferTo(odtFile);
            odtFile.delete();
        }
    }

    @Test
    public void testOfficeConverter2() throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);

        BlobHolder bh = getBlobFromPath("data/testMe.md", "text/x-web-markdown");
        String converterName = cs.getConverterName(bh.getBlob().getMimeType(), ODT_MT);
        assertEquals("any2odt", converterName);

        if (cs.isConverterAvailable(converterName).isAvailable()) {
            BlobHolder result = cs.convert(converterName, bh, null);
            File odtFile = Framework.createTempFile("mdfile", "odt");
            result.getBlob().transferTo(odtFile);
            odtFile.delete();
        }
    }

    @Test
    public void testOfficeConverter3() throws Exception {

        BlobHolder bh = getBlobFromPath("data/Spec_ModelNux.odt", "application/vnd.oasis.opendocument.text");

        ConversionService cs = Framework.getService(ConversionService.class);

        String converterName = cs.getConverterName(bh.getBlob().getMimeType(), "application/pdf");
        assertEquals("any2pdf", converterName);

        if (cs.isConverterAvailable(converterName).isAvailable()) {

            BlobHolder result = cs.convert(converterName, bh, null);

            File pdfFile = Framework.createTempFile("testfile", "pdf");

            result.getBlob().transferTo(pdfFile);

            pdfFile.delete();

        }

    }

    @Test
    public void testOfficeConverter4() throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);

        BlobHolder bh = getBlobFromPath("data/testMe.html", "text/html");
        String converterName = cs.getConverterName(bh.getBlob().getMimeType(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertEquals("any2docx", converterName);

        boolean isAvailable = cs.isConverterAvailable(converterName).isAvailable();
        assumeTrue(isAvailable);

        BlobHolder result = cs.convert(converterName, bh, null);
        File docxFile = Framework.createTempFile("docxfile", "docx");
        result.getBlob().transferTo(docxFile);

        XWPFDocument doc = new XWPFDocument(new FileInputStream(docxFile));
        XWPFWordExtractor extractor = new XWPFWordExtractor(doc);

        String text = extractor.getText();
        assertTrue(text.length() > 0);
        assertTrue(text.contains("Titre 1"));

        docxFile.delete();
    }

    @Test
    public void testOfficeConverter5() throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);

        BlobHolder bh = getBlobFromPath("data/testMe.html", "text/html");
        String converterName = cs.getConverterName(bh.getBlob().getMimeType(),
                "application/msword");
        assertEquals("any2doc", converterName);

        boolean isAvailable = cs.isConverterAvailable(converterName).isAvailable();
        assumeTrue(isAvailable);

        BlobHolder result = cs.convert(converterName, bh, null);
        File docFile = Framework.createTempFile("docfile", "doc");
        result.getBlob().transferTo(docFile);

        HWPFDocument doc = new HWPFDocument(new FileInputStream(docFile));
        WordExtractor extractor = new WordExtractor(doc);

        String text = extractor.getText();
        assertTrue(text.length() > 0);
        assertTrue(text.contains("Titre 1"));

        docFile.delete();
    }

}
