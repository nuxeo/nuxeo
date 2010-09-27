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

package org.nuxeo.ecm.platform.signature.api.pki;

import java.io.File;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;

/**
 *
 * Certificate Authority services that allows processing of certificates
 *
 * The primary functionality of this entity is signing CSRs (Certificate Signing
 * Requests) with the root certificate
 *
 * This entity also handles CRLs (certificate revocation lists)
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public interface CAService {

    /**
     * Retrieves the root certificate
     *
     * @return
     * @throws CertException
     */
    public X509Certificate getRootCertificate() throws CertException;

    /**
     * Signs a submitted Certificate Signing Request
     *
     * @param csr
     * @param certInfo
     * @return
     * @throws CertException
     */
    public X509Certificate createCertificateFromCSR(
            PKCS10CertificationRequest csr, CertInfo certInfo)
            throws CertException;

    /**
     * Retrieves a certificate from file
     *
     * @param certFile
     * @return
     * @throws CertException
     */
    public X509Certificate getCertificate(File certFile) throws CertException;

    /**
     * Sets up a root certificate to be used for CA Services
     *
     * @param rootCertificate
     * @throws CertException
     */
    public void setRootCertificate(X509Certificate rootCertificate)
            throws CertException;

}
