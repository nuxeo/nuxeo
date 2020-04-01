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
import java.util.HashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFEncryption;
import org.nuxeo.ecm.platform.pdf.operations.PDFEncryptOperation;
import org.nuxeo.ecm.platform.pdf.operations.PDFEncryptReadOnlyOperation;
import org.nuxeo.ecm.platform.pdf.operations.PDFRemoveEncryptionOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFEncryptionTest {

    private FileBlob pdfFileBlob, pdfEncryptedFileBlob;

    private DocumentModel testDocsFolder;

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
        File pdfFile = FileUtils.getResourceFileFromContext(TestUtils.PDF_PATH);
        assertNotNull(pdfFile);
        pdfFileBlob = new FileBlob(pdfFile);
        assertNotNull(pdfFileBlob);
        pdfFileBlob.setMimeType("application/pdf");
        pdfFileBlob.setFilename(pdfFile.getName());
        DocumentModel pdfDocModel = coreSession.createDocumentModel(testDocsFolder.getPathAsString(),
            pdfFile.getName(), "File");
        pdfDocModel.setPropertyValue("dc:title", pdfFile.getName());
        pdfDocModel.setPropertyValue("file:content", pdfFileBlob);
        pdfDocModel = coreSession.createDocument(pdfDocModel);
        pdfDocModel = coreSession.saveDocument(pdfDocModel);
        assertNotNull(pdfDocModel);
        File pdfEncryptedFile = FileUtils.getResourceFileFromContext(TestUtils.PDF_ENCRYPTED_PATH);
        assertNotNull(pdfEncryptedFile);
        pdfEncryptedFileBlob = new FileBlob(pdfEncryptedFile);
        assertNotNull(pdfEncryptedFileBlob);
        pdfEncryptedFileBlob.setMimeType("application/pdf");
        pdfEncryptedFileBlob.setFilename(pdfEncryptedFile.getName());
        DocumentModel pdfEncryptedDocModel = coreSession.createDocumentModel(testDocsFolder.getPathAsString(),
            pdfEncryptedFile.getName(), "File");
        pdfEncryptedDocModel.setPropertyValue("dc:title", pdfEncryptedFile.getName());
        pdfEncryptedDocModel.setPropertyValue("file:content", pdfEncryptedFileBlob);
        pdfEncryptedDocModel = coreSession.createDocument(pdfEncryptedDocModel);
        pdfEncryptedDocModel = coreSession.saveDocument(pdfEncryptedDocModel);
        assertNotNull(pdfEncryptedDocModel);
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    private void checkIsReadOnly(Blob inBlob, String ownerPwd, String userPwd) throws Exception {
        // load the blob as a PDDocument and check permissions as a user
        PDDocument pdfDoc = PDDocument.load(inBlob.getFile(), userPwd);
        assertTrue(pdfDoc.isEncrypted());
        AccessPermission pdfPermissions = pdfDoc.getCurrentAccessPermission();
        assertTrue(pdfPermissions.canPrint());
        assertTrue(pdfPermissions.canExtractContent());
        assertTrue(pdfPermissions.canExtractForAccessibility());
        assertTrue(pdfPermissions.canPrintDegraded());
        assertFalse(pdfPermissions.canModifyAnnotations());
        assertFalse(pdfPermissions.canModify());
        assertFalse(pdfPermissions.canFillInForm());
        assertFalse(pdfPermissions.canAssembleDocument());
        pdfDoc.close();
        // load the blob as a PDDocument and check permission as an owner
        pdfDoc = PDDocument.load(inBlob.getFile(), ownerPwd);
        assertTrue(pdfDoc.isEncrypted());
        pdfPermissions = pdfDoc.getCurrentAccessPermission();
        assertTrue(pdfPermissions.canPrint());
        assertTrue(pdfPermissions.canExtractContent());
        assertTrue(pdfPermissions.canExtractForAccessibility());
        assertTrue(pdfPermissions.canPrintDegraded());
        assertTrue(pdfPermissions.canModifyAnnotations());
        assertTrue(pdfPermissions.canModify());
        assertTrue(pdfPermissions.canFillInForm());
        assertTrue(pdfPermissions.canAssembleDocument());
        pdfDoc.close();
    }

    @Test
    public void testEncryptReadOnly() throws Exception {
        PDFEncryption pdfe = new PDFEncryption(pdfFileBlob);
        pdfe.setKeyLength(128);
        pdfe.setUserPwd(TestUtils.PDF_PROTECTED_USER_PASSWORD);
        pdfe.setOwnerPwd(TestUtils.PDF_PROTECTED_OWNER_PASSWORD);
        Blob result = pdfe.encryptReadOnly();
        assertNotNull(result);
        checkIsReadOnly(result, TestUtils.PDF_PROTECTED_OWNER_PASSWORD, TestUtils.PDF_PROTECTED_USER_PASSWORD);
    }

    @Test
    public void testRemoveEncryption() throws Exception {
        // verify that a given PDF file is encrypted
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_ENCRYPTED_PATH);
        try {
            PDDocument.load(f);
            fail();
        } catch (InvalidPasswordException e) {
            // ok
        }
        // decrypt that same PDF file and verify that the resulting PDF is now decrypted
        FileBlob fb = new FileBlob(f);
        PDFEncryption pdfe = new PDFEncryption(fb);
        pdfe.setOriginalOwnerPwd(TestUtils.PDF_ENCRYPTED_PASSWORD);
        Blob result = pdfe.removeEncryption();
        assertNotNull(result);
        PDDocument pdfDoc = PDDocument.load(result.getFile());
        assertFalse(pdfDoc.isEncrypted());
        pdfDoc.close();
        // decrypt a non-encrypted PDF file (should not trigger an error)
        pdfe = new PDFEncryption(pdfFileBlob);
        pdfe.setOriginalOwnerPwd(TestUtils.PDF_ENCRYPTED_PASSWORD);
        result = pdfe.removeEncryption();
        assertNotNull(result);
        pdfDoc = PDDocument.load(result.getFile());
        assertFalse(pdfDoc.isEncrypted());
        pdfDoc.close();
    }

    @Test
    public void testEncryptOperationSimple() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileBlob);
        chain.add(PDFEncryptOperation.ID)
            .set("originalOwnerPwd", TestUtils.PDF_PROTECTED_OWNER_PASSWORD)
            .set("ownerPwd", TestUtils.PDF_PROTECTED_OWNER_PASSWORD)
            .set("userPwd", TestUtils.PDF_PROTECTED_USER_PASSWORD);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        PDDocument originalPDF = PDDocument.load(pdfFileBlob.getFile());
        assertFalse(originalPDF.isEncrypted());
        originalPDF.close();
        PDDocument encryptedPDF = PDDocument.load(result.getFile(), TestUtils.PDF_PROTECTED_USER_PASSWORD);
        assertTrue(encryptedPDF.isEncrypted());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canModify());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canModifyAnnotations());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canPrint());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canPrintDegraded());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canExtractContent());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canExtractForAccessibility());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canAssembleDocument());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canFillInForm());
        encryptedPDF.close();
    }

    @Test
    public void testEncryptOperationComplex() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileBlob);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("modify", "true");
        properties.put("modifyannot", "true");
        properties.put("print", "false");
        properties.put("printdegraded", "false");
        properties.put("copy", "true");
        properties.put("extractforaccessibility", "true");
        chain.add(PDFEncryptOperation.ID)
            .set("originalOwnerPwd", TestUtils.PDF_PROTECTED_OWNER_PASSWORD)
            .set("ownerPwd", TestUtils.PDF_PROTECTED_OWNER_PASSWORD)
            .set("userPwd", TestUtils.PDF_PROTECTED_USER_PASSWORD)
            .set("keyLength", "40")
            .set("permissions", new Properties(properties));
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        PDDocument originalPDF = PDDocument.load(pdfFileBlob.getFile());
        assertFalse(originalPDF.isEncrypted());
        originalPDF.close();
        PDDocument encryptedPDF = PDDocument.load(result.getFile(), TestUtils.PDF_PROTECTED_USER_PASSWORD);
        assertTrue(encryptedPDF.isEncrypted());
        assertTrue(encryptedPDF.getCurrentAccessPermission().canModify());
        assertTrue(encryptedPDF.getCurrentAccessPermission().canModifyAnnotations());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canPrint());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canPrintDegraded());
        assertTrue(encryptedPDF.getCurrentAccessPermission().canExtractContent());
        assertTrue(encryptedPDF.getCurrentAccessPermission().canExtractForAccessibility());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canAssembleDocument());
        assertFalse(encryptedPDF.getCurrentAccessPermission().canFillInForm());
        encryptedPDF.close();
    }

    @Test
    public void testRemoveEncryptionOperation() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfEncryptedFileBlob);
        chain.add(PDFRemoveEncryptionOperation.ID)
            .set("ownerPwd", TestUtils.PDF_ENCRYPTED_PASSWORD);
        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);
        try {
            PDDocument.load(pdfEncryptedFileBlob.getFile());
            fail();
        } catch (InvalidPasswordException e) {
            // ok
        }
        PDDocument decryptedPDF = PDDocument.load(result.getFile());
        assertFalse(decryptedPDF.isEncrypted());
        decryptedPDF.close();
    }

    @Test
    public void testEncryptReadOnlyOperationBlob() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfFileBlob);
        chain.add(PDFEncryptReadOnlyOperation.ID)
            .set("ownerPwd", TestUtils.PDF_PROTECTED_OWNER_PASSWORD)
            .set("userPwd", TestUtils.PDF_PROTECTED_USER_PASSWORD);
        Blob result = (Blob) automationService.run(ctx, chain);
        checkIsReadOnly(result, TestUtils.PDF_PROTECTED_OWNER_PASSWORD, TestUtils.PDF_PROTECTED_USER_PASSWORD);
    }

    @Test
    public void testEncryptReadOnlyOperationBlobList() throws Exception {
        BlobList bl = new BlobList();
        bl.add(pdfFileBlob);
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_ENCRYPTED_PATH);
        bl.add(new FileBlob(f));
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(bl);
        chain.add(PDFEncryptReadOnlyOperation.ID)
            .set("originalOwnerPwd", TestUtils.PDF_ENCRYPTED_PASSWORD)
            .set("ownerPwd", TestUtils.PDF_PROTECTED_OWNER_PASSWORD)
            .set("userPwd", TestUtils.PDF_PROTECTED_USER_PASSWORD);
        BlobList result = (BlobList) automationService.run(ctx, chain);
        assertEquals(2, result.size());
        for(Blob b : result) {
            checkIsReadOnly(b, TestUtils.PDF_PROTECTED_OWNER_PASSWORD, TestUtils.PDF_PROTECTED_USER_PASSWORD);
        }
    }

}
