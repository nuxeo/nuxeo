/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.tokens;

import java.util.List;

/**
 * Service interface for managing {@link OAuthToken} used in the Filter This
 * service provides a center access point for all Token related actions.
 *
 * @author tiry
 *
 */
public interface OAuthTokenStore {

    // Request token

    /**
     * Creates a new REQUEST Token (transient)
     */
    OAuthToken createRequestToken(String consumerKey, String callBack);

    /**
     * Generates a verification code and attache it to the REQUEST Token
     *
     * @param token
     * @return
     */
    OAuthToken addVerifierToRequestToken(String token);

    /**
     * Retrieves a REQUEST Token given a Token string (extracted from the
     * Request)
     *
     * @param token
     * @return
     */
    OAuthToken getRequestToken(String token);

    /**
     * Deletes a REQUEST Token
     *
     * @param token
     */
    void removeRequestToken(String token);

    // Access token

    /**
     * Exchanges the REQUEST Token witha Real ACCESS Token (persistent)
     * Token/TocketSecret Strings are regerated during the exchange
     *
     */
    OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken);

    /**
     * Retrieves an ACCESS from the store
     *
     * @param token
     * @return
     */
    OAuthToken getAccessToken(String token);

    /**
     * Deletes an ACCESS Token from the storage
     *
     * @param token
     * @throws Exception
     */
    void removeAccessToken(String token) throws Exception;

    /**
     * Lists ACCESS Token associated to a User
     *
     * @param login
     * @return
     */
    List<OAuthToken> listAccessTokenForUser(String login);

    /**
     * Lists ACCESS Token associated to a Consumer application
     *
     * @param consumerKey
     * @return
     */
    List<OAuthToken> listAccessTokenForConsumer(String consumerKey);

}
