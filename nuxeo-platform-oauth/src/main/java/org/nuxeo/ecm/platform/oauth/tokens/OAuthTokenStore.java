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
 * Service interface for managing {@link OAuthToken} used both :
 *  - in the OAuth the Filter (Server side Tokens) : where Nuxeo is the provider
 *  - in Shindig (Client side Tokens) ; where Nuxeo is the consumer
 *
 * This service provides a center access point for all Token related actions.
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
     * Generates a verification code and attache it to the REQUEST Token.
     */
    OAuthToken addVerifierToRequestToken(String token,Long duration);

    /**
     * Retrieves a REQUEST Token given a Token string (extracted from the
     * Request).
     */
    OAuthToken getRequestToken(String token);

    /**
     * Deletes a REQUEST Token.
     */
    void removeRequestToken(String token);

    // Access token

    /**
     * Exchanges the REQUEST Token witha Real ACCESS Token (persistent)
     * Token/TocketSecret Strings are regerated during the exchange.
     */
    OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken);

    /**
     * Retrieves an ACCESS from the store.
     */
    OAuthToken getAccessToken(String token);

    /**
     * Deletes an ACCESS Token from the storage.
     */
    void removeAccessToken(String token) throws Exception;

    /**
     * Lists ACCESS Token associated to a User.
     */
    List<OAuthToken> listAccessTokenForUser(String login);

    /**
     * Lists ACCESS Token associated to a Consumer application.
     */
    List<OAuthToken> listAccessTokenForConsumer(String consumerKey);

    // Client Token

    /**
     * Stores a Access token generated fro Shindig client.
     */
    void storeClientAccessToken(String consumerKey, String callBack, String token, String tokenSecret, String appId, String owner);

    /**
     * Get a Access token for the Shindig Client.
     */
    NuxeoOAuthToken getClientAccessToken(String appId, String owner) throws Exception;

    /**
     * Deletes a Client side Access Token.
     */
    void removeClientAccessToken(String appId, String owner) throws Exception;

}
