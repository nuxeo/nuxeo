/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.core.sign;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryProxy;
import org.nuxeo.ecm.platform.signature.api.exception.AlreadySignedException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.signature.core.pki.RootServiceImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator", init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy( { "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api",
        "org.nuxeo.runtime.management", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.platform.usermanager",
        "org.nuxeo.ecm.platform.usermanager.api",
        "org.nuxeo.ecm.platform.signature.core",
        "org.nuxeo.ecm.platform.signature.core.test" })
public class SignatureServiceTest {

    @Inject
    protected CertService certService;

    @Inject
    protected CertService rootService;

    @Inject
    protected CUserService cUserService;

    @Inject
    protected SignatureService signatureService;

    @Inject
    protected CoreSession session;

    /**
     * A test keystore file a user pdfca with pdfcacert and pdfkey entries
     */

    // mark this true if you want to keep the signed pdf for manual
    // verification/preview
    private static final boolean KEEP_SIGNED_PDF = true;

    private static final String ROOT_KEY_PASSWORD = "abc";

    private static final String KEYSTORE_PASSWORD = "abc";

    private static final String ROOT_USER_ID = "PDFCA";

    private static final String USER_KEY_PASSWORD = "abc";

    private static final String CERTIFICATE_DIRECTORY_NAME = "certificate";

    private static final Log log = LogFactory.getLog(SignatureServiceTest.class);

    private File origPdfFile;

    private static final String KEYSTORE_PATH = "test-files/keystore.jks";

    private static DocumentModel user;
    private static DocumentModel user2;

    private static final String DEFAULT_USER_ID = "hSimpson";

    private static final String SECOND_USER_ID = "mSimpson";

    // Signing Prerequisite: a user with a certificate needs to be present
    @Before
    public void setup() throws Exception {
        // setup the naming service

//        setUpContextFactory();
        // pre-populate users & certificates
        DocumentModel user = getUser();
        DocumentModel certificate = cUserService.createCertificate(user,
                USER_KEY_PASSWORD);
        assertNotNull(certificate);

        DocumentModel user2 = getUser2();
        DocumentModel certificate2 = cUserService.createCertificate(user2,
                USER_KEY_PASSWORD);
        assertNotNull(certificate2);

        origPdfFile = FileUtils.getResourceFileFromContext("pdf-tests/original.pdf");
    }

    @After
    public void cleanup() throws Exception {

        // delete certificates associated with user ids
        Session sqlSession = getDirectoryService().open(
                CERTIFICATE_DIRECTORY_NAME);
        sqlSession.deleteEntry(DEFAULT_USER_ID);
        sqlSession.deleteEntry(SECOND_USER_ID);
        sqlSession.commit();
        sqlSession.close();

        // delete users
        UserManager userManager = Framework.getLocalService(UserManager.class);
        assertNotNull(userManager);
        if (userManager.getUserModel(DEFAULT_USER_ID) != null) {
            userManager.deleteUser(DEFAULT_USER_ID);
        }
        if (userManager.getUserModel(SECOND_USER_ID) != null) {
            userManager.deleteUser(SECOND_USER_ID);
        }
    }

    @Test
    public void testSignPDF() throws Exception {
        // first user signed

        DocumentModel defaultUser = getUser();
        DocumentModel secondUser = getUser2();

        File signedFile = signatureService.signPDF(defaultUser,
                USER_KEY_PASSWORD, "test reason", new FileInputStream(
                        origPdfFile));

        File fileSignedTwice = null;

        // try for the same user to sign the certificate again
        try {
            fileSignedTwice=signatureService.signPDF(defaultUser, USER_KEY_PASSWORD,
                    "test reason", new FileInputStream(signedFile));
        } catch (SignException sE) {
            if (sE.getCause().getClass().equals(AlreadySignedException.class)) {
                log.debug("Caught AlreadySignedException");
            } else {
                // rethrow if not the alreadySignedException
                throw sE;
            }
        }

        // try for the second user to sign the certificate, do not catch the
        // alreadySignedException here
        fileSignedTwice=signatureService.signPDF(secondUser, USER_KEY_PASSWORD, "test reason",
                new FileInputStream(signedFile));

        assertTrue(signedFile.exists());
        assertTrue(fileSignedTwice.exists());

        // test presence of multiple signatures
        PdfReader reader = new PdfReader(fileSignedTwice.getAbsolutePath());
        AcroFields af = reader.getAcroFields();
        ArrayList<String> names = af.getSignatureNames();
        log.debug("Registered signature names:");
        for (String signatureName : names) {
            log.debug(signatureName);
        }

        assertTrue(2 == names.size());

        if (KEEP_SIGNED_PDF) {
            log.info("SIGNED PDF: " + signedFile.getAbsolutePath());
        } else {
            signedFile.deleteOnExit();
        }
    }

