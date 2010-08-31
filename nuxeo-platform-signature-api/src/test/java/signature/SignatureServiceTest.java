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
 *     ws@nuxeo.com
 */


package signature;

import static org.junit.Assert.assertTrue;
import key.CertInfo;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
public class SignatureServiceTest {

    //TODO will be injected after refactoring
    SignatureService signatureService = new SignatureServiceImpl();

    @Test
    public void testSignPDF() {
        String origPdfPath= FileUtils.getResourceFileFromContext("pdf-tests/original.pdf").getAbsolutePath();
        String outputPdfPath=FileUtils.getResourceFileFromContext("pdf-tests").getAbsolutePath()+System.getProperty("file.separator")+"signed.pdf";
        CertInfo certInfo=new CertInfo();
        certInfo.setSecurityProviderName("BC");//BouncyCastle
        certInfo.setUserID("100");
        certInfo.setCertSubject("Subject for the PDF signature test");
        certInfo.setKeyAlgorithm("RSA");
        certInfo.setNumBits(1024);
        certInfo.setCertSignatureAlgorithm("SHA256WithRSAEncryption");
        certInfo.setValidMillisBefore(0);
        certInfo.setValidMillisAfter(1000000);
        Status executionStatus=signatureService.signPDF(certInfo,origPdfPath,outputPdfPath);
        assertTrue(Status.OK==executionStatus);
        if(Status.OK==executionStatus){
            System.out.println("Check this PDF's signature: "+outputPdfPath);
        }
    }
}