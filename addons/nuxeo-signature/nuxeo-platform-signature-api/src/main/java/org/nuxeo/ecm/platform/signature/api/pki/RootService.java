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
package org.nuxeo.ecm.platform.signature.api.pki;

import java.io.InputStream;
import java.security.KeyStore;

import org.nuxeo.ecm.platform.signature.api.exception.CertException;

/**
 * Allows interaction with CA root-related PKI objects:
 * certificates, keys, keystore & certificate files
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public interface RootService {

    public KeyStore getRootKeyStore();
    public String getRootKeystoreFilePath();
    public String getRootKeystorePassword();
    public String getRootKeyAlias();
    public String getRootKeyPassword();
    public String getRootCertificateAlias();

    public boolean isRootSetup();
    public byte[] getRootPublicCertificate() throws CertException;
    public InputStream getRootKeystoreIS() throws CertException;
    
    public void setRootKeyStore(KeyStore rootKeyStore);
    public void setRootKeystoreFilePath(String rootKeystoreFilePath);
    public void setRootKeystorePassword(String rootKeystorePassword);
    public void setRootKeyAlias(String rootKeyAlias);
    public void setRootKeyPassword(String rootKeyPassword);
    public void setRootCertificateAlias(String rootCertificateAlias);
}