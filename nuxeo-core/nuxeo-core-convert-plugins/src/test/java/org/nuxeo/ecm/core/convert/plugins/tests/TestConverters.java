/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.convert.plugins.tests;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

public class TestConverters extends BaseConverterTest {

    public void testHTMLConverter() throws Exception {
        doTestTextConverter("text/html", "html2text", "hello.html");
    }

    public void testHTMLConverterWithEncoding() throws Exception {

        String srcMimeType = "text/html";
        String expectedconverterName = "html2text";
        String fileName = "strange.html";

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(srcMimeType, "text/plain");
        assertEquals(expectedconverterName, converterName);

        BlobHolder holder = getBlobFromPath("test-docs/" + fileName);
        BlobHolder result = cs.convert(converterName, holder, null);
        assertNotNull(result);

        String textResult = result.getBlob().getString().trim();
        //System.out.print(textResult);
        assertTrue(textResult.contains("Nuxeo propose aux organisations"));
        assertTrue(textResult.contains("\u00c9v\u00e9nements \u00e0 venir"));

        // System.out.println(result.getBlob().getString());
    }

    public void testXMLConverter() throws Exception {
        doTestTextConverter("text/xml", "xml2text", "hello.xml");
    }

    public void testXlConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-excel", "xl2text", "hello.xls");
    }

    public void testOOWriterConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.writer", "oo2text", "hello.sxw");
        String textContent = doTestTextConverter("application/vnd.oasis.opendocument.text", "oo2text", "hello.odt");
        assertTrue(textContent.contains(" first "));
        assertTrue(textContent.contains(" second "));
        assertTrue(textContent.contains(" third "));
        assertTrue(textContent.contains("d\u00e9j\u00e0"));
    }

    public void testOOWriterArabicConverter() throws Exception {
        doTestArabicTextConverter("application/vnd.oasis.opendocument.text", "oo2text", "wikipedia-internet-ar.odt");
    }

    public void testHTMLArabicConverter() throws Exception {
        doTestArabicTextConverter("text/html", "html2text", "wikipedia-internet-ar.html");
    }

    public void testOOCalcConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.calc", "oo2text", "hello.sxc");
        doTestTextConverter("application/vnd.oasis.opendocument.spreadsheet", "oo2text", "hello.ods");
    }

    public void testOOPrezConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.impress", "oo2text", "hello.sxi");
        doTestTextConverter("application/vnd.oasis.opendocument.presentation", "oo2text", "hello.odp");
    }

    public void testPDFConverter() throws Exception {
        String textContent = doTestTextConverter("application/pdf", "pdf2text", "hello.pdf");
        assertTrue(textContent.contains(" first "));
        assertTrue(textContent.contains(" second "));
        assertTrue(textContent.contains(" third "));
        assertTrue(textContent.contains("d\u00e9j\u00e0"));
    }

    public void testPDFArabicConverter() throws Exception {
        doTestArabicTextConverter("application/pdf", "pdf2text",
                "wikipedia-internet-ar.pdf");
    }

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
    }

}
