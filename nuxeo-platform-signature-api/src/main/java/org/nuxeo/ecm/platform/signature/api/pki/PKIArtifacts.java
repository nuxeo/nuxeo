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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.api.pki;

import java.security.KeyPair;
import java.security.cert.Certificate;

/**
 * DTO for pki artifacts
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class PKIArtifacts {
    KeyPair keyPair;
    Certificate cert;

    public PKIArtifacts(KeyPair keyPair,Certificate certificate){
        setKeyPair(keyPair);
        setCert(certificate);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }
    public Certificate getCert() {
        return cert;
    }
    public void setCert(Certificate cert) {
        this.cert = cert;
    }
}
