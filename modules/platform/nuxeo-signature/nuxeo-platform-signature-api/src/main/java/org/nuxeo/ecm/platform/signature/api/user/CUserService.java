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
package org.nuxeo.ecm.platform.signature.api.user;

import java.security.KeyStore;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;

/**
 * High-level user certificate and keystore operations. These services help retrieving certificates, keystores and other
 * information related to specific users.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public interface CUserService {

    /**
     * Generates user certificate and user keys, saves them to a user store, and persists the store in the directory.
     */
    DocumentModel createCertificate(DocumentModel user, String userKeyPassword) throws CertException;

    /**
     * Retrieves a UserInfo object containing information needed for certificate generation.
     */
    UserInfo getUserInfo(DocumentModel userModel) throws CertException;

    /**
     * Returns simplified textual representation of a certificate's contents.
     *
     * @return Simple certificate string.
     */
    String getUserCertInfo(DocumentModel user, String userKeyPassword) throws CertException;

    /**
     * Retrieves user keystore from the directory.
     *
     * @return User KeyStore object
     */
    KeyStore getUserKeystore(String userID, String userKeyPassword) throws CertException;

    /**
     * Retrieves a user certificate from the directory.
     *
     * @return certificate document model
     */
    DocumentModel getCertificate(String userID);

    /**
     * Retrieves the public root certificate.
     *
     * @return certificate document model
     */
    byte[] getRootCertificateData();

    /**
     * Checks if the user is present in the certificate directory.
     */
    boolean hasCertificate(String userID) throws CertException;

    /**
     * Deletes user entry from the certificate directory.
     * <p>
     * This is a high-level operation. The following containers/entries are removed:
     * <ul>
     * <li>a certificate directory entry related to the userID
     * <li>a keystore (which was saved as a field in the directory entry)
     * <li>a private key and a public certificate (which were contained in the keystore)
     * </ul>
     */
    void deleteCertificate(String userID) throws CertException;

}
