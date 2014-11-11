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
 *
 */
public interface OAuthToken {

    public static enum Type {
        REQUEST, ACCESS
    }

    /**
     * Returns consumer application identifier
     *
     * @return
     */
    String getAppId();

    /**
     * Returns consumer call back url (may be used to override what is provided
     * in the {@link OAuthConsumerRegistry}.
     *
     * @return
     */
    String getCallbackUrl();

    /**
     * Returns Nuxeo Login as determined during the authorize phase
     *
     * @return
     */
    String getNuxeoLogin();

    /**
     * Returns OAuth token
     *
     * @return
     */
    String getToken();

    /**
     * Returns secret associated to the Token
     *
     * @return
     */
    String getTokenSecret();

    /**
     * Get the Consumer Key
     *
     * @return
     */
    String getConsumerKey();

    /**
     * Get the type of token : REQUEST / ACCESS
     *
     * @return
     */
    Type getType();

    /**
     * Get creation date of the Token
     *
     * @return
     */
    Calendar getCreationDate();

    /**
     * Generic getter (not used for now)
     *
     * @param keyName
     * @return
     */
    String getValue(String keyName);

    /**
     * Generic setter (not used for now)
     *
     * @param keyName
     * @param value
     */
    void setValue(String keyName, String value);

    /**
     * Get the verifier code
     *
     * @return
     */
    String getVerifier();

    /**
     * Check is token is expired
     *
     * @return
     */
    boolean isExpired();

    /**
     *
     * Setter for the Login
     *
     * @param login
     */
    void setNuxeoLogin(String login);
}
