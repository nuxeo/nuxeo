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
 * $Id: TestJOOoConverterTranformer2pdf.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.jooconverter;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

/**
 *
 * Testing the joooconverter.
 *
 * This tests are conveting basic documents to pdf
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestJOOoConverterTranformer2pdf extends AbstractPluginTestCase {

    private Transformer transformer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        transformer = service.getTransformerByName("any2pdf");
        assertNotNull(transformer);
    }

    @Override
    public void tearDown() throws Exception {
        transformer = null;
        super.tearDown();
    }

    public void testText2pdfConversion() throws Exception {
        String path = "test-data/hello.txt";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path), "text/plain"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content", "Hello from a text document!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    /*
     * Microsoft office documents.
     *
     */
    public void testDoc2pdfConversion() throws Exception {
        String path = "test-data/hello.doc";

        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content", "Hello from a Microsoft Word Document!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    public void testXls2pdfConversion() throws Exception {
        String path = "test-data/hello.xls";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.ms-excel"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content",
                "Sheet1\nPage 1\nHello from a Microsoft Excel Spreadsheet!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    public void testPpt2pdfConversion() throws Exception {

        String path = "test-data/hello.ppt";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.ms-powerpoint"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content",
                "Hello from a Microsoft PowerPoint Presentation!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    /*
     * OpenOffice.org 2.x documents.
     *
     */

    public void testOdt2pdfConversion() throws Exception {
        String path = "test-data/hello.odt";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.oasis.opendocument.text"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content", "Hello from an OpenDocument Text!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    public void testOdp2pdfConversion() throws Exception {
        String path = "test-data/hello.odp";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.oasis.opendocument.presentation"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content", "Hello from an OpenDocument Presentation!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    public void testOds2pdfConversion() throws Exception {
        String path = "test-data/hello.ods";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.oasis.opendocument.spreadsheet"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content",
                "Sheet1\nPage 1\nHello from an OpenDocument Spreadsheet!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    /*
     * OpenOffice.org 1.x documents
     */

    public void testSxw2pdfConversion() throws Exception {
        String path = "test-data/hello.sxw";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.sun.xml.writer"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content",
                "Hello from an OpenOffice.org 1.0 Text Document!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    public void testSxc2pdfConversion() throws Exception {
        String path = "test-data/hello.sxc";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.sun.xml.calc"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals(
                "pdf content",
                "Sheet1\nPage 1\nHello from an OpenOffice.org 1.0 Spreadsheet!",
                DocumentTestUtils.readPdfText(pdfFile));
    }

    public void testSxi2pdfConversion() throws Exception {
        String path = "test-data/hello.sxi";
        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/vnd.sun.xml.impress"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertEquals("pdf content",
                "Hello from an OpenOffice.org 1.0 Presentation!",
                DocumentTestUtils.readPdfText(pdfFile));
    }
    public void testHtml2pdfConversion() throws Exception {
        String path = "test-data/hello.html";
        transformer = service.getTransformerByName("any2pdf");

        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),"text/html"));

        File pdfFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "pdf");
        assertTrue("pdf content ",
                DocumentTestUtils.readPdfText(pdfFile).length() > 0);
    }
}
