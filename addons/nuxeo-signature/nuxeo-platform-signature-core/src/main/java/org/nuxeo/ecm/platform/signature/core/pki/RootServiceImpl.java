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
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class RootServiceImpl extends DefaultComponent implements RootService {

    private KeyStore rootKeyStore;

    private String rootKeystoreFilePath;

    private String rootKeystorePassword;

    private String rootCertificateAlias;

    private String rootKeyAlias;

    private String rootKeyPassword;

    protected List<RootDescriptor> config;

    private static final String KEYSTORE_TYPE = "JKS";

    @Override
    public void activate(ComponentContext context) {
        config = new ArrayList<>();
    }

    @Override
    public KeyStore getRootKeyStore() {
        return rootKeyStore;
    }

    @Override
    public void setRootKeyStore(KeyStore rootKeyStore) {
        this.rootKeyStore = rootKeyStore;
    }

    @Override
    public String getRootKeystoreFilePath() {
        return rootKeystoreFilePath;
    }

    @Override
    public void setRootKeystoreFilePath(String rootKeystoreFilePath) {
        this.rootKeystoreFilePath = rootKeystoreFilePath;
    }

    @Override
    public String getRootKeystorePassword() {
        return rootKeystorePassword;
    }

    @Override
    public void setRootKeystorePassword(String rootKeystorePassword) {
        this.rootKeystorePassword = rootKeystorePassword;
    }

    @Override
    public String getRootCertificateAlias() {
        return rootCertificateAlias;
    }

    @Override
    public void setRootCertificateAlias(String rootCertificateAlias) {
        this.rootCertificateAlias = rootCertificateAlias;
    }

    @Override
    public String getRootKeyAlias() {
        return rootKeyAlias;
    }

    @Override
    public void setRootKeyAlias(String rootKeyAlias) {
        this.rootKeyAlias = rootKeyAlias;
    }

    @Override
    public String getRootKeyPassword() {
        return rootKeyPassword;
    }

    @Override
    public void setRootKeyPassword(String rootKeyPassword) {
        this.rootKeyPassword = rootKeyPassword;
    }

    @Override
    public boolean isRootSetup() {
        boolean rootIsSetup = false;
        if (rootKeyStore != null && rootKeystorePassword != null && rootCertificateAlias != null
                && rootKeyAlias != null && rootKeyPassword != null) {
            rootIsSetup = true;
        }
        return rootIsSetup;
    }

    protected void initializeRoot() throws CertException {
        for (RootDescriptor certDescriptor : config) {
            if (certDescriptor.getRootKeystoreFilePath() != null) {
                setRootKeystoreFilePath(certDescriptor.getRootKeystoreFilePath());
            } else if (getRootKeyStore() == null) {
                throw new CertException("Keystore path is missing");
            }
            if (certDescriptor.getRootCertificateAlias() != null) {
                setRootCertificateAlias(certDescriptor.getRootCertificateAlias());
            } else {
                throw new CertException("You have to provide root certificate alias");
            }
            if (certDescriptor.getRootKeystorePassword() != null) {
                setRootKeystorePassword(certDescriptor.getRootKeystorePassword());
            } else {
                throw new CertException("You have to provide root keystore password");
            }
            if (certDescriptor.getRootKeyAlias() != null) {
                setRootKeyAlias(certDescriptor.getRootKeyAlias());
            } else {
                throw new CertException("You have to provide root key alias");
            }
            if (certDescriptor.getRootKeyPassword() != null) {
                setRootKeyPassword(certDescriptor.getRootKeyPassword());
            } else {
                throw new CertException("You have to provide root key password");
            }
        }
        KeyStore keystore = getKeyStore(getRootKeystoreIS(), getRootKeystorePassword());
        setRootKeyStore(keystore);
    }

    public KeyStore getKeyStore(InputStream keystoreIS, String password) throws CertException {
        KeyStore ks;
        try {
            ks = java.security.KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(keystoreIS, password.toCharArray());
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (IOException e) {
            throw new CertException(e);
        }
        return ks;
    }

    @Override
    public InputStream getRootKeystoreIS() throws CertException {
        InputStream keystoreIS = null;
        File rootKeystoreFile = null;
        try {
            rootKeystoreFile = new File(getRootKeystoreFilePath());
            if (rootKeystoreFile.exists()) {
                keystoreIS = new FileInputStream(rootKeystoreFile);
            } else {// try a temporary resource keystore instead of a
                // configurable one
                keystoreIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        getRootKeystoreFilePath());
            }
        } catch (IOException e) {
            // try local path
            throw new CertException("Certificate not found at" + rootKeystoreFile.getAbsolutePath());
        }
        return keystoreIS;
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

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor)
            throws CertException {
        config.add((RootDescriptor) contribution);
        initializeRoot();
        if (!isRootSetup()) {
            throw new CertException("Root keystore was not set up correctly");
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        config.remove(contribution);
    }

}
