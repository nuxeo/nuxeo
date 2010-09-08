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

import java.security.KeyPair;
import java.security.cert.Certificate;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public interface CertService {
    /**
     * Creates a new certificate
     *
     * @param privateKey
     * @param userInfo
     */
    public Certificate createCertificate(KeyPair keyPair, CertInfo userInfo)
            throws Exception;

    /**
     * Retrieves an existing certificate from storage
     *
     * @return
     */
    Certificate retrieveCertificate(String userId);

    /**
     * Stores a certificate via a storage mechanism
     */
    void storeCertificate(Certificate cert, StoreService store);

}
