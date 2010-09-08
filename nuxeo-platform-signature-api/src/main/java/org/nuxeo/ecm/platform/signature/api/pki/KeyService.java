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
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public interface KeyService {

    /**
     * Retrieves existing keys
     *
     * @param userId
     * @return
     */
    public KeyPair getKeys(CertInfo certInfo) throws Exception;

    /**
     * Creates a new key-pair
     * @param certInfo
     * @return
     * @throws NoSuchAlgorithmException
     */
    public KeyPair createKeys(CertInfo certInfo)
            throws NoSuchAlgorithmException;


    /**
     * Stores a key-pair in the store
     */
    void storeKeys(StoreService store);

}
