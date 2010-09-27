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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
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
public class SignatureServiceTest {

    @Inject
    protected CertService certService;

    @Inject
    protected CAService cAService;

    @Inject
    protected CoreSession session;

    @Inject
    SignatureService signatureService;

    private static final Log log = LogFactory.getLog(SignatureServiceTest.class);

    @Before
    public void setUp() throws Exception {
        String rootCertFilePath = "test-files/root.crt";
        File rootFile = FileUtils.getResourceFileFromContext(rootCertFilePath);
        X509Certificate rootCertificate=cAService.getCertificate(rootFile);
        cAService.setRootCertificate(rootCertificate);

    }


    @Test
    public void testSignPDF() throws Exception {
        InputStream origPdf = new FileInputStream(
                FileUtils.getResourceFileFromContext("pdf-tests/original.pdf"));
        File outputFile = signatureService.signPDF(getCertInfo(), origPdf);
        assertTrue(outputFile.exists());
        //TODO add signature verification
        outputFile.deleteOnExit();
    }

    protected CertInfo getCertInfo() throws Exception {
        CertInfo certInfo = new CertInfo();
        certInfo.setUserID("100");
        certInfo.setUserName("Wojciech Sulejman");
        certInfo.setUserDN("CN=Wojciech Sulejman, OU=StarUs, O=Nuxeo, C=US");
        certInfo.setKeyAlgorithm("RSA");
        certInfo.setNumBits(1024);
        certInfo.setCertSignatureAlgorithm("SHA256WithRSAEncryption");

        Calendar cal=Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MONTH, 12);//add a year from now
        Date inAYear=cal.getTime();

        certInfo.setValidFrom(now);
        certInfo.setValidTo(inAYear);
        log.error(now+" : "+inAYear);
        certInfo.setSigningReason("Nuxeo signed document");
        return certInfo;
    }

    private DateFormat getFormatter() {
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
        return formatter;
    }

}