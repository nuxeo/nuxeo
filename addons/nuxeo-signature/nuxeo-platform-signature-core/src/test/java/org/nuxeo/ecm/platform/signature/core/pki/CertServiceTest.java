package org.nuxeo.ecm.platform.signature.core.pki;
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



import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
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
        "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.platform.signature.core",
        "org.nuxeo.ecm.platform.signature.core.test" })
public class CertServiceTest {

    @Inject
    protected CertService certService;

    @Inject
    protected CoreSession session;

    protected File userCertFile;

    protected X509Certificate rootCertificate;

    private static final int EXPECTED_MIN_ENCODED_CERT_LENGTH = 100;

    private static final String ROOT_KEY_PASSWORD = "abc";
    private static final String ROOT_KEYSTORE_PASSWORD = "abc";
    private static final String ROOT_USER_ID= "PDFCA";
    private static final String USER_KEY_PASSWORD = "abc";
    private static final String USER_KEYSTORE_PASSWORD = "abc";

    /**
     * A test keystore file a user pdfca with pdfcacert and pdfkey entries
     */
    protected String keystorePath = "test-files/keystore.jks";

    /**
     * Replace root keystore from the config file with a custom one
     * loaded from a test resource file
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        KeyStore rootKeystore=certService.getKeyStore(getKeystoreIS(keystorePath), ROOT_KEYSTORE_PASSWORD);
        RootService rootService=new RootServiceImpl();
        AliasWrapper alias = new AliasWrapper(ROOT_USER_ID);
        rootService.setRootKeyAlias(alias.getId(AliasType.KEY));
        rootService.setRootCertificateAlias(alias.getId(AliasType.CERT));
        rootService.setRootKeyPassword(ROOT_KEY_PASSWORD);
        rootService.setRootKeyStore(rootKeystore);
        rootService.setRootKeystorePassword(ROOT_KEYSTORE_PASSWORD);
        certService.setRootService(rootService);
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

    @Test
    public void testGetKeys() throws Exception {
        String userID = getPDFCAInfo().getUserFields().get(CNField.UserID);
        AliasWrapper alias = new AliasWrapper(userID);
        String keyAliasName = alias.getId(AliasType.KEY);
        String certificateAliasName = alias.getId(AliasType.CERT);
        KeyPair keyPair = certService.getKeyPair(getKeystore(USER_KEYSTORE_PASSWORD), keyAliasName,
                certificateAliasName, USER_KEY_PASSWORD);
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    public void testGetCertificate() throws Exception {
        KeyStore keystore = generateUserKeystore();
        Certificate cert = certService.getCertificate(keystore, getAliasId(
                getUserInfo(), AliasType.CERT));
        assertNotNull(cert.getPublicKey());
        assertTrue(cert.getPublicKey().getEncoded().length > EXPECTED_MIN_ENCODED_CERT_LENGTH);
    }

    @Test
    public void testInitializeUser() throws Exception {
        KeyStore keystore = generateUserKeystore();
        assertNotNull(keystore.containsAlias(getUserInfo().getUserFields().get(
                CNField.UserID)));
    }

    public KeyStore generateUserKeystore() throws Exception {
        KeyStore keystore = certService.initializeUser(getUserInfo(), USER_KEYSTORE_PASSWORD);
        return keystore;
    }


    private String getAliasId(UserInfo userInfo, AliasType aliasType) {
        AliasWrapper alias = new AliasWrapper(userInfo.getUserFields().get(CNField.UserID));
        return alias.getId(aliasType);
    }

    public UserInfo getPDFCAInfo() throws Exception {
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.CN, "PDFCA");
        userFields.put(CNField.C, "US");
        userFields.put(CNField.OU, "CA");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.UserID, "PDFCA");
        userFields.put(CNField.Email, "pdfca@nuxeo.com");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }

    public UserInfo getUserInfo() throws Exception {
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.C, "US");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.UserID, "wsulejman");
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }
}