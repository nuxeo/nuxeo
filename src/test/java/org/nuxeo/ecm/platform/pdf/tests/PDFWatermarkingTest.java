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

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFWatermarking;
import org.nuxeo.ecm.platform.pdf.operations.PDFWatermarkImageOperation;
import org.nuxeo.ecm.platform.pdf.operations.PDFWatermarkPDFOperation;
import org.nuxeo.ecm.platform.pdf.operations.PDFWatermarkTextOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.pdf" })
public class PDFWatermarkingTest {

    private static final String TEXT_WATERMARK = "(c) Test Text Watermark";

    private FileBlob pdfFileBlob;

    private FileBlob pdfFileWithImagesBlob;

    private DocumentModel testDocsFolder, pngForWatermarkDoc;

    private boolean imageIOGeIoUseCache;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setUp() {
        imageIOGeIoUseCache = ImageIO.getUseCache();
        ImageIO.setUseCache(false);
        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        pdfFileBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_PATH));
        pdfFileWithImagesBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_IMAGES_PATH));
        File f = FileUtils.getResourceFileFromContext(TestUtils.PNG_WATERMARK_PATH);
        DocumentModel doc = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), f.getName(), "File");
        doc.setPropertyValue("dc:title", f.getName());
        doc.setPropertyValue("file:content", new FileBlob(f));
        pngForWatermarkDoc = coreSession.createDocument(doc);
    }

    @After
    public void tearDown() {
        ImageIO.setUseCache(imageIOGeIoUseCache);
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    private boolean hasTextOnAllPages(Blob blob, String watermark) throws Exception {
        PDDocument doc = PDDocument.load(blob.getStream());
        for (int i = 1; i <= doc.getNumberOfPages(); i++) {
            if(!TestUtils.extractText(doc, i, i).replace("\n", "").contains(watermark)) {
                doc.close();
                return false;
            }
        }
        doc.close();
        return true;
    }

    private boolean hasImageWithDimensions(PDPage page, int width, int height) {
        Map<String, PDXObject> pageObjects = page.getResources().getXObjects();
        for (Map.Entry<String, PDXObject> pageObjectsEntry : pageObjects.entrySet()) {
            if (pageObjectsEntry.getValue() instanceof PDXObjectImage) {
                PDXObjectImage pageImage = (PDXObjectImage) pageObjectsEntry.getValue();
                if (pageImage.getWidth() == width && pageImage.getHeight() == height) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasImageWithDimensionsInAllPages(Blob blob, int width, int height) throws Exception {
        PDDocument doc = PDDocument.load(blob.getFile());
        List docPages = doc.getDocumentCatalog().getAllPages();
        for (Object docObject : docPages) {
            PDPage docPage = (PDPage) docObject;
            if (!hasImageWithDimensions(docPage, width, height)) {
                doc.close();
                return false;
            }
        }
        doc.close();
        return true;
    }

    @Test
    public void testWatermarkTextPDF() throws Exception {
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        pdfw.setText(TEXT_WATERMARK);
        assertFalse(hasTextOnAllPages(pdfFileBlob, TEXT_WATERMARK));
        assertTrue(hasTextOnAllPages(pdfw.watermark(), TEXT_WATERMARK));
    }

    @Test
    public void testWatermarkTextPDFWithImages() throws Exception {
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);
        pdfw.setText(TEXT_WATERMARK);
        assertFalse(hasTextOnAllPages(pdfFileWithImagesBlob, TEXT_WATERMARK));
        assertTrue(hasTextOnAllPages(pdfw.watermark(), TEXT_WATERMARK));
    }

    @Test
    public void testWatermarkTextStylizedPDFWithImages() throws Exception {
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);
        pdfw.setText(TEXT_WATERMARK).setXPosition(100).setYPosition(100)
            .setAlphaColor(0.3f).setFontSize(12f).setTextRotation(45);
        assertFalse(hasTextOnAllPages(pdfFileWithImagesBlob, TEXT_WATERMARK));
        assertTrue(hasTextOnAllPages(pdfw.watermark(), TEXT_WATERMARK));
    }

    @Test
    public void testWatermarkNoText() throws Exception {
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        pdfw.setText("");
        assertEquals(TestUtils.calculateMd5(pdfFileBlob.getFile()), TestUtils.calculateMd5(pdfw.watermark().getFile()));
    }

    @Test
    public void testWatermarkPDF() throws Exception {
        FileBlob overlayBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_WATERMARK_PATH));
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        Blob result = pdfw.watermarkWithPdf(overlayBlob);
        PDDocument originalPDF = PDDocument.load(pdfFileBlob.getFile());
        PDDocument watermarkedPDF = PDDocument.load(result.getFile());
        assertEquals(originalPDF.getNumberOfPages(), watermarkedPDF.getNumberOfPages());
        assertEquals(TestUtils.extractText(originalPDF, 1, originalPDF.getNumberOfPages()),
            TestUtils.extractText(watermarkedPDF, 1 ,watermarkedPDF.getNumberOfPages()));
        assertNotEquals(TestUtils.calculateMd5(pdfFileBlob.getFile()), TestUtils.calculateMd5(result.getFile()));
        originalPDF.close();
        watermarkedPDF.close();
    }

    @Test
    public void testWatermarkImagePDF() throws Exception {
        FileBlob overlayPictureBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PNG_WATERMARK_PATH));
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        Blob result = pdfw.watermarkWithImage(overlayPictureBlob, 100, 100, 0.8f);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

    @Test
    public void testWatermarkPNGImagePDFWithImages() throws Exception {
        FileBlob overlayPictureBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PNG_WATERMARK_PATH));
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileWithImagesBlob);
        Blob result = pdfw.watermarkWithImage(overlayPictureBlob, 200, 200, 0.5f);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileWithImagesBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

    @Test
    public void testWatermarkJPGImagePDFWithImages() throws Exception {
        FileBlob overlayPictureBlob = new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.JPG_WATERMARK_PATH));
        PDFWatermarking pdfw = new PDFWatermarking(pdfFileBlob);
        Blob result = pdfw.watermarkWithImage(overlayPictureBlob, 300, 300, 0.2f);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

    @Test
    public void testWatermarkTextOperation() throws Exception {
        Properties properties = new Properties();
        properties.put("xPosition", "200");
        properties.put("yPosition", "300");
        properties.put("alphaColor", "0.9");
        properties.put("invertY", "true");
        properties.put("textRotation", "45");
        String watermark = "© ACME - Watermark";
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileWithImagesBlob);
        chain.add(PDFWatermarkTextOperation.ID)
            .set("watermark", watermark)
            .set("properties", properties);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertFalse(hasTextOnAllPages(pdfFileWithImagesBlob, watermark));
        assertTrue(hasTextOnAllPages(result, watermark));
    }

    @Test
    public void testWatermarkImageOperation() throws Exception {
        File overlayPictureFile = FileUtils.getResourceFileFromContext(TestUtils.PNG_WATERMARK_PATH);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileWithImagesBlob);
        ctx.put("overlay", overlayPictureBlob);
        chain.add(PDFWatermarkImageOperation.ID)
            .set("imageContextVarName", "overlay");
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileWithImagesBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

    @Test
    public void testWatermarkImageOperationStylized() throws Exception {
        File overlayPictureFile = FileUtils.getResourceFileFromContext(TestUtils.PNG_WATERMARK_PATH);
        FileBlob overlayPictureBlob = new FileBlob(overlayPictureFile);
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileWithImagesBlob);
        ctx.put("overlay", overlayPictureBlob);
        chain.add(PDFWatermarkImageOperation.ID)
            .set("imageContextVarName", "overlay")
            .set("x", 200)
            .set("y", "400")
            .set("scale", "2.0");
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileWithImagesBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

    @Test
    public void testWatermarkImageOperationDocumentId() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileWithImagesBlob);
        chain.add(PDFWatermarkImageOperation.ID)
            .set("imageDocRef", pngForWatermarkDoc.getId());
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileWithImagesBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

    @Test
    public void testWatermarkPDFOperation() throws Exception {
        File overlayPdfFile = FileUtils.getResourceFileFromContext(TestUtils.PDF_WATERMARK_PATH);
        FileBlob overlayPdfBlob = new FileBlob(overlayPdfFile);
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileWithImagesBlob);
        ctx.put("overlay", overlayPdfBlob);
        chain.add(PDFWatermarkPDFOperation.ID)
            .set("pdfContextVarName", "overlay");
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertFalse(hasImageWithDimensionsInAllPages(pdfFileWithImagesBlob,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
        assertTrue(hasImageWithDimensionsInAllPages(result,
            TestUtils.PNG_WATERMARK_WIDTH, TestUtils.PNG_WATERMARK_HEIGHT));
    }

}
