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

package key;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import store.StoreService;


/**
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
public class CertServiceImpl implements CertService {

    private CertServiceConfigDescriptor descriptor;
    private static int DEFAULT_KEY_CHAIN_SIZE=2;


    @Override
    public Certificate createCertificate(KeyPair keyPair, CertInfo certInfo) throws Exception{
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal("CN="+certInfo.getUserDN()));
        certGen.setSubjectDN(new X500Principal("CN="+certInfo.getCertSubject()));
        certGen.setNotBefore(new Date(System.currentTimeMillis() - certInfo.getValidMillisBefore()));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + certInfo.getValidMillisAfter()));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(certInfo.getCertSignatureAlgorithm());
        Certificate cert=certGen.generate(keyPair.getPrivate(), Security.getProvider(certInfo.getSecurityProviderName()).getName());
        return cert;
    }

    public int getCertChainSize(){
        return DEFAULT_KEY_CHAIN_SIZE;
    }

    @Override
    public Certificate retrieveCertificate(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void storeCertificate(Certificate cert, StoreService store) {
        // TODO Auto-generated method stub
    }

    public CertServiceConfigDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(CertServiceConfigDescriptor descriptor) {
        this.descriptor = descriptor;
    }
}