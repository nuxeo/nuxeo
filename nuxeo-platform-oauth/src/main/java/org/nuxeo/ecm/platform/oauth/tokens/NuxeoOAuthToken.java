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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Implementation of the {@link OAuthToken} interface. Provides mapping features
 * to DocumentModel so that Token can be stored in a SQL Directory
 *
 * @author tiry
 *
 */
public class NuxeoOAuthToken implements OAuthToken {

    public static final String SCHEMA = "oauthToken";

    protected String appId;

    protected String callbackUrl;

    protected String nuxeoLogin;

    protected String token;

    protected String tokenSecret;

    protected String consumerKey;

    protected Type type;

    protected Calendar creationDate;

    protected String verifier;

    protected long durationInMinutes;

    protected boolean clientToken = false;

    protected String clientId = null;

    public NuxeoOAuthToken(String consumerKey, String callBack) {
        this.appId = consumerKey;
        this.consumerKey = consumerKey;
        this.callbackUrl = callBack;
        this.creationDate = Calendar.getInstance();
    }

    public NuxeoOAuthToken(NuxeoOAuthToken originalToken) {
        this.appId = originalToken.appId;
        this.callbackUrl = originalToken.callbackUrl;
        this.nuxeoLogin = originalToken.nuxeoLogin;
        this.token = originalToken.token;
        this.tokenSecret = originalToken.tokenSecret;
        this.consumerKey = originalToken.consumerKey;
        this.type = originalToken.type;
        this.verifier = originalToken.verifier;
        this.durationInMinutes = originalToken.durationInMinutes;
        this.creationDate = Calendar.getInstance();
    }

    public NuxeoOAuthToken(DocumentModel entry) throws ClientException {
        this.appId = (String) entry.getProperty(SCHEMA, "appId");
        this.callbackUrl = (String) entry.getProperty(SCHEMA, "callbackUrl");
        this.nuxeoLogin = (String) entry.getProperty(SCHEMA, "nuxeoLogin");
        this.token = (String) entry.getProperty(SCHEMA, "token");
        this.tokenSecret = (String) entry.getProperty(SCHEMA, "tokenSecret");
        this.consumerKey = (String) entry.getProperty(SCHEMA, "consumerKey");
        ;
        this.type = OAuthToken.Type.ACCESS;
        this.verifier = (String) entry.getProperty(SCHEMA, "verifier");
        this.durationInMinutes = (Long) entry.getProperty(SCHEMA,
                "durationInMinutes");
        this.creationDate = (Calendar) entry.getProperty(SCHEMA, "creationDate");

        Long clientTokenL = (Long) entry.getProperty(SCHEMA, "clientToken");
        if (clientTokenL!=null && clientTokenL.equals(1)) {
            this.clientToken = true;
        }
        this.clientId = (String) entry.getProperty(SCHEMA, "clientId");
    }

    public void updateEntry(DocumentModel entry) throws ClientException {
        entry.setProperty(SCHEMA, "appId", this.appId);
        entry.setProperty(SCHEMA, "callbackUrl", this.callbackUrl);
        entry.setProperty(SCHEMA, "nuxeoLogin", this.nuxeoLogin);
        entry.setProperty(SCHEMA, "tokenSecret", this.tokenSecret);
        entry.setProperty(SCHEMA, "consumerKey", this.consumerKey);
        entry.setProperty(SCHEMA, "verifier", this.verifier);
        entry.setProperty(SCHEMA, "durationInMinutes", this.durationInMinutes);
        entry.setProperty(SCHEMA, "creationDate", this.creationDate);

        entry.setProperty(SCHEMA, "clientId", this.clientId);
        if (this.clientToken) {
            entry.setProperty(SCHEMA, "clientToken", 1);
        } else {
            entry.setProperty(SCHEMA, "clientToken", 0);
        }
    }

    public String getAppId() {
        return appId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getNuxeoLogin() {
        return nuxeoLogin;
    }

    public String getToken() {
        return token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public Type getType() {
        return type;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    @Override
    public String getValue(String keyName) {

        return null;
    }

    @Override
    public void setValue(String keyName, String value) {

    }

    public String getVerifier() {
        return verifier;
    }

    public boolean isExpired() {
        // XXX
        return false;
    }

    public void setNuxeoLogin(String login) {
        nuxeoLogin = login;
    }

    public boolean isClientToken() {
        return clientToken;
    }

    public String getClientId() {
        return clientId;
    }


}
