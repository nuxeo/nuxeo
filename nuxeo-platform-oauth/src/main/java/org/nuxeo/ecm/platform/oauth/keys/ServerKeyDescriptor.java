/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.oauth.keys;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Xmap object used to represent the contribution to {@link OAuthServerKeyManager}.
 * => contribute a simple RSA Key Pair
 * @author tiry
 *
 */
@XObject("serverKeyPair")
public class ServerKeyDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("privateKey")
    protected String externalPrivateKey;

    /**
     * Most folks should not need to change this from the default value of
     * nuxeo. It's unclear that there are many service providers that actually
     * use this value.
     */
    @XNode("privateKeyName")
    protected String externalPrivateKeyName;

    /**
     * This is here just for convenience of keeping everything together. This is
     * the public key (really a certificate) that you need to give to external
     * oauth provider to indicate that you have the private key above. Assuming
     * you generated the key with the lines above, you should be able to simply
     * paste the "certificate" portion of the testkey.pem file into this field.
     * It is not used by nuxeo in any way, but <b>will</b> be needed when you
     * configure an external provider.
     * <p>
     * Note that many providers accept the certificate then run a computation to
     * extract the public key from it. This means that the value displayed when
     * you look at the provider configuration may be different than the one you
     * provided.
     */
    @XNode("publicCertificate")
    protected String externalPublicCertificate;

}
