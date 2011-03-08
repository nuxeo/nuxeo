/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.api.user;

import java.security.KeyStore;

/**
 * Allows interaction with CA root-related PKI objects:
 * certificates, keys, keystores, keystore & certificate files
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class RootService {

    private KeyStore rootKeyStore;

    private String rootKeystoreFilePath;

    private String rootKeystorePassword;

    private String rootCertificateAlias;

    private String rootKeyAlias;

    private String rootKeyPassword;

    public KeyStore getRootKeyStore() {
        return rootKeyStore;
    }

    public void setRootKeyStore(KeyStore rootKeyStore) {
        this.rootKeyStore = rootKeyStore;
    }

    public String getRootKeystoreFilePath() {
        return rootKeystoreFilePath;
    }

    public void setRootKeystoreFilePath(String rootKeystoreFilePath) {
        this.rootKeystoreFilePath = rootKeystoreFilePath;
    }

    public String getRootKeystorePassword() {
        return rootKeystorePassword;
    }

    public void setRootKeystorePassword(String rootKeystorePassword) {
        this.rootKeystorePassword = rootKeystorePassword;
    }

    public String getRootCertificateAlias() {
        return rootCertificateAlias;
    }

    public void setRootCertificateAlias(String rootCertificateAlias) {
        this.rootCertificateAlias = rootCertificateAlias;
    }

    public String getRootKeyAlias() {
        return rootKeyAlias;
    }

    public void setRootKeyAlias(String rootKeyAlias) {
        this.rootKeyAlias = rootKeyAlias;
    }

    public String getRootKeyPassword() {
        return rootKeyPassword;
    }

    public void setRootKeyPassword(String rootKeyPassword) {
        this.rootKeyPassword = rootKeyPassword;
    }

    public boolean isRootSetup() {
        boolean rootIsSetup = false;
        if (rootKeyStore != null && rootKeystorePassword != null
                && rootCertificateAlias != null && rootKeyAlias != null
                && rootKeyPassword != null) {
            rootIsSetup = true;
        }
        return rootIsSetup;
    }

}
