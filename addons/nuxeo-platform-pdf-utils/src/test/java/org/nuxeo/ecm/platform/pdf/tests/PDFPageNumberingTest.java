/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf.tests;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFPageNumbering.PAGE_NUMBER_POSITION;
import org.nuxeo.ecm.platform.pdf.PDFPageNumbering;
import org.nuxeo.ecm.platform.pdf.operations.PDFAddPageNumbersOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFPageNumberingTest {

    private FileBlob pdfFileBlob;

    private String pdfMD5;

    private DocumentModel testDocsFolder;

    private static final int[] indexOfNumberingByPage = new int[] {
        1665, 1387, 1515, 1592, 1397, 1592, 1387, 1729, 1589, 1444, 1459, 1667, 849 };

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setUp() throws Exception {
        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        pdfFileBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_PATH));
        pdfMD5 = TestUtils.calculateMd5(pdfFileBlob.getFile());
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    private void checkPageNumbering(Blob pdfBlob, int firstPage, int firstNumber) throws Exception {
        assertNotNull(pdfBlob);
        assertNotSame(pdfMD5, TestUtils.calculateMd5(pdfBlob.getFile()));
        PDDocument resultPDF = PDDocument.load(pdfBlob.getFile());
        assertNotNull(resultPDF);
        assertEquals(13, resultPDF.getNumberOfPages());
        if (firstPage > 1) {
            // there should not be any numbers in the pages before the ones that were numbered
            assertFalse(TestUtils.extractText(resultPDF, 1, firstPage - 1).replace("\n", "").matches(".*\\d+.*"));
        }
        for (int page = firstPage; page <= 13; page++) {
            // every numbered page should have the right number
            int currentPageNumber = firstNumber + (page - firstPage);
            assertEquals(indexOfNumberingByPage[page - 1],
                TestUtils.extractText(resultPDF, page, page).indexOf(Integer.toString(currentPageNumber)));
        }
        resultPDF.close();
    }

    @Test
    public void testPageNumberingBottomLeft() throws Exception {
        PDFPageNumbering pdfpn = new PDFPageNumbering(pdfFileBlob);
        Blob result = pdfpn.addPageNumbers(1, 1, null, 0, "ff0000", PAGE_NUMBER_POSITION.BOTTOM_LEFT);
        checkPageNumbering(result, 1, 1);
    }

    @Test
    public void testPageNumberingBottomCenter() throws Exception {
        PDFPageNumbering pdfpn = new PDFPageNumbering(pdfFileBlob);
        Blob result = pdfpn.addPageNumbers(5, 3, null, 0, "00ff00", PAGE_NUMBER_POSITION.BOTTOM_CENTER);
        checkPageNumbering(result, 5, 3);
    }

    @Test
    public void testPageNumberingBottomRight() throws Exception {
        PDFPageNumbering pdfpn = new PDFPageNumbering(pdfFileBlob);
        Blob result = pdfpn.addPageNumbers(10, 10, null, 0, "0000ff", PAGE_NUMBER_POSITION.BOTTOM_RIGHT);
        checkPageNumbering(result, 10, 10);
    }

    @Test
    public void testPageNumberingTopLeft() throws Exception {
        PDFPageNumbering pdfpn = new PDFPageNumbering(pdfFileBlob);
        Blob result = pdfpn.addPageNumbers(1, 150, null, 0, "FF0000", PAGE_NUMBER_POSITION.TOP_LEFT);
        checkPageNumbering(result, 1, 150);
    }

    @Test
    public void testPageNumberingTopCenter() throws Exception {
        PDFPageNumbering pdfpn = new PDFPageNumbering(pdfFileBlob);
        Blob result = pdfpn.addPageNumbers(1, 1, null, 0, "0x0000ff", PAGE_NUMBER_POSITION.TOP_CENTER);
        checkPageNumbering(result, 1, 1);
    }

    @Test
    public void testPageNumberingTopRight() throws Exception {
        PDFPageNumbering pdfpn = new PDFPageNumbering(pdfFileBlob);
        Blob result = pdfpn.addPageNumbers(1, 1, null, 0, "", PAGE_NUMBER_POSITION.TOP_RIGHT);
        checkPageNumbering(result, 1, 1);
    }

    @Test
    public void testAddPageNumbersOperationSimple() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileBlob);
        chain.add(PDFAddPageNumbersOperation.ID);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        checkPageNumbering(result, 1, 1);
    }

    @Test
    public void testAddPageNumbersOperationComplex() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileBlob);
        chain.add(PDFAddPageNumbersOperation.ID)
            .set("startAtPage", 2)
            .set("startAtNumber", 10)
            .set("position", "Top left")
            .set("fontName", PDType1Font.COURIER.getBaseFont())
            .set("fontSize", 32)
            .set("hex255Color", "ff00ff");
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        checkPageNumbering(result, 2, 10);
    }

}
