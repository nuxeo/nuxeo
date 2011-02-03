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
 *
 *  - private/public key pair used by Nuxeo to use Signed request with RSA
 *
 *  - shared key between Nuxeo and Shindig to manage Signed Fetch
 *
 * @author tiry
 *
 */
public interface OAuthServerKeyManager {

    /**
     * Return the Public Key certificate used by Nuxeo server to do RSA Signing
     *
     * @return
     */
    public String getPublicKeyCertificate();

    /**
     * Return the Public Key certificate used by Nuxeo server to do RSA Signing
     * (Removes OpenSSL decorators)
     * @return
     */
    public String getBarePublicCertificate();

    /**
     * Return the Private Key used by Nuxeo server to do RSA Signing
     *
     * @return
     */
    public String getPrivateKey();

    /**
     * Return the Private Key used by Nuxeo server to do RSA Signing
     * (Removes OpenSSL decorators)
     * @return
     */
    public String getBarePrivateKey();

    /**
     * Return key name (not really used)
     *
     * @return
     */
    public String getKeyName();

    /**
     * Return the consumerKey used in Shindig => Nuxeo sign fetch
     *
     * @return
     */
    public String getInternalKey();

    /**
     * Return the consumerSecret (HMAC) used in Shindig => Nuxeo sign fetch
     *
     * @return
     */
    public String getInternalSecret();

    /**
     * Return the {@link NuxeoOAuthConsumer} representing local (embeded) Shindig instance
     *
     * @return
     */
    public NuxeoOAuthConsumer getInternalConsumer();

}