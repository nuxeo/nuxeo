/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.key;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.HashMap;
import java.util.Map;

@XObject("configuration")
public class KeyDescriptor {

    @XNode("keystoreFilePath")
    protected String keystoreFilePath;

    @XNode("keystorePassword")
    protected String keystorePassword;

    @XNode("signingKey")
    protected String signingKey;

    @XNode("encryptionKey")
    protected String encryptionKey;

    @XNode("tlsKey")
    protected String tlsKey;

    @XNodeMap(value = "passwords/password", key = "@key", type = HashMap.class,
            componentType = String.class)
    protected Map<String, String> passwords;

    public String getKeystoreFilePath() {
        return keystoreFilePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public Map<String, String> getPasswords() {
        return passwords;
    }

    public String getSigningKey() {
        return signingKey;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public String getTlsKey() {
        return tlsKey;
    }
}