    @Test
    public void testGetPDFCertificates() throws Exception {
        // sign the original PDF file
        File signedFile = signatureService.signPDF(getUser(),
                USER_KEY_PASSWORD, "test reason", new FileInputStream(
                        origPdfFile));
        assertTrue(signedFile.exists());
        // verify there are certificates in the signed file
        List<X509Certificate> certificates = signatureService.getPDFCertificates(new FileInputStream(
                signedFile));
        assertTrue(
                "There has to be at least 1 certificate in a signed document",
                certificates.size() > 0);
        assertTrue(certificates.get(0).getSubjectDN().toString().contains(
                "CN=Homer Simpson"));
        signedFile.deleteOnExit();
    }

    InputStream getKeystoreIS(String keystoreFilePath) throws Exception {
        File keystoreFile = FileUtils.getResourceFileFromContext(keystoreFilePath);
        return new FileInputStream(keystoreFile);
    }

    protected static Directory getDirectory(String dirName)
            throws DirectoryException {
        DirectoryServiceImpl dirServiceImpl = (DirectoryServiceImpl) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
        Directory dir = dirServiceImpl.getDirectory(dirName);
        if (dir instanceof SQLDirectoryProxy) {
            dir = ((SQLDirectoryProxy) dir).getDirectory();
        }
        return dir;
    }

    protected static DirectoryService getDirectoryService()
            throws ClientException {
        try {
            return Framework.getService(DirectoryService.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static UserManager getUserManager() {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        assertNotNull(userManager);
        return userManager;
    }

    public DocumentModel getUser() throws Exception {
        if (user == null) {
            DocumentModel userModel = getUserManager().getBareUserModel();
            userModel.setProperty("user", "username", DEFAULT_USER_ID);
            userModel.setProperty("user", "firstName", "Homer");
            userModel.setProperty("user", "lastName", "Simpson");
            userModel.setProperty("user", "email", "hsimpson@springfield.com");
            userModel.setPathInfo("/", DEFAULT_USER_ID);
            user = getUserManager().createUser(userModel);
        }
        return user;
    }

    public DocumentModel getUser2() throws Exception {
        if (user2 == null) {
            DocumentModel userModel = getUserManager().getBareUserModel();
            userModel.setProperty("user", "username", SECOND_USER_ID);
            userModel.setProperty("user", "firstName", "Marge");
            userModel.setProperty("user", "lastName", "Simpson");
            userModel.setProperty("user", "email", "msimpson@springfield.com");
            userModel.setPathInfo("/", SECOND_USER_ID);
            user2 = getUserManager().createUser(userModel);
        }
        return user2;
    }

    public UserInfo getUserInfo(String userID) throws Exception {
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.C, "US");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.UserID, userID);
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }

    public CertService getCertServiceMock() throws Exception {
        KeyStore rootKeystore = certService.getKeyStore(
                getKeystoreIS(KEYSTORE_PATH), KEYSTORE_PASSWORD);
        RootService rootService = new RootServiceImpl();
        AliasWrapper alias = new AliasWrapper(ROOT_USER_ID);
        rootService.setRootKeyAlias(alias.getId(AliasType.KEY));
        rootService.setRootCertificateAlias(alias.getId(AliasType.CERT));
        rootService.setRootKeyPassword(ROOT_KEY_PASSWORD);
        rootService.setRootKeyStore(rootKeystore);
        rootService.setRootKeystorePassword(KEYSTORE_PASSWORD);
        certService.setRootService(rootService);
        return certService;
    }
}