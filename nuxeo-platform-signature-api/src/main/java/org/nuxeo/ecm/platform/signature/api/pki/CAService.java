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
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;

/**
 *
 * Certificate services allowing PKI key and certificate generation, signing CSRs (Certificate Signing
 * Requests) with the root certificate and CRLs (certificate revocation lists)
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
     * Retrieves a certificate from a File
     *
     * @param certFile
     * @return
     * @throws CertException
     */
    public X509Certificate getCertificate(File certFile) throws CertException;

    /**
     * Sets up a root certificate to be used for CA type of services
     * like certificate signing and revocation
     *
     * @param rootCertificate
     * @throws CertException
     */
    public void setRootCertificate(X509Certificate rootCertificate)
            throws CertException;


    /**
     * Retrieves a KeyStore object from a supplied InputStream
     *
     * @param userId
     * @return
     */
    public KeyStore getKeyStore(InputStream keystoreIS, UserInfo userInfo,
            String password) throws CertException;

    /**
     * Retrieves existing private key from a KeyStore
     *
     * @param userId
     * @return
     */
    public KeyPair getKeyPair(KeyStore keystore, UserInfo userInfo,
            String password) throws CertException;

    /**
     * Retrieves an existing certificate from a keystore.
     *
     * @param userId
     * @return
     */
    public X509Certificate getCertificate(KeyStore keystore,
            UserInfo userInfo) throws CertException;

    /**
     * Generates a private key and a public certificate for a user whose X.509 field information
     * is provided as a UserInfo parameter. Stores those artifacts in a password protected keystore.
     *
     * @param userId
     * @return
     */

    public KeyStore initializeUser(UserInfo userInfo, String suppliedPassword)
            throws CertException;

}
