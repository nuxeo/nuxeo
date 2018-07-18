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

import java.util.Calendar;

import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;

/**
 * Represents Token data as manipulated in OAuth during the 3 legged authentication. The same interface is used for
 * Request Token and Access Token.
 *
 * @author tiry
 */
public interface OAuthToken {

    enum Type {
        REQUEST, ACCESS
    }

    /**
     * Returns consumer application identifier.
     */
    String getAppId();

    /**
     * Returns consumer call back url (may be used to override what is provided in the {@link OAuthConsumerRegistry}.
     */
    String getCallbackUrl();

    /**
     * Returns Nuxeo Login as determined during the authorize phase.
     */
    String getNuxeoLogin();

    /**
     * Returns OAuth token.
     */
    String getToken();

    /**
     * Returns secret associated to the Token.
     */
    String getTokenSecret();

    /**
     * Gets the Consumer Key.
     */
    String getConsumerKey();

    /**
     * Gets the type of token: REQUEST / ACCESS.
     */
    Type getType();

    /**
     * Gets creation date of the Token.
     */
    Calendar getCreationDate();

    /**
     * Generic getter (not used for now).
     */
    String getValue(String keyName);

    /**
     * Generic setter (not used for now).
     */
    void setValue(String keyName, String value);

    /**
     * Gets the verifier code.
     */
    String getVerifier();

    /**
     * Checks is token is expired.
     */
    boolean isExpired();

    /**
     * Setter for the Login.
     */
    void setNuxeoLogin(String login);

}
