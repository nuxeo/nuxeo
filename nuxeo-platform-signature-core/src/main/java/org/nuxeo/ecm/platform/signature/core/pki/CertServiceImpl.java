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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Vector;

import javax.security.auth.x500.X500Principal;

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
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.pki.StoreService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class CertServiceImpl implements CertService {

    protected CAService cAService;

    protected KeyService keyService;

    private static int DEFAULT_KEY_CHAIN_SIZE = 2;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public int getCertChainSize() {
        return DEFAULT_KEY_CHAIN_SIZE;
    }

    public CertificationRequest generateCSR(CertInfo certInfo)
            throws CertException {

        CertificationRequest csr;

        // create a SubjectAlternativeName extension value
        GeneralNames subjectAltName = new GeneralNames(new GeneralName(
                GeneralName.rfc822Name, "ws@nuxeo.com"));

        Vector<DERObjectIdentifier> objectIdentifiers = new Vector<DERObjectIdentifier>();
        Vector<X509Extension> extensionValues = new Vector<X509Extension>();

        objectIdentifiers.add(X509Extensions.SubjectAlternativeName);
        extensionValues.add(new X509Extension(false, new DEROctetString(subjectAltName)));

        X509Extensions extensions = new X509Extensions(objectIdentifiers, extensionValues);

        Attribute attribute = new Attribute(
                PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, new DERSet(
                        extensions));

        try {
            csr = new PKCS10CertificationRequest(
                    certInfo.getCertSignatureAlgorithm(), new X500Principal(
                            certInfo.getUserDN()), getKeyService().getKeys(
                            certInfo).getPublic(), new DERSet(attribute),
                    getKeyService().getKeys(certInfo).getPrivate());
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
    public X509Certificate getCertificate(CertInfo certInfo)
            throws CertException {
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) generateCSR(certInfo);
        X509Certificate certificate = getCAService().createCertificateFromCSR(
                csr, certInfo);
        return certificate;
    }

    public X509Certificate getCertificate(File certFile)
            throws CertException {
        return getCAService().getCertificate(certFile);
    }

    @Override
    public void storeCertificate(Certificate cert, StoreService store) {
        // TODO implementation
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
}