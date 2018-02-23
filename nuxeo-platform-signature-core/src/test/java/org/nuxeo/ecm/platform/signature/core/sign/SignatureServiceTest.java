/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Wojciech Sulejman
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.signature.core.sign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.SIGNED_CURRENT;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.SIGNED_OTHER;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.UNSIGNABLE;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.UNSIGNED;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.exception.AlreadySignedException;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService.SigningDisposition;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.signature.core.SignatureCoreFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.lowagie.text.pdf.PdfReader;

@RunWith(FeaturesRunner.class)
@Features(SignatureCoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.usermanager.api")
@Deploy("org.nuxeo.ecm.platform.convert")
public class SignatureServiceTest {

    @Inject
    protected CUserService cUserService;

    @Inject
    protected SignatureService signatureService;

    @Inject
    protected UserManager userManager;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected CoreSession session;

    private static final String ORIGINAL_PDF = "pdf-tests/original.pdf";

    private static final String SIGNED_PDF = "pdf-tests/signed.pdf";

    private static final String HELLO_TXT = "pdf-tests/hello.txt";

    private static final String USER_KEY_PASSWORD = "abc";

    private static final String CERTIFICATE_DIRECTORY_NAME = "certificate";

    private static final String DEFAULT_USER_ID = "hSimpson";

    private static final String SECOND_USER_ID = "mSimpson";

    private File origPdfFile;

    private File signedPdfFile;

    private File helloTxtFile;

    private DocumentModel user;

    private DocumentModel user2;

    /**
     * Signing Prerequisite: a user with a certificate needs to be present
     */
    @Before
    public void setUp() {

        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", DEFAULT_USER_ID);
        userModel.setProperty("user", "firstName", "Homer");
        userModel.setProperty("user", "lastName", "Simpson");
        userModel.setProperty("user", "email", "hsimpson@springfield.com");
        userModel.setPathInfo("/", DEFAULT_USER_ID);
        user = userManager.createUser(userModel);

        userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", SECOND_USER_ID);
        userModel.setProperty("user", "firstName", "Marge");
        userModel.setProperty("user", "lastName", "Simpson");
        userModel.setProperty("user", "email", "msimpson@springfield.com");
        userModel.setPathInfo("/", SECOND_USER_ID);
        user2 = userManager.createUser(userModel);

        DocumentModel certificate = cUserService.createCertificate(user, USER_KEY_PASSWORD);
        assertNotNull(certificate);

        DocumentModel certificate2 = cUserService.createCertificate(user2, USER_KEY_PASSWORD);
        assertNotNull(certificate2);

        origPdfFile = FileUtils.getResourceFileFromContext(ORIGINAL_PDF);
        signedPdfFile = FileUtils.getResourceFileFromContext(SIGNED_PDF);
        helloTxtFile = FileUtils.getResourceFileFromContext(HELLO_TXT);
    }

    @After
    public void tearDown() {

        // delete certificates associated with user ids
        try (Session sqlSession = directoryService.open(CERTIFICATE_DIRECTORY_NAME)) {
            sqlSession.deleteEntry(DEFAULT_USER_ID);
            sqlSession.deleteEntry(SECOND_USER_ID);
        }

        // delete users
        userManager.deleteUser(DEFAULT_USER_ID);
        userManager.deleteUser(SECOND_USER_ID);
    }

    @Test
    public void testSignPDF() throws Exception {
        SignatureServiceImpl ssi = (SignatureServiceImpl) signatureService;

        // first user signs
        Blob origBlob = Blobs.createBlob(origPdfFile);
        assertEquals(UNSIGNED, ssi.getSigningStatus(origBlob, user));
        Blob signedBlob = signatureService.signPDF(origBlob, null, user, USER_KEY_PASSWORD, "test reason");
        assertNotNull(signedBlob);
        assertEquals(SIGNED_CURRENT, ssi.getSigningStatus(signedBlob, user));
        assertEquals(SIGNED_OTHER, ssi.getSigningStatus(signedBlob, user2));

        // try for the same user to sign the certificate again
        try {
            signatureService.signPDF(signedBlob, null, user, USER_KEY_PASSWORD, "test reason");
            fail("Should raise AlreadySignedException");
        } catch (AlreadySignedException e) {
            // ok
        }

        // try for the second user to sign the certificate
        Blob signedBlobTwice = signatureService.signPDF(signedBlob, null, user2, USER_KEY_PASSWORD, "test reason");
        assertNotNull(signedBlobTwice);
        assertEquals(SIGNED_CURRENT, ssi.getSigningStatus(signedBlobTwice, user));
        assertEquals(SIGNED_CURRENT, ssi.getSigningStatus(signedBlobTwice, user2));

        // test presence of multiple signatures
        List<String> names = getSignatureNames(signedBlobTwice);
        assertEquals(2, names.size());
        assertEquals(Arrays.asList("Signature2", "Signature1"), names);
    }

    protected List<String> getSignatureNames(Blob blob) throws IOException {
        PdfReader reader = new PdfReader(blob.getStream());
        try {
            @SuppressWarnings("unchecked")
            List<String> names = reader.getAcroFields().getSignatureNames();
            return names;
        } finally {
            reader.close();
        }
    }

    @Test
    public void testGetCertificates() throws Exception {
        SignatureServiceImpl ssi = (SignatureServiceImpl) signatureService;

        // sign the original PDF file
        Blob signedBlob = signatureService.signPDF(Blobs.createBlob(origPdfFile), null, user, USER_KEY_PASSWORD,
                "test reason");
        assertNotNull(signedBlob);
        // verify there are certificates in the signed file
        List<X509Certificate> certificates = ssi.getCertificates(signedBlob);
        assertTrue("There has to be at least 1 certificate in a signed document", certificates.size() > 0);
        assertTrue(certificates.get(0).getSubjectDN().toString().contains("CN=Homer Simpson"));
    }

    @Test
    public void testGetSigningStatus() throws Exception {
        Serializable pdfBlob = (Serializable) Blobs.createBlob(origPdfFile, "application/pdf");
        Serializable signedBlob = (Serializable) Blobs.createBlob(signedPdfFile, "application/pdf");
        Blob otherBlob = Blobs.createBlob("foo", "application/octet-stream");
        DocumentModel doc = session.createDocumentModel("File");
        StatusWithBlob swb;

        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(UNSIGNABLE, swb.status);
        assertNull(swb.path);
        assertNull(swb.blob);

        // unsigned PDF in main file
        doc.setPropertyValue("file:content", pdfBlob);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(UNSIGNED, swb.status);
        assertEquals("file:content", swb.path);
        assertEquals(pdfBlob, swb.blob);

        // signed PDF in main file
        doc.setPropertyValue("file:content", signedBlob);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(SIGNED_OTHER, swb.status);
        assertEquals("file:content", swb.path);
        assertEquals(signedBlob, swb.blob);

        // not PDF in main file
        doc.setPropertyValue("file:content", (Serializable) otherBlob);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(UNSIGNED, swb.status);
        assertEquals("file:content", swb.path);
        assertEquals(otherBlob, swb.blob);

        // no files attached
        doc.setPropertyValue("file:content", null);
        doc.setPropertyValue("files:files", null);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(UNSIGNABLE, swb.status);
        assertNull(swb.path);
        assertNull(swb.blob);
        doc.setPropertyValue("files:files", (Serializable) Collections.emptyList());
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(UNSIGNABLE, swb.status);
        assertNull(swb.path);
        assertNull(swb.blob);

        // unsigned PDF attached
        List<Map<String, Serializable>> fileList = new ArrayList<>();
        fileList.add(Collections.singletonMap("file", pdfBlob));
        doc.setPropertyValue("files:files", (Serializable) fileList);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(UNSIGNABLE, swb.status);
        assertNull(swb.path);
        assertNull(swb.blob);

        // signed PDF attached second
        fileList.clear();
        fileList.add(Collections.singletonMap("file", pdfBlob));
        fileList.add(Collections.singletonMap("file", signedBlob));
        doc.setPropertyValue("files:files", (Serializable) fileList);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(SIGNED_OTHER, swb.status);
        assertEquals("files:files/1/file", swb.path);
        assertEquals(signedBlob, swb.blob);

        // and PDF as main file
        doc.setPropertyValue("file:content", pdfBlob);
        swb = signatureService.getSigningStatus(doc, null);
        assertEquals(SIGNED_OTHER, swb.status);
        assertEquals("files:files/1/file", swb.path);
        assertEquals(signedBlob, swb.blob);
    }

    @Test
    public void testSignDocumentReplace() throws Exception {
        Blob txtBlob = Blobs.createBlob(helloTxtFile, "text/plain", null, "foo.txt");
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) txtBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.REPLACE, null);

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(signedBlob));
        assertEquals(signedBlob, doc.getPropertyValue("file:content"));
        assertEquals(Collections.emptyList(), doc.getPropertyValue("files:files"));
    }

    @Test
    public void testSignDocumentAttach() throws Exception {
        Blob txtBlob = Blobs.createBlob(helloTxtFile, "text/plain", null, "foo.txt");
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) txtBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.ATTACH, null);

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(signedBlob));
        assertEquals(txtBlob, doc.getPropertyValue("file:content"));
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
        assertEquals(1, files.size());
        assertEquals(signedBlob, files.get(0).get("file"));
    }

    @Test
    public void testSignDocumentArchive() throws Exception {
        Blob txtBlob = Blobs.createBlob(helloTxtFile, "text/plain", null, "foo.txt");
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) txtBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.ARCHIVE, "foo archive.txt");

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(signedBlob));
        assertEquals(signedBlob, doc.getPropertyValue("file:content"));
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
        assertEquals(1, files.size());
        Blob archivedBlob = (Blob) files.get(0).get("file");
        assertEquals("text/plain", archivedBlob.getMimeType());
        assertEquals("foo archive.txt", archivedBlob.getFilename());
    }

    @Test
    public void testSignPDFDocumentReplace() throws Exception {
        Blob pdfBlob = Blobs.createBlob(origPdfFile, "application/pdf", null, "foo.pdf");
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) pdfBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.REPLACE, null);

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(signedBlob));
        assertEquals(signedBlob, doc.getPropertyValue("file:content"));
        assertEquals(Collections.emptyList(), doc.getPropertyValue("files:files"));
    }

    @Test
    public void testSignPDFDocumentAttach() throws Exception {
        Blob pdfBlob = Blobs.createBlob(origPdfFile, "application/pdf", null, "foo.pdf");
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) pdfBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.ATTACH, null);

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(signedBlob));
        assertEquals(pdfBlob, doc.getPropertyValue("file:content"));
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
        assertEquals(1, files.size());
        assertEquals(signedBlob, files.get(0).get("file"));
    }

    @Test
    public void testSignPDFDocumentArchive() throws Exception {
        Blob pdfBlob = Blobs.createBlob(origPdfFile, "application/pdf", null, "foo.pdf");
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) pdfBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.ARCHIVE, "foo archive.pdf");

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(signedBlob));
        assertEquals(signedBlob, doc.getPropertyValue("file:content"));
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
        assertEquals(1, files.size());
        Blob archivedBlob = (Blob) files.get(0).get("file");
        assertEquals("application/pdf", archivedBlob.getMimeType());
        assertEquals("foo archive.pdf", archivedBlob.getFilename());
    }

    @Test
    public void testResignDocument() throws Exception {
        Blob pdfBlob = Blobs.createBlob(signedPdfFile, "application/pdf", null, "foo.pdf");
        assertEquals(Collections.singletonList("Signature1"), getSignatureNames(pdfBlob));

        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", (Serializable) pdfBlob);

        Blob signedBlob = signatureService.signDocument(doc, user, USER_KEY_PASSWORD, "test", false,
                SigningDisposition.REPLACE, null);

        assertEquals("foo.pdf", signedBlob.getFilename());
        assertEquals(Arrays.asList("Signature2", "Signature1"), getSignatureNames(signedBlob));
    }
    
    @Test public void testGetDefaultSignatureAppearance() throws Exception {
        SignatureServiceImpl ssi = (SignatureServiceImpl) signatureService;
        assertNotNull(ssi.getSignatureAppearanceFactory());
    }

}
