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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.pki.StoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class KeyServiceImpl extends DefaultComponent implements KeyService {

    String dirName = "";

    String schemaName = "";

    String fieldName = "";

    private KeyPair createKeys(CertInfo certInfo) throws CertException {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(certInfo.getKeyAlgorithm());
            keyGen.initialize(certInfo.getNumBits());
            keyPair = keyGen.genKeyPair();
        } catch (NoSuchAlgorithmException nsae) {
            throw new CertException(nsae);
        }
        return keyPair;
    }

    @Override
    public KeyPair getKeys(CertInfo certInfo) throws CertException {
        KeyPair value = null;
        boolean keysExist = false;
        try {
            if (keysExist) {
                DirectoryService dm = Framework.getService(DirectoryService.class);
                Session session = dm.open(dirName);
                try {
                    DocumentModel entry = session.getEntry(certInfo.getUserID());
                    value = (KeyPair) entry.getProperty(schemaName, fieldName);
                } finally {
                    session.close();
                }
            } else {
                value = createKeys(certInfo);
            }
        } catch (DirectoryException e) {
            throw new CertException(e);
        } catch (ClientException e) {
            throw new CertException(e);
        } catch (Exception e) {
            throw new CertException(e);
        }
        return value;
    }

    public void storeKeys(StoreService store) {

    }
}