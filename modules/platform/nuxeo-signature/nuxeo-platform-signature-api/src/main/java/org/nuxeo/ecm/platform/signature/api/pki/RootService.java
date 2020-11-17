/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.api.pki;

import java.io.InputStream;
import java.security.KeyStore;

import org.nuxeo.ecm.platform.signature.api.exception.CertException;

/**
 * Allows interaction with CA root-related PKI objects: certificates, keys, keystore &amp; certificate files
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public interface RootService {

    KeyStore getRootKeyStore();

    String getRootKeystoreFilePath();

    String getRootKeystorePassword();

    String getRootKeyAlias();

    String getRootKeyPassword();

    String getRootCertificateAlias();

    boolean isRootSetup();

    byte[] getRootPublicCertificate() throws CertException;

    InputStream getRootKeystoreIS() throws CertException;

    void setRootKeyStore(KeyStore rootKeyStore);

    void setRootKeystoreFilePath(String rootKeystoreFilePath);

    void setRootKeystorePassword(String rootKeystorePassword);

    void setRootKeyAlias(String rootKeyAlias);

    void setRootKeyPassword(String rootKeyPassword);

    void setRootCertificateAlias(String rootCertificateAlias);
}
