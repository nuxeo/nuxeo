/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.signature.core.pki;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class RootServiceImpl extends DefaultComponent implements RootService {

    protected static final String XP = "rootconfig";

    private static final String KEYSTORE_TYPE = "JKS";

    private KeyStore rootKeyStore;

    private String rootKeystoreFilePath;

    private String rootKeystorePassword;

    private String rootCertificateAlias;

    private String rootKeyAlias;

    private String rootKeyPassword;

    @Override
    public void start(ComponentContext context) {
        this.<RootDescriptor> getRegistryContribution(XP).ifPresent(desc -> {
            if (desc.getRootKeystoreFilePath() == null) {
                throw new CertException("Keystore path is missing");
            } else {
                rootKeystoreFilePath = desc.getRootKeystoreFilePath();
            }
            if (desc.getRootCertificateAlias() == null) {
                throw new CertException("You have to provide root certificate alias");
            } else {
                rootCertificateAlias = desc.getRootCertificateAlias();
            }
            if (desc.getRootKeystorePassword() == null) {
                throw new CertException("You have to provide root keystore password");
            } else {
                rootKeystorePassword = desc.getRootKeystorePassword();
            }
            if (desc.getRootKeyAlias() == null) {
                throw new CertException("You have to provide root key alias");
            } else {
                rootKeyAlias = desc.getRootKeyAlias();
            }
            if (desc.getRootKeyPassword() == null) {
                throw new CertException("You have to provide root key password");
            } else {
                rootKeyPassword = desc.getRootKeyPassword();
            }
            rootKeyStore = getKeyStore(getRootKeystoreIS(rootKeystoreFilePath), rootKeystorePassword);
        });
    }

    protected KeyStore getKeyStore(InputStream keystoreIS, String password) throws CertException {
        KeyStore ks;
        try {
            ks = java.security.KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(keystoreIS, password.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new CertException(e);
        }
        return ks;
    }

    protected InputStream getRootKeystoreIS(String rootKeystoreFilePath) throws CertException {
        InputStream keystoreIS = null;
        File rootKeystoreFile = null;
        try {
            rootKeystoreFile = new File(rootKeystoreFilePath);
            if (rootKeystoreFile.exists()) {
                keystoreIS = new FileInputStream(rootKeystoreFile);
            } else {
                // try a temporary resource keystore instead of a configurable one
                keystoreIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(rootKeystoreFilePath);
            }
        } catch (IOException e) {
            throw new CertException("Certificate not found at" + rootKeystoreFile.getAbsolutePath());
        }
        return keystoreIS;
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        rootKeyStore = null;
        rootCertificateAlias = null;
        rootKeyAlias = null;
        rootKeyPassword = null;
    }

    @Override
    public KeyStore getRootKeyStore() {
        return rootKeyStore;
    }

    @Override
    public String getRootKeystoreFilePath() {
        return rootKeystoreFilePath;
    }

    @Override
    public String getRootKeystorePassword() {
        return rootKeystorePassword;
    }

    @Override
    public String getRootCertificateAlias() {
        return rootCertificateAlias;
    }

    @Override
    public String getRootKeyAlias() {
        return rootKeyAlias;
    }

    @Override
    public String getRootKeyPassword() {
        return rootKeyPassword;
    }

    /**
     * Public certificate for the CA root. Encoded as an ASN.1 DER ("anybody there?") formatted byte array.
     */
    @Override
    public byte[] getRootPublicCertificate() throws CertException {
        X509Certificate certificate;
        try {
            certificate = getCertificate(getRootKeyStore(), getRootCertificateAlias());
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new CertException(e);
        }
    }

    // custom certificate type for the root certificate
    protected X509Certificate getCertificate(KeyStore ks, String certificateAlias) throws CertException {
        X509Certificate certificate = null;
        try {
            if (ks == null) {
                throw new CertException("Keystore missing for " + certificateAlias);
            }
            if (ks.containsAlias(certificateAlias)) {
                certificate = (X509Certificate) ks.getCertificate(certificateAlias);
            } else {
                throw new CertException("Certificate not found");
            }
        } catch (KeyStoreException e) {
            throw new CertException(e);
        }
        return certificate;
    }

}
