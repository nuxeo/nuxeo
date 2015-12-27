/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.oauth.keys;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Xmap object used to represent the contribution to {@link OAuthServerKeyManager}. => contribute a simple RSA Key Pair.
 *
 * @author tiry
 */
@XObject("serverKeyPair")
public class ServerKeyDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("privateKey")
    protected String externalPrivateKey;

    /**
     * Most folks should not need to change this from the default value of nuxeo. It's unclear that there are many
     * service providers that actually use this value.
     */
    @XNode("privateKeyName")
    protected String externalPrivateKeyName;

    /**
     * This is here just for convenience of keeping everything together. This is the public key (really a certificate)
     * that you need to give to external oauth provider to indicate that you have the private key above. Assuming you
     * generated the key with the lines above, you should be able to simply paste the "certificate" portion of the
     * testkey.pem file into this field. It is not used by nuxeo in any way, but <b>will</b> be needed when you
     * configure an external provider.
     * <p>
     * Note that many providers accept the certificate then run a computation to extract the public key from it. This
     * means that the value displayed when you look at the provider configuration may be different than the one you
     * provided.
     */
    @XNode("publicCertificate")
    protected String externalPublicCertificate;

}
