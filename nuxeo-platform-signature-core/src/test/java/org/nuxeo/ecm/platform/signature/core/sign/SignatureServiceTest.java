/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.CertUserService;
import org.nuxeo.ecm.platform.signature.api.user.RootService;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
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
    protected CertUserService certUserService;

    @Inject
    protected SignatureService signatureService;

    @Inject
    protected CoreSession session;

    // mark this true if you want to keep the signed pdf for verification
    private static final boolean KEEP_SIGNED_PDF = false;

    private static final String ROOT_KEY_PASSWORD = "abc";

    private static final String ROOT_KEYSTORE_PASSWORD = "abc";

    private static final String ROOT_USER_ID = "PDFCA";

    private static final String USER_KEY_PASSWORD = "abc";

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    /**
     * A test keystore file a user pdfca with pdfcacert and pdfkey entries
     */
    protected String keystorePath = "test-files/keystore.jks";

    private static final Log log = LogFactory.getLog(SignatureServiceTest.class);


    private File origPdfFile;

    DocumentModel user;

    protected String rootKeystorePath = "test-files/keystore.jks";

    private static final String USER_NAME = "hsimpson";

    /**
     * Replace root keystore from the config file with a custom one loaded from
     * a test resource file
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {

        UserManager userManager = Framework.getLocalService(UserManager.class);
        assertNotNull(userManager);

        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", USER_NAME);

        // delete the test user object if it exists
//        if(userManager.searchUsers(USER_NAME).size()>0){
//            userManager.deleteUser(userModel);
//        }
        userModel.setProperty("user", "firstName", "Homer");
        userModel.setProperty("user", "lastName", "Simpson");
        userModel.setProperty("user", "email", "simps@on.com");
        userModel.setPathInfo("/", USER_NAME);
        user = userManager.createUser(userModel);

        // delete the certificate test object if it exists
        if(certUserService.hasCertificateEntry(USER_NAME)){
            certUserService.deleteCertificateEntry(USER_NAME);
        }

        KeyStore rootKeystore = certService.getKeyStore(
                getKeystoreIS(keystorePath), ROOT_KEYSTORE_PASSWORD);
        RootService rootService = new RootService();
        AliasWrapper alias = new AliasWrapper(ROOT_USER_ID);
        rootService.setRootKeyAlias(alias.getId(AliasType.KEY));
        rootService.setRootCertificateAlias(alias.getId(AliasType.CERT));
        rootService.setRootKeyPassword(ROOT_KEY_PASSWORD);
        rootService.setRootKeyStore(rootKeystore);
        rootService.setRootKeystorePassword(ROOT_KEYSTORE_PASSWORD);
        certService.setRootService(rootService);
        origPdfFile = FileUtils.getResourceFileFromContext("pdf-tests/original.pdf");

        // Prerequisite: there is a user with a certificate that is to be used for document signing
        DocumentModel certificate = certUserService.createCert(userModel,
                USER_KEY_PASSWORD);
        assertNotNull(certificate.getPropertyValue("cert:keystore"));
        KeyStore keystore = certUserService.getUserKeystore(USER_NAME,
                USER_KEY_PASSWORD);
        assertNotNull(keystore);
    }

    @After
    public void destroy() throws Exception {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        DocumentModel userDM=userManager.getUserModel(USER_NAME);
        userManager.deleteUser(userDM);
    }

    @Test
    public void testSignPDF() throws Exception {
        File outputFile = signatureService.signPDF(user, USER_KEY_PASSWORD,
                "test reason", new FileInputStream(origPdfFile));
        assertTrue(outputFile.exists());
        if (KEEP_SIGNED_PDF) {
            log.info("SIGNED PDF: " + outputFile.getAbsolutePath());
        } else {
            outputFile.deleteOnExit();
        }
    }


    @Test
    public void testGetPDFCertificates() throws Exception {
        // sign the original PDF file
        File signedFile = signatureService.signPDF(user, USER_KEY_PASSWORD,
                "test reason", new FileInputStream(origPdfFile));
        assertTrue(signedFile.exists());
        // verify there are certificates in the signed file
        List<X509Certificate> certificates = signatureService.getPDFCertificates(new FileInputStream(signedFile));
        assertTrue("There has to be at least 1 certificate in a signed document", certificates.size()>0);
        assertTrue(certificates.get(0).getSubjectDN().toString().contains("CN=Homer Simpson"));
    }


    InputStream getKeystoreIS(String keystoreFilePath) throws Exception {
        File keystoreFile = FileUtils.getResourceFileFromContext(keystoreFilePath);
        return new FileInputStream(keystoreFile);
    }

    KeyStore getKeystore(String password) throws Exception {
        KeyStore keystore = certService.getKeyStore(getKeystoreIS(keystorePath),
                password);
        return keystore;
    }





    private UserInfo getSampleUserInfo(DocumentModel userModel)
            throws Exception {
        UserInfo userInfo = null;
        try {
            String userID = (String) userModel.getPropertyValue("user:username");
            String firstName = (String) userModel.getPropertyValue("user:firstName");
            String lastName = (String) userModel.getPropertyValue("user:lastName");
            String email = (String) userModel.getPropertyValue("user:email");

            Map<CNField, String> userFields;
            userFields = new HashMap<CNField, String>();

            userFields.put(CNField.C, "US");
            userFields.put(CNField.O, "Nuxeo");
            userFields.put(CNField.OU, "IT");

            userFields.put(CNField.CN, firstName + " " + lastName);
            userFields.put(CNField.Email, email);
            userFields.put(CNField.UserID, userID);
            userInfo = new UserInfo(userFields);
        } catch (ClientException e) {
            throw new CertException(
                    "User data could not be retrieved from the system", e);
        }
        return userInfo;
    }
}