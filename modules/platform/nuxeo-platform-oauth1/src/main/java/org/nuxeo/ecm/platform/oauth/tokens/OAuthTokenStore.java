/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.tokens;

import java.util.List;


/**
 * Service interface for managing {@link OAuthToken} used both : - in the OAuth the Filter (Server side Tokens) : where
 * Nuxeo is the provider - in Shindig (Client side Tokens) ; where Nuxeo is the consumer This service provides a center
 * access point for all Token related actions.
 *
 * @author tiry
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
    OAuthToken addVerifierToRequestToken(String token, Long duration);

    /**
     * Retrieves a REQUEST Token given a Token string (extracted from the Request).
     */
    OAuthToken getRequestToken(String token);

    /**
     * Deletes a REQUEST Token.
     */
    void removeRequestToken(String token);

    // Access token

    /**
     * Exchanges the REQUEST Token witha Real ACCESS Token (persistent) Token/TocketSecret Strings are regerated during
     * the exchange.
     */
    OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken);

    /**
     * Retrieves an ACCESS from the store.
     */
    OAuthToken getAccessToken(String token);

    /**
     * Deletes an ACCESS Token from the storage.
     */
    void removeAccessToken(String token);

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
    void storeClientAccessToken(String consumerKey, String callBack, String token, String tokenSecret, String appId,
            String owner);

    /**
     * Get a Access token for the Shindig Client.
     */
    NuxeoOAuthToken getClientAccessToken(String appId, String owner);

    /**
     * Deletes a Client side Access Token.
     */
    void removeClientAccessToken(String appId, String owner);

}
