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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.core.sign.SignatureServiceImpl;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class SignatureServiceTest {

    SignatureService signatureService = new SignatureServiceImpl();

    private static final Log log = LogFactory.getLog(SignatureServiceTest.class);

    @Test
    public void testSignPDF() throws Exception {
        InputStream origPdf = new FileInputStream(
                FileUtils.getResourceFileFromContext("pdf-tests/original.pdf"));
        CertInfo certInfo = new CertInfo();
        certInfo.setSecurityProviderName("BC");// BouncyCastle
        certInfo.setUserID("100");
        certInfo.setUserName("Wojciech Sulejman"); // userName
        certInfo.setKeyAlgorithm("RSA");
        certInfo.setNumBits(1024);
        certInfo.setCertSignatureAlgorithm("SHA256WithRSAEncryption");
        certInfo.setValidMillisBefore(0);
        certInfo.setValidMillisAfter(1000000);
        certInfo.setSigningReason("Nuxeo signed document");// to be provided via
                                                           // web form
        File outputFile = signatureService.signPDF(certInfo, origPdf);
        assertTrue(outputFile.exists());
        log.info("Check this PDF's signature: " + outputFile.getAbsolutePath());
    }
}