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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.signature.core.pki.CAServiceImpl;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class MockCAServiceImpl extends CAServiceImpl {

    private static final String ROOT_PASSWORD = "abc";

    public void setRoot(String keystoreFilePath, UserInfo pdfCAInfo) throws CertException{
        File keystoreFile = FileUtils.getResourceFileFromContext(keystoreFilePath);
        X509Certificate certificate;
        try {
            KeyStore ks=this.getKeyStore(new FileInputStream(keystoreFile), getRootUserInfo(), ROOT_PASSWORD);
            this.setRootKeyStore(ks);
            this.setRootPassword(ROOT_PASSWORD);
            this.setRootUserInfo(pdfCAInfo);
            certificate = this.getCertificate(ks, getRootUserInfo());
        } catch (FileNotFoundException e) {
             throw new CertException(e);
        }
        this.setRootCertificate(certificate);
    }
}
