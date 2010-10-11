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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.core.pki;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
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
        "org.nuxeo.runtime.management", "org.nuxeo.ecm.platform.signature.core" })
public class CertServiceTest {

    private final static Log log = LogFactory.getLog(CertServiceTest.class);

    @Inject
    protected CertService certService;

    @Inject
    protected KeyService keyService;

    @Inject
    protected CAService cAService;

    @Inject
    protected CoreSession session;

    protected File userCertFile;

    protected X509Certificate rootCertificate;

    @Before
    public void setUp() throws Exception {
        String outputPath = FileUtils.getResourcePathFromContext("test-files");
        String userFilePath = outputPath + "/user.crt";
        userCertFile = new File(userFilePath);
        if (userCertFile.exists()) {
            FileUtils.deleteTree(userCertFile);
        }

        // set root certificate
        String rootCertFilePath = "test-files/root.crt";
        File rootFile = FileUtils.getResourceFileFromContext(rootCertFilePath);
        rootCertificate = cAService.getCertificate(rootFile);
        cAService.setRootCertificate(rootCertificate);
    }

    /**
     * Test method for .
     * {@link org.nuxeo.ecm.platform.signature.api.pki.CertService#createCertificate(java.security.KeyPair, org.nuxeo.ecm.platform.signature.api.pki.CertInfo)}
     */
    @Test
    public void testCreateCertificate() throws Exception {
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) certService.generateCSR(getUserInfo());

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        OutputStream outputStream = new FileOutputStream(userCertFile);

        // read the saved certificate from file and verify it
        X509Certificate userCertificate = cAService.createCertificateFromCSR(
                csr, getUserInfo());
        bOut.write(userCertificate.getEncoded());
        outputStream = new FileOutputStream(userCertFile);
        bOut.writeTo(outputStream);
        bOut.close();

        X509Certificate userCertificateFromFile = certService.getCertificate(userCertFile);

        assertTrue(userCertificateFromFile.getSubjectDN().getName().equals(
                userCertificate.getSubjectDN().getName()));
        assertFalse(userCertificateFromFile.getSubjectDN().getName().equals(
                rootCertificate.getSubjectDN().getName()));


        boolean deleteGeneratedCertificateFiles = true;
        if (deleteGeneratedCertificateFiles) {
            userCertFile.deleteOnExit();
        }
    }

    public UserInfo getUserInfo() throws Exception{
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.C, "US");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        userFields.put(CNField.UserID, "wsulejman");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }


}