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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
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
        "org.nuxeo.runtime.management",
        "org.nuxeo.ecm.platform.signature.core",
        "org.nuxeo.ecm.platform.signature.core.test" })
public class CAServiceTest {

    @Inject
    protected CAService cAService;

    @Inject
    protected CoreSession session;

    protected File userCertFile;

    protected X509Certificate rootCertificate;

    private static final int EXPECTED_MIN_ENCODED_CERT_LENGTH = 100;

    private final String PASSWORD = "abc";

    protected String keystorePath = "test-files/keystore.jks";

    @Before
    public void setup() throws Exception {
        ((MockCAServiceImpl) cAService).setRoot(keystorePath, getPDFCAInfo());
    }

    InputStream getKeystoreIS(String keystoreFilePath) throws Exception {
        File keystoreFile = FileUtils.getResourceFileFromContext(keystoreFilePath);
        return new FileInputStream(keystoreFile);
    }

    KeyStore getKeystore() throws Exception {
        KeyStore keystore = cAService.getKeyStore(getKeystoreIS(keystorePath),
                getPDFCAInfo(), PASSWORD);
        return keystore;
    }

    @Test
    public void testGetKeys() throws Exception {
        KeyPair keyPair = cAService.getKeyPair(getKeystore(), getPDFCAInfo(),
                PASSWORD);
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    public void testGetCertificate() throws Exception {
        KeyStore keystore=generateUserKeystore();
        Certificate cert = cAService.getCertificate(keystore,
                getUserInfo());
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
        KeyStore keystore = cAService.initializeUser(getUserInfo(), PASSWORD);
        return keystore;
    }

    public CAService getCAService() throws Exception {
        if (cAService == null) {
            cAService = Framework.getService(CAService.class);
        }
        return cAService;
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