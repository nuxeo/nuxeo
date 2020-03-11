/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Wojciech Sulejman
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.signature.core.pki;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Base implementation of the certification service.
 */
public class CertServiceImpl extends DefaultComponent implements CertService {

    protected RootService rootService;

    private static final Log LOG = LogFactory.getLog(CertServiceImpl.class);

    @Override
    public void setRootService(RootService rootService) {
        this.rootService = rootService;
    }

    protected X509Certificate rootCertificate;

    private static final int CERTIFICATE_DURATION_IN_MONTHS = 12;

    private static final String CERT_SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";

    private static final String KEY_ALGORITHM = "RSA";

    private static final int KEY_SIZE = 1024;

    private static final String KEYSTORE_TYPE = "JKS";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public X509Certificate getRootCertificate() throws CertException {
        if (rootCertificate == null) {
            rootCertificate = getCertificate(getRootService().getRootKeyStore(),
                    getRootService().getRootCertificateAlias());
        }
        return rootCertificate;
    }

    protected Date getCertStartDate() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    protected Date getCertEndDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, CERTIFICATE_DURATION_IN_MONTHS);
        return cal.getTime();
    }

    @Override
    public KeyStore initializeUser(UserInfo userInfo, String suppliedPassword) throws CertException {
        char[] password = suppliedPassword.toCharArray();
        KeyStore ks = null;
        String userName = userInfo.getUserFields().get(CNField.UserID);
        AliasWrapper keystoreAlias = new AliasWrapper(userName);
        try {
            ks = java.security.KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(null, password);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.genKeyPair();
            java.security.cert.Certificate[] chain = { getRootCertificate() };
            ks.setKeyEntry(keystoreAlias.getId(AliasType.KEY), keyPair.getPrivate(), password, chain);
            X509Certificate cert = getCertificate(keyPair, userInfo);
            ks.setCertificateEntry(keystoreAlias.getId(AliasType.CERT), cert);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (IOException e) {
            throw new CertException(e);
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        }
        return ks;
    }

    @Override
    public KeyPair getKeyPair(KeyStore ks, String keyAlias, String certAlias, String keyPassword) throws CertException {
        KeyPair keyPair = null;
        try {
            if (!ks.containsAlias(keyAlias)) {
                throw new CertException("Missing keystore key entry for key alias:" + keyAlias);
            }
            if (!ks.containsAlias(certAlias)) {
                throw new CertException("Missing keystore certificate entry for :" + certAlias);
            }
            PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, keyPassword.toCharArray());
            X509Certificate cert = (X509Certificate) ks.getCertificate(certAlias);
            PublicKey publicKey = cert.getPublicKey();
            keyPair = new KeyPair(publicKey, privateKey);
        } catch (UnrecoverableKeyException e) {
            throw new CertException(e);
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        }
        return keyPair;
    }

    @Override
    public X509Certificate getCertificate(KeyStore ks, String certificateAlias) throws CertException {
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

    protected X509Certificate getCertificate(KeyPair keyPair, UserInfo userInfo) throws CertException {
        X509Certificate rootCertificate = getRootCertificate();
        X500Principal issuer = rootCertificate.getIssuerX500Principal();
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        X500Principal principal = userInfo.getX500Principal();
        String email = userInfo.getUserFields().get(CNField.Email);
        try {
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial, getCertStartDate(),
                    getCertEndDate(), principal, keyPair.getPublic());
            builder.addExtension(Extension.authorityKeyIdentifier, false,
                    extUtils.createAuthorityKeyIdentifier(rootCertificate))
                   .addExtension(Extension.subjectKeyIdentifier, false,
                           extUtils.createSubjectKeyIdentifier(keyPair.getPublic()))
                   .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
                   .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature))
                   .addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth))
                   .addExtension(Extension.subjectAlternativeName, false,
                           new GeneralNames(new GeneralName(GeneralName.rfc822Name, email)));
            ContentSigner signer = new JcaContentSignerBuilder(CERT_SIGNATURE_ALGORITHM).setProvider("BC")
                                                                                        .build(keyPair.getPrivate());
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
        } catch (GeneralSecurityException | OperatorException | IOException e) {
            throw new CertException(e);
        }
    }

    @Override
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
            if (String.valueOf(e.getMessage()).contains("password was incorrect")) {
                // "Keystore was tampered with, or password was incorrect"
                // is not very useful to end-users
                throw new CertException("Incorrect password");
            }
            throw new CertException(e);
        }
        return ks;
    }

    @Override
    public String getCertificateEmail(X509Certificate certificate) throws CertException {
        try {
            @SuppressWarnings("unchecked")
            Collection<List<?>> altNames = X509ExtensionUtil.getSubjectAlternativeNames(certificate);
            for (List<?> names : altNames) {
                if (Integer.valueOf(GeneralName.rfc822Name).equals(names.get(0))) {
                    return (String) names.get(1);
                }
            }
            return null;
        } catch (GeneralSecurityException e) {
            throw new CertException(e);
        }
    }

    @Override
    public void storeCertificate(KeyStore keystore, OutputStream os, String keystorePassword) throws CertException {
        try {
            keystore.store(os, keystorePassword.toCharArray());
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (IOException e) {
            throw new CertException(e);
        }
    }

    protected RootService getRootService() throws CertException {
        if (rootService == null) {
            rootService = Framework.getService(RootService.class);
        }
        return rootService;
    }
}
