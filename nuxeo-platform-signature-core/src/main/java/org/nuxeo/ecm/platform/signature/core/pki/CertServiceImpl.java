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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.api.Framework;

import sun.security.x509.CertificateIssuerName;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class CertServiceImpl implements CertService {

    protected CAService cAService;

    protected KeyService keyService;

    // TODO move to descriptor
    private static final String CERT_SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";

    private boolean storeInitialized = true;

    private File keyStoreFile = new File("/home/ws/Desktop/keystore.ks");

    private String caAlias = "caAlias";

    private char[] cAPassword = new char[] { 'a', 'b', 'c' };

    String certToSignAlias = "certToSignAlias";

    char[] password = new char[] { 'a', 'b', 'c' };

    char[] certPassword = new char[] { 'a', 'b', 'c' };

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public CertificationRequest generateCSR(UserInfo userInfo)
            throws CertException {

        CertificationRequest csr;

        // TODO extract email using the principal
        GeneralNames subjectAltName = new GeneralNames(new GeneralName(
                GeneralName.rfc822Name, "ws@nuxeo.com"));

        Vector<DERObjectIdentifier> objectIdentifiers = new Vector<DERObjectIdentifier>();
        Vector<X509Extension> extensionValues = new Vector<X509Extension>();

        objectIdentifiers.add(X509Extensions.SubjectAlternativeName);
        extensionValues.add(new X509Extension(false, new DEROctetString(
                subjectAltName)));

        X509Extensions extensions = new X509Extensions(objectIdentifiers,
                extensionValues);

        Attribute attribute = new Attribute(
                PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, new DERSet(
                        extensions));

        try {
            KeyPair keyPair = getKeyService().getKeys(userInfo);
            csr = new PKCS10CertificationRequest(CERT_SIGNATURE_ALGORITHM,
                    userInfo.getX500Principal(), keyPair.getPublic(),
                    new DERSet(attribute), keyPair.getPrivate());
        } catch (InvalidKeyException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (NoSuchProviderException e) {
            throw new CertException(e);
        } catch (java.security.SignatureException e) {
            throw new CertException(e);
        } catch (Exception e) {
            throw new CertException(e);
        }
        return csr;
    }

    @Override
    public X509Certificate getCertificate(UserInfo userInfo)
            throws CertException {
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) generateCSR(userInfo);
        X509Certificate certificate = getCAService().createCertificateFromCSR(
                csr, userInfo);
        return certificate;
    }

    public X509Certificate getCertificate(File certFile) throws CertException {
        return getCAService().getCertificate(certFile);
    }

    protected KeyService getKeyService() throws CertException {
        if (keyService == null) {
            try {
                keyService = Framework.getService(KeyService.class);
            } catch (Exception e) {
                throw new CertException(e);
            }
        }
        return keyService;
    }

    protected CAService getCAService() throws CertException {
        if (cAService == null) {
            try {
                cAService = Framework.getService(CAService.class);
            } catch (Exception e) {
                throw new CertException(e);
            }
        }
        return cAService;
    }

    @Override
    public void storeCertificate(Certificate cert) throws CertException {

        try {

        } catch (Exception e) {
            throw new CertException("Certificate storage problem:" + e);
        }
    }

    /**
     * User userId as store alias
     *
     * @param userInfo
     */
    public PrivateKey getKey(UserInfo userInfo) throws Exception {
        KeyStore keyStore = java.security.KeyStore.getInstance("JKS");
        FileInputStream input = new FileInputStream(keyStoreFile);
        keyStore.load(input, cAPassword);
        java.security.cert.Certificate cACert = keyStore.getCertificate(caAlias);
        byte[] encoded = cACert.getEncoded();
        X509CertImpl caCertImpl = new X509CertImpl(encoded);
        X509CertInfo caCertInfo = (X509CertInfo) caCertImpl.get(X509CertImpl.NAME
                + "." + X509CertImpl.INFO);
        X500Name issuer = (X500Name) caCertInfo.get(X509CertInfo.SUBJECT + "."
                + CertificateIssuerName.DN_NAME);
        java.security.cert.Certificate cert = keyStore.getCertificate(caAlias);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(certToSignAlias,
                cAPassword);
        return privateKey;
    }

    private void initKeyStore(KeyStore keyStore, String caAlias)
            throws Exception {

        OutputStream output = new FileOutputStream(keyStoreFile);
        keyStore.load(null, password);
        KeyPair caKeys = keyService.getKeys(getUserInfo());
        PrivateKey caPrivateKey = caKeys.getPrivate();
        Certificate cACert = cAService.getRootCertificate();
        keyStore.setCertificateEntry(caAlias, cACert);

        keyStore.store(output, password);
        output.close();
    }

    //TODO move to descriptor or external file
    public UserInfo getUserInfo() throws Exception {
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.C, "US");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.OU, "CA");
        userFields.put(CNField.CN, "PDFCA");
        userFields.put(CNField.Email, "pdfca@nuxeo.com");
        userFields.put(CNField.UserID, "Administrator");
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }

}