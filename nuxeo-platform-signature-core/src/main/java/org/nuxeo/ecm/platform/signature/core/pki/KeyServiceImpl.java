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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class KeyServiceImpl extends DefaultComponent implements KeyService {

    //TODO move to descriptor
    private static final String KEY_ALGORITHM="RSA";
    private static final int KEY_SIZE=1024;

    private KeyPair createKeys(UserInfo userInfo) throws CertException {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            keyPair = keyGen.genKeyPair();
        } catch (NoSuchAlgorithmException nsae) {
            throw new CertException(nsae);
        }
        return keyPair;
    }

    @Override
    public KeyPair getKeys(UserInfo userInfo) throws CertException {
        KeyPair keyPair = null;
        boolean keysExist = false;
        try {
            if (keysExist) {
                DirectoryService dm = Framework.getService(DirectoryService.class);
                Session session = dm.open("keys");
                try {

                } finally {
                    session.close();
                }
            } else {
                keyPair = createKeys(userInfo);
            }
        } catch (DirectoryException e) {
            throw new CertException(e);
        } catch (ClientException e) {
            throw new CertException(e);
        } catch (Exception e) {
            throw new CertException(e);
        }
        return keyPair;
    }

    public void storeKeys() {

    }
}