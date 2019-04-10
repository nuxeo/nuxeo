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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.pdf.PDFPageExtractor;
import org.nuxeo.ecm.platform.pdf.operations.PDFConvertToPicturesOperation;
import org.nuxeo.ecm.platform.pdf.operations.PDFExtractPagesOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import java.io.File;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFPageExtractorTest {

    private FileBlob pdfFileBlob, encryptedPdfFileBlob;

    private DocumentModel testDocsFolder, pdfDocModel;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setUp() {
        testDocsFolder = coreSession.createDocumentModel("/", "test-pdf", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        assertNotNull(testDocsFolder);
        pdfFileBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_PATH));
        pdfFileBlob.setMimeType("application/pdf");
        pdfFileBlob.setFilename(pdfFileBlob.getFile().getName());
        pdfDocModel = coreSession.createDocumentModel(testDocsFolder.getPathAsString(),
            pdfFileBlob.getFile().getName(), "File");
        pdfDocModel.setPropertyValue("dc:title", pdfFileBlob.getFile().getName());
        pdfDocModel.setPropertyValue("file:content", pdfFileBlob);
        pdfDocModel = coreSession.createDocument(pdfDocModel);
        pdfDocModel = coreSession.saveDocument(pdfDocModel);
        assertNotNull(pdfDocModel);
        encryptedPdfFileBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_ENCRYPTED_PATH));
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    private void checkExtractedPDFPages1To3(File extractedPDFPages) throws Exception {
        PDDocument resultPDF = PDDocument.load(extractedPDFPages);
        assertEquals(3, resultPDF.getNumberOfPages());
        String text = TestUtils.extractText(resultPDF, 1, 3);
        assertEquals(4567, text.length());
        assertTrue(text.startsWith("Creative Brief\nDo this\nLorem ipsum dolor sit amet, consectetur adipisicing"));
        assertTrue(text.endsWith("blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et \n"));
        resultPDF.close();
    }

    @Test
    public void testExtractPDFPages() throws Exception {
        PDFPageExtractor pdfpe = new PDFPageExtractor(pdfFileBlob);
        Blob result = pdfpe.extract(1, 3);
        assertEquals(pdfFileBlob.getFilename().replace(".pdf", "-1-3.pdf"), result.getFilename());
        assertEquals("application/pdf", result.getMimeType());
        checkExtractedPDFPages1To3(result.getFile());
    }

    @Test
    public void testExtractPDFPagesEncrypted() throws Exception {
        PDFPageExtractor pdfpe = new PDFPageExtractor(encryptedPdfFileBlob);
        pdfpe.setPassword(TestUtils.PDF_ENCRYPTED_PASSWORD);
        Blob result = pdfpe.extract(1, 3);
        assertEquals(encryptedPdfFileBlob.getFilename().replace(".pdf", "-1-3.pdf"), result.getFilename());
        assertEquals("application/pdf", result.getMimeType());
        checkExtractedPDFPages1To3(result.getFile());
    }

    @Test
    public void testExtractPDFPagesWithFileName() throws Exception {
        PDFPageExtractor pdfpe = new PDFPageExtractor(pdfFileBlob);
        Blob result = pdfpe.extract(1, 3, "newpdf.pdf", null, null, null);
        assertEquals("newpdf.pdf", result.getFilename());
        assertEquals("application/pdf", result.getMimeType());
        checkExtractedPDFPages1To3(result.getFile());
    }

    @Test
    public void testExtractPDFPagesWithInfo() throws Exception {
        PDFPageExtractor pe = new PDFPageExtractor(pdfFileBlob);
        Blob result = pe.extract(1, 3, null, "Once Upon a Time", "Fairyland", "Cool Author");
        assertEquals(pdfFileBlob.getFilename().replace(".pdf", "-1-3.pdf"), result.getFilename());
        assertEquals("application/pdf", result.getMimeType());
        checkExtractedPDFPages1To3(result.getFile());
        PDDocument resultPDF = PDDocument.load(result.getFile());
        assertEquals("Once Upon a Time", resultPDF.getDocumentInformation().getTitle());
        assertEquals("Fairyland", resultPDF.getDocumentInformation().getSubject());
        assertEquals("Cool Author", resultPDF.getDocumentInformation().getAuthor());
        resultPDF.close();
    }

    @Test
    public void testExtractPDFPagesToPictures() throws Exception {
        FileBlob fb = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_TRANSCRIPT_PATH));
        PDFPageExtractor pdfpe = new PDFPageExtractor(fb);
        BlobList results = pdfpe.getPagesAsImages(null);
        assertEquals(results.size(), 2);
        assertEquals("picture/png", results.get(0).getMimeType());
        assertEquals(TestUtils.PDF_TRANSCRIPT_PATH.replace("files/", "").replace(".pdf", ".pdf-1.png"),
            results.get(0).getFilename());
        assertEquals("picture/png", results.get(1).getMimeType());
        assertEquals(TestUtils.PDF_TRANSCRIPT_PATH.replace("files/", "").replace(".pdf", ".pdf-2.png"),
            results.get(1).getFilename());
    }

    @Test
    public void testExtractPDFPagesOperation() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileBlob);
        chain.add(PDFExtractPagesOperation.ID)
            .set("startPage", 1)
            .set("endPage", 3);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(pdfFileBlob.getFilename().replace(".pdf", "-1-3.pdf"), result.getFilename());
        assertEquals("application/pdf", result.getMimeType());
        checkExtractedPDFPages1To3(result.getFile());
    }

    @Test
    public void testExtractPDFPagesOperationEncrypted() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(encryptedPdfFileBlob);
        chain.add(PDFExtractPagesOperation.ID)
            .set("startPage", 1)
            .set("endPage", 3)
            .set("password", TestUtils.PDF_ENCRYPTED_PASSWORD);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(encryptedPdfFileBlob.getFilename().replace(".pdf", "-1-3.pdf"), result.getFilename());
        assertEquals("application/pdf", result.getMimeType());
        checkExtractedPDFPages1To3(result.getFile());
    }

    @Test
    public void testConvertPDFToPicturesOperation() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfDocModel);
        chain.add(PDFConvertToPicturesOperation.ID);
        BlobList result = (BlobList) automationService.run(ctx, chain);
        assertNotNull(result);
        for (Blob pictureBlob : result) {
            assertEquals("picture/png", pictureBlob.getMimeType());
            assertEquals(pdfDocModel.getPropertyValue("dc:title") + "-" + (result.indexOf(pictureBlob) + 1) + ".png",
                pictureBlob.getFilename());
        }
    }

    @Test(expected = NuxeoException.class)
    public void testExtractPDFPagesShouldFail() throws Exception {
        PDFPageExtractor pdfpe = new PDFPageExtractor(
            new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.JPG_PATH)));
        pdfpe.extract(1, 3); // IOException: Failed to load the PDF
    }

}
