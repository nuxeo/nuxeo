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
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.api.pki;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;

/**
 * This service provides certificate generation and certificate related keystore operations.
 * <p>
 * The interfaces provided by this service are intended to abstract low-level generic certificate operations like PKI
 * key and certificate generation, CSR (Certificate Signing Request) signing with the root certificate, retrieving the
 * certificates from the keystore in a generic way, and also providing CRLs (Certificate Revocation Lists).
 * <p>
 * The bulk of this functionality is provided via the initializeUser(..) method used to generate a fully initialized
 * certificate enclosed in a secured keystore.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public interface CertService {

    /**
     * Retrieves the root certificate.
     */
    X509Certificate getRootCertificate() throws CertException;

    /**
     * Sets up a root service to be used for CA-related services like certificate request signing and certificate
     * revocation.
     */
    void setRootService(RootService rootService) throws CertException;

    /**
     * Retrieves a KeyStore object from a supplied InputStream. Requires a keystore password.
     */
    KeyStore getKeyStore(InputStream keystoreIS, String password) throws CertException;

    /**
     * Retrieves existing private and public key from a KeyStore.
     */
    KeyPair getKeyPair(KeyStore ks, String keyAlias, String certificateAlias, String keyPassword)
            throws CertException;

    /**
     * Retrieves an existing certificate from a keystore using keystore's certificate alias.
     */
    X509Certificate getCertificate(KeyStore keystore, String certificateAlias) throws CertException;

    /**
     * Generates a private key and a public certificate for a user whose X.509 field information was enclosed in a
     * UserInfo parameter. Stores those artifacts in a password protected keystore. This is the principal method for
     * activating a new certificate and signing it with a root certificate.
     *
     * @return KeyStore based on the provided userInfo
     */

    KeyStore initializeUser(UserInfo userInfo, String keyPassword) throws CertException;

    /**
     * Wraps a certificate object into an OutputStream object secured by a keystore password
     */
    void storeCertificate(KeyStore keystore, OutputStream os, String keystorePassword) throws CertException;

    /**
     * Extracts the email address from a certificate
     */
    String getCertificateEmail(X509Certificate certificate) throws CertException;

}
