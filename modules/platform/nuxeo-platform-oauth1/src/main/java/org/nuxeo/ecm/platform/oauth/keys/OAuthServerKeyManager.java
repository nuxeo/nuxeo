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

import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;

/**
 * Service to manage the key and shared secret used by Nuxeo server :
 * <ul>
 * <li>private/public key pair used by Nuxeo to use Signed request with RSA
 * <li>shared key between Nuxeo and Shindig to manage Signed Fetch
 * </ul>
 *
 * @author tiry
 */
public interface OAuthServerKeyManager {

    /**
     * Returns the Public Key certificate used by Nuxeo server to do RSA Signing.
     */
    String getPublicKeyCertificate();

    /**
     * Returns the Public Key certificate used by Nuxeo server to do RSA Signing. (Removes OpenSSL decorators).
     */
    String getBarePublicCertificate();

    /**
     * Returns the Private Key used by Nuxeo server to do RSA Signing.
     */
    String getPrivateKey();

    /**
     * Returns the Private Key used by Nuxeo server to do RSA Signing. (Removes OpenSSL decorators).
     */
    String getBarePrivateKey();

    /**
     * Returns key name (not really used).
     */
    String getKeyName();

    /**
     * Returns the consumerKey used in Shindig => Nuxeo sign fetch.
     */
    String getInternalKey();

    /**
     * Returns the consumerSecret (HMAC) used in Shindig => Nuxeo sign fetch.
     */
    String getInternalSecret();

    /**
     * Returns the {@link NuxeoOAuthConsumer} representing local (embedded) Shindig instance.
     */
    NuxeoOAuthConsumer getInternalConsumer();

}
