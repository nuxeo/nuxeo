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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFMerge;
import org.nuxeo.ecm.platform.pdf.operations.PDFMergeBlobsOperation;
import org.nuxeo.ecm.platform.pdf.operations.PDFMergeDocumentsOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFMergeTests {

    private DocumentModel testDocsFolder, docMergePDF1, docMergePDF2, docMergePDF3;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setUp() {
        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_1);
        DocumentModel d = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), f.getName(), "File");
        d.setPropertyValue("dc:title", f.getName());
        d.setPropertyValue("file:content", new FileBlob(f));
        docMergePDF1 = coreSession.createDocument(d);
        f = FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2);
        d = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), f.getName(), "File");
        d.setPropertyValue("dc:title", f.getName());
        d.setPropertyValue("file:content", new FileBlob(f));
        docMergePDF2 = coreSession.createDocument(d);
        f = FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_3);
        d = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), f.getName(), "File");
        d.setPropertyValue("dc:title", f.getName());
        d.setPropertyValue("file:content", new FileBlob(f));
        docMergePDF3 = coreSession.createDocument(d);
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    private void checkMergedTwoPDFs(File mergedPDFFile) throws Exception {
        PDDocument mergedPDF = PDDocument.load(mergedPDFFile);
        assertEquals(5, mergedPDF.getNumberOfPages());
        assertTrue(TestUtils.extractText(mergedPDF, 1, 1).contains("This is pdf 1. It has 2 pages"));
        assertTrue(TestUtils.extractText(mergedPDF, 2, 2).contains("End of pdf1"));
        assertTrue(TestUtils.extractText(mergedPDF, 3, 3).contains("This is pdf 2. It has 3 pages"));
        assertTrue(TestUtils.extractText(mergedPDF, 5, 5).contains("End of pdf2 - it has 3 pages"));
        mergedPDF.close();
    }

    private void checkMergedThreePDFs(File mergedPDFFile) throws Exception {
        PDDocument mergedPDF = PDDocument.load(mergedPDFFile);
        assertEquals(6, mergedPDF.getNumberOfPages());
        assertTrue(TestUtils.extractText(mergedPDF, 1, 1).contains("This is pdf 1. It has 2 pages"));
        assertTrue(TestUtils.extractText(mergedPDF, 2, 2).contains("End of pdf1"));
        assertTrue(TestUtils.extractText(mergedPDF, 3, 3).contains("This is pdf 2. It has 3 pages"));
        assertTrue(TestUtils.extractText(mergedPDF, 5, 5).contains("End of pdf2 - it has 3 pages"));
        assertTrue(TestUtils.extractText(mergedPDF, 6, 6).contains("This is pdf 3. It has 1 page"));
        mergedPDF.close();
    }

    @Test
    public void testMergePDFBlobs() throws Exception {
        PDFMerge pdfm = new PDFMerge();
        pdfm.addBlob(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_1)));
        pdfm.addBlob(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2)));
        pdfm.addBlob(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_3)));
        Blob result = pdfm.merge("merged.pdf");
        assertNotNull(result);
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFBlobList() throws Exception {
        BlobList bl = new BlobList();
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_1)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_3)));
        PDFMerge pdfm = new PDFMerge(bl);
        Blob result = pdfm.merge("merged.pdf");
        assertNotNull(result);
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFDocuments() throws Exception {
        PDFMerge pdfm = new PDFMerge();
        pdfm.addBlob(docMergePDF1, null);
        pdfm.addBlob(docMergePDF2, null);
        pdfm.addBlob(docMergePDF3, null);
        Blob result = pdfm.merge("merged.pdf");
        assertNotNull(result);
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFDocumentList() throws Exception {
        DocumentModelList docList = new DocumentModelListImpl();
        docList.add(docMergePDF1);
        docList.add(docMergePDF2);
        docList.add(docMergePDF3);
        PDFMerge pdfm = new PDFMerge(docList, null);
        Blob result = pdfm.merge("merged.pdf");
        assertNotNull(result);
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFDocumentIDs() throws Exception {
        String[] docIDs = new String[3];
        docIDs[0] = docMergePDF1.getId();
        docIDs[1] = docMergePDF2.getId();
        docIDs[2] = docMergePDF3.getId();
        PDFMerge pdfm = new PDFMerge(docIDs, null, coreSession);
        Blob result = pdfm.merge("merged.pdf");
        assertNotNull(result);
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFOperationBlobs() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_1)));
        ctx.put(TestUtils.PDF_MERGE_2, new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2)));
        String mergedPDFName = "merged.pdf";
        chain.add(PDFMergeBlobsOperation.ID)
            .set("toAppendVarName", TestUtils.PDF_MERGE_2)
            .set("fileName", mergedPDFName);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(mergedPDFName, result.getFilename());
        checkMergedTwoPDFs(result.getFile());
    }

    @Test
    public void testMergePDFOperationBlobList() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        BlobList bl = new BlobList();
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_1)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2)));
        ctx.setInput(bl);
        ctx.put(TestUtils.PDF_MERGE_3, new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_3)));
        String mergedPDFName = "merged.pdf";
        chain.add(PDFMergeBlobsOperation.ID)
            .set("toAppendVarName", TestUtils.PDF_MERGE_3)
            .set("fileName", mergedPDFName);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(mergedPDFName, result.getFilename());
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFOperationBlobListAndBlob() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_1)));
        BlobList bl = new BlobList();
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2)));
        bl.add(new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_3)));
        ctx.put("blobList", bl);
        String mergedPDFName = "merged.pdf";
        chain.add(PDFMergeBlobsOperation.ID)
            .set("toAppendListVarName", "blobList")
            .set("fileName", mergedPDFName);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(mergedPDFName, result.getFilename());
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFOperationDocuments() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(docMergePDF1);
        ctx.put(TestUtils.PDF_MERGE_2, new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_2)));
        String mergedPDFName = "merged.pdf";
        chain.add(PDFMergeDocumentsOperation.ID)
            .set("toAppendVarName", TestUtils.PDF_MERGE_2)
            .set("fileName", mergedPDFName);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(mergedPDFName, result.getFilename());
        checkMergedTwoPDFs(result.getFile());
    }

    @Test
    public void testMergePDFOperationDocumentList() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        DocumentModelList docList = new DocumentModelListImpl();
        docList.add(docMergePDF1);
        docList.add(docMergePDF2);
        ctx.setInput(docList);
        ctx.put(TestUtils.PDF_MERGE_3, new FileBlob(FileUtils.getResourceFileFromContext(TestUtils.PDF_MERGE_3)));
        String mergedPDFName = "merged.pdf";
        chain.add(PDFMergeDocumentsOperation.ID)
            .set("toAppendVarName", TestUtils.PDF_MERGE_3)
            .set("fileName", mergedPDFName);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(mergedPDFName, result.getFilename());
        checkMergedThreePDFs(result.getFile());
    }

    @Test
    public void testMergePDFOperationDocumentAndDocumentIDs() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(docMergePDF1);
        String[] docIDs = new String[2];
        docIDs[0] = docMergePDF2.getId();
        docIDs[1] = docMergePDF3.getId();
        ctx.put("ids", docIDs);
        String mergedPDFName = "merged.pdf";
        chain.add(PDFMergeDocumentsOperation.ID)
            .set("toAppendDocIDsVarName", "ids")
            .set("fileName", mergedPDFName);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        assertEquals(mergedPDFName, result.getFilename());
        checkMergedThreePDFs(result.getFile());
    }

}
