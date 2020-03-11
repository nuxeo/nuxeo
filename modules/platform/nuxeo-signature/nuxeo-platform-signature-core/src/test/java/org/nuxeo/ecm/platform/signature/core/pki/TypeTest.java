/*
 * (C) Copyright 2011-2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.signature.core.SignatureCoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(SignatureCoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TypeTest {

    @Inject
    protected CertService certService;

    @Inject
    private DirectoryService directoryService;

    private static final String ROOT_KEY_PASSWORD = "abc";

    private static final String ROOT_KEYSTORE_PASSWORD = "abc";

    private static final String ROOT_USER_ID = "PDFCA";

    private static final String USER_KEY_PASSWORD = "abc";

    private static final String USER_KEYSTORE_PASSWORD = "abc";

    private static final String KEYSTORE_PATH = "test-files/keystore.jks";

    @Before
    public void setUp() throws Exception {
        File keystoreFile = FileUtils.getResourceFileFromContext(KEYSTORE_PATH);
        InputStream keystoreIS = new FileInputStream(keystoreFile);
        KeyStore rootKeystore = certService.getKeyStore(keystoreIS, ROOT_KEYSTORE_PASSWORD);
        RootService rootService = new RootServiceImpl();
        AliasWrapper alias = new AliasWrapper(ROOT_USER_ID);
        rootService.setRootKeyAlias(alias.getId(AliasType.KEY));
        rootService.setRootCertificateAlias(alias.getId(AliasType.CERT));
        rootService.setRootKeyPassword(ROOT_KEY_PASSWORD);
        rootService.setRootKeyStore(rootKeystore);
        rootService.setRootKeystorePassword(ROOT_KEYSTORE_PASSWORD);
        certService.setRootService(rootService);
    }

    @Test
    public void savePropertiesToDirectory() throws Exception {
        // create an entry in the directory
        String userID = "testUserID2";
        try (Session sqlSession = directoryService.open("certificate")) {
            Map<String, Object> map = new HashMap<>();
            map.put("userid", userID);

            // add a keystore to the entry
            KeyStore keystore = certService.initializeUser(getUserInfo(userID), USER_KEY_PASSWORD);
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            keystore.store(byteOS, USER_KEYSTORE_PASSWORD.toCharArray());
            String keystore64Encoded = Base64.encodeBase64String(byteOS.toByteArray());
            map.put("keystore", keystore64Encoded);

            DocumentModel entry = sqlSession.createEntry(map);
            assertNotNull(entry);

            // retrieve a persisted entry from the directory
            DocumentModel entryFromSession = sqlSession.getEntry(userID);
            String keystore64EncodedFromSession = (String) entryFromSession.getPropertyValue("cert:keystore");
            byte[] keystoreBytes = Base64.decodeBase64(keystore64EncodedFromSession);
            ByteArrayInputStream keystoreByteIS = new ByteArrayInputStream(keystoreBytes);
            keystore.load(keystoreByteIS, USER_KEYSTORE_PASSWORD.toCharArray());
            AliasWrapper userAlias = new AliasWrapper(userID);

            // check if you can read an existing alias from the persisted keystore
            assertTrue(keystore.containsAlias(userAlias.getId(AliasType.KEY)));

            sqlSession.deleteEntry(userID);
        }
    }

    public UserInfo getUserInfo(String userID) {
        Map<CNField, String> userFields;
        userFields = new HashMap<>();
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.C, "US");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.UserID, userID);
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        return new UserInfo(userFields);
    }

}
