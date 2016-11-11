/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
public class TestConverters extends SimpleConverterTest {

    @Inject
    protected ConversionService cs;

    @Inject
    protected CommandLineExecutorService cles;

    @Test
    public void testHTMLConverter() throws Exception {
        doTestTextConverter("text/html", "html2text", "hello.html");
    }

    @Test
    public void testMDConverter() throws Exception {
        String textContent = doTestTextConverter("text/x-web-markdown", "md2text", "hello.md");
        assertTrue(textContent.contains("Hello from a markdown file"));
        assertTrue(textContent.contains("# This is the title"));
        assertTrue(textContent.contains("This is markdown content..."));
    }

    @Test
    public void testHTMLConverterWithEncoding() throws Exception {

        String srcMimeType = "text/html";
        String expectedconverterName = "html2text";
        String fileName = "strange.html";

        String converterName = cs.getConverterName(srcMimeType, "text/plain");
        assertEquals(expectedconverterName, converterName);

        BlobHolder holder = getBlobFromPath("test-docs/" + fileName);
        BlobHolder result = cs.convert(converterName, holder, null);
        assertNotNull(result);

        String textResult = result.getBlob().getString().trim();
        // System.out.print(textResult);
        assertTrue(textResult.contains("Nuxeo propose aux organisations"));
        assertTrue(textResult.contains("\u00c9v\u00e9nements \u00e0 venir"));

        // System.out.println(result.getBlob().getString());
    }

    @Test
    public void testXMLConverter() throws Exception {
        doTestTextConverter("text/xml", "xml2text", "hello.xml");
    }

    @Test
    public void testOOWriterConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.writer", "oo2text", "hello.sxw");
        String textContent = doTestTextConverter("application/vnd.oasis.opendocument.text", "oo2text", "hello.odt");
        assertTrue(textContent.contains(" first "));
        assertTrue(textContent.contains(" second "));
        assertTrue(textContent.contains(" third "));
        assertTrue(textContent.contains("d\u00e9j\u00e0"));
    }

    @Test
    public void testOOWriterArabicConverter() throws Exception {
        doTestArabicTextConverter("application/vnd.oasis.opendocument.text", "oo2text", "wikipedia-internet-ar.odt");
    }

    @Test
    public void testHTMLArabicConverter() throws Exception {
        doTestArabicTextConverter("text/html", "html2text", "wikipedia-internet-ar.html");
    }

    @Test
    public void testOOCalcConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.calc", "oo2text", "hello.sxc");
        doTestTextConverter("application/vnd.oasis.opendocument.spreadsheet", "oo2text", "hello.ods");
    }

    @Test
    public void testOOPrezConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.impress", "oo2text", "hello.sxi");
        doTestTextConverter("application/vnd.oasis.opendocument.presentation", "oo2text", "hello.odp");
    }

    @Test
    public void testPDFConverter() throws Exception {
        String textContent = doTestTextConverter("application/pdf", "pdf2text", "hello.pdf");
        assertTrue(textContent.contains(" first "));
        assertTrue(textContent.contains(" second "));
        assertTrue(textContent.contains(" third "));
        assertTrue(textContent.contains("d\u00e9j\u00e0"));
    }

    @Test
    public void testAiConverter() throws Exception {
        String textContent = doTestTextConverter("application/illustrator", "ps2pdf2text", "hello.ai");
        assertTrue(textContent.contains("Hello from a .ai document!"));
    }

    @Test
    public void testEpsConverter() throws Exception {
        String textContent = doTestTextConverter("application/eps", "ps2pdf2text", "hello.eps");
        assertTrue(textContent.contains("Hello from a .eps document!"));
    }

    // fails since upgrade from pdfbox 1.6.0 to 1.8.5
    // but deemed lower priority than NXP-11676
    @Ignore
    @Test
    public void testPDFArabicConverter() throws Exception {
        doTestArabicTextConverter("application/pdf", "pdf2text", "wikipedia-internet-ar.pdf");
    }

    @Test
    public void testAnyToTextConverter() throws Exception {
        doTestAny2TextConverter("text/html", "any2text", "hello.html");
        doTestAny2TextConverter("text/xml", "any2text", "hello.xml");
        doTestAny2TextConverter("application/vnd.ms-excel", "any2text", "hello.xls");
        doTestAny2TextConverter("application/vnd.sun.xml.writer", "any2text", "hello.sxw");
        doTestAny2TextConverter("application/vnd.oasis.opendocument.text", "any2text", "hello.odt");
        doTestAny2TextConverter("application/vnd.sun.xml.calc", "any2text", "hello.sxc");
        doTestAny2TextConverter("application/vnd.oasis.opendocument.spreadsheet", "any2text", "hello.ods");
        doTestAny2TextConverter("application/vnd.sun.xml.impress", "any2text", "hello.sxi");
        doTestAny2TextConverter("application/vnd.oasis.opendocument.presentation", "any2text", "hello.odp");
        doTestAny2TextConverter("application/pdf", "any2text", "hello.pdf");
        doTestAny2TextConverter("application/rtf", "any2text", "hello.rtf");
        doTestAny2TextConverter("text/rtf", "any2text", "hello.rtf");

    }

}
