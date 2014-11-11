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

import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;

/**
 * Service to manage the key and shared secret used by Nuxeo server :
 * <ul>
 * <li>private/public key pair used by Nuxeo to use Signed request with RSA
 *
 * <li> shared key between Nuxeo and Shindig to manage Signed Fetch
 * </ul>
 * @author tiry
 *
 */
public interface OAuthServerKeyManager {

    /**
     * Returns the Public Key certificate used by Nuxeo server to do RSA Signing.
     */
    String getPublicKeyCertificate();

    /**
     * Returns the Public Key certificate used by Nuxeo server to do RSA Signing.
     * (Removes OpenSSL decorators).
     */
    String getBarePublicCertificate();

    /**
     * Returns the Private Key used by Nuxeo server to do RSA Signing.
     */
    String getPrivateKey();

    /**
     * Returns the Private Key used by Nuxeo server to do RSA Signing.
     * (Removes OpenSSL decorators).
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