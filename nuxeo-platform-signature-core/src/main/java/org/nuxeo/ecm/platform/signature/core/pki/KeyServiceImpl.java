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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.pki.StoreService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class KeyServiceImpl implements KeyService {

    public KeyPair createKeys(CertInfo certInfo)
            throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(certInfo.getKeyAlgorithm());
        keyGen.initialize(certInfo.getNumBits());
        KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }

    /*
     * (non-Javadoc)
     *
     * @see store.StoreService#retrieveKey(java.lang.String)
     */
    @Override
    public KeyPair getKeys(CertInfo certInfo) throws Exception {
        // TODO implement
        KeyPair value = null;
        boolean keysExist = false;
        if (keysExist) {
            DirectoryService dm = Framework.getService(DirectoryService.class);
            Session session = dm.open(KeyDirConfigDescriptor.getDirectoryName());
            try {
                DocumentModel entry = session.getEntry(certInfo.getUserID());
                value = (KeyPair) entry.getProperty(
                        KeyDirConfigDescriptor.getSchemaName(),
                        KeyDirConfigDescriptor.getFieldName());
            } finally {
                session.close();
            }
        } else {
            value = createKeys(certInfo);
        }
        return value;
    }

    public void storeKeys(StoreService store) {

    }

}