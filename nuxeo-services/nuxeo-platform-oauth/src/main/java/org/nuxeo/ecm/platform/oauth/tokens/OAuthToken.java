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

import java.util.Calendar;

import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;

/**
 * Represents Token data as manipulated in OAuth during the 3 legged
 * authentication. The same interface is used for Request Token and Access
 * Token.
 *
 * @author tiry
 */
public interface OAuthToken {

    public static enum Type {
        REQUEST, ACCESS
    }

    /**
     * Returns consumer application identifier.
     */
    String getAppId();

    /**
     * Returns consumer call back url (may be used to override what is provided
     * in the {@link OAuthConsumerRegistry}.
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
