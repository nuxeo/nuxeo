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
package org.nuxeo.ecm.platform.signature.core.pki;

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

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy({ "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api", "org.nuxeo.runtime.management",
        "org.nuxeo.ecm.platform.signature.core", "org.nuxeo.ecm.platform.signature.core.test" })
public class CertServiceTest {

    @Inject
    protected CertService certService;

    private static final int EXPECTED_MIN_ENCODED_CERT_LENGTH = 100;

    private static final String ROOT_KEY_PASSWORD = "abc";

    private static final String ROOT_KEYSTORE_PASSWORD = "abc";

    private static final String ROOT_USER_ID = "PDFCA";

    private static final String USER_KEY_PASSWORD = "abc";

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    private static final String KEYSTORE_PATH = "test-files/keystore.jks";

    /**
     * Replace root keystore from the config file with a custom one loaded from a test resource file
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        KeyStore rootKeystore = certService.getKeyStore(getKeystoreIS(KEYSTORE_PATH), ROOT_KEYSTORE_PASSWORD);
        RootService rootService = new RootServiceImpl();
        AliasWrapper alias = new AliasWrapper(ROOT_USER_ID);
        rootService.setRootKeyAlias(alias.getId(AliasType.KEY));
        rootService.setRootCertificateAlias(alias.getId(AliasType.CERT));
        rootService.setRootKeyPassword(ROOT_KEY_PASSWORD);
        rootService.setRootKeyStore(rootKeystore);
        rootService.setRootKeystorePassword(ROOT_KEYSTORE_PASSWORD);
        certService.setRootService(rootService);
    }

    protected InputStream getKeystoreIS(String keystoreFilePath) throws Exception {
        File keystoreFile = FileUtils.getResourceFileFromContext(keystoreFilePath);
        return new FileInputStream(keystoreFile);
    }

    @Test
    public void testGetKeys() throws Exception {
        String userID = getPDFCAInfo().getUserFields().get(CNField.UserID);
        AliasWrapper alias = new AliasWrapper(userID);
        String keyAliasName = alias.getId(AliasType.KEY);
        String certificateAliasName = alias.getId(AliasType.CERT);
        KeyStore keystore = certService.getKeyStore(getKeystoreIS(KEYSTORE_PATH), USER_KEYSTORE_PASSWORD);
        KeyPair keyPair = certService.getKeyPair(keystore, keyAliasName, certificateAliasName, USER_KEY_PASSWORD);
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    public void testGetCertificate() throws Exception {
        KeyStore keystore = generateUserKeystore();
        Certificate cert = certService.getCertificate(keystore, getAliasId(getUserInfo(), AliasType.CERT));
        assertNotNull(cert.getPublicKey());
        assertTrue(cert.getPublicKey().getEncoded().length > EXPECTED_MIN_ENCODED_CERT_LENGTH);
    }

    @Test
    public void testInitializeUser() throws Exception {
        KeyStore keystore = generateUserKeystore();
        String userid = getUserInfo().getUserFields().get(CNField.UserID);
        assertTrue(keystore.containsAlias(userid + "key"));
        assertTrue(keystore.containsAlias(userid + "cert"));
    }

    protected KeyStore generateUserKeystore() throws Exception {
        KeyStore keystore = certService.initializeUser(getUserInfo(), USER_KEYSTORE_PASSWORD);
        return keystore;
    }

    protected String getAliasId(UserInfo userInfo, AliasType aliasType) {
        AliasWrapper alias = new AliasWrapper(userInfo.getUserFields().get(CNField.UserID));
        return alias.getId(aliasType);
    }

    protected UserInfo getPDFCAInfo() throws Exception {
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

    protected UserInfo getUserInfo() throws Exception {
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
