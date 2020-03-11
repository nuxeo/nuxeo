/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

    @XNodeMap(value = "passwords/password", key = "@key", type = HashMap.class, componentType = String.class)
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
