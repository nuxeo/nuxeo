/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.oauth2.tokens;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.google.api.client.auth.oauth2.Credential;

public class NuxeoOAuth2Token {

    public static final String SCHEMA = "oauth2Token";

    protected Long id;

    protected String serviceName;

    protected String nuxeoLogin;

    protected String accessToken;

    protected String clientId;

    protected Calendar creationDate;

    private String refreshToken;

    private Long expirationTimeMilliseconds;

    public NuxeoOAuth2Token(long expirationTimeMilliseconds, String clientId) {
        this("", "", expirationTimeMilliseconds);
        this.clientId = clientId;
        refresh();
    }

    public NuxeoOAuth2Token(String accessToken, String refreshToken,
            Long expirationTimeMilliseconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTimeMilliseconds = expirationTimeMilliseconds;
        this.creationDate = Calendar.getInstance();
    }

    public NuxeoOAuth2Token(Credential credential) {
        this(credential.getAccessToken(), credential.getRefreshToken(),
                credential.getExpirationTimeMilliseconds());
    }

    public NuxeoOAuth2Token(DocumentModel entry) throws ClientException {
        this.id = (Long) entry.getProperty(SCHEMA, "id");
        this.accessToken = (String) entry.getProperty(SCHEMA, "accessToken");
        this.refreshToken = (String) entry.getProperty(SCHEMA, "refreshToken");
        this.expirationTimeMilliseconds = (Long) entry.getProperty(SCHEMA,
                "expirationTimeMilliseconds");
        this.serviceName = (String) entry.getProperty(SCHEMA, "serviceName");
        this.nuxeoLogin = (String) entry.getProperty(SCHEMA, "nuxeoLogin");
        this.clientId = (String) entry.getProperty(SCHEMA, "clientId");
        this.creationDate = (Calendar) entry.getProperty(SCHEMA, "creationDate");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("serviceName", serviceName);
        map.put("nuxeoLogin", nuxeoLogin);
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        map.put("expirationTimeMilliseconds", expirationTimeMilliseconds);
        map.put("clientId", clientId);
        map.put("creationDate", creationDate);
        return map;
    }

    public Map<String, Object> toJsonObject() {
        Map<String, Object> m = new HashMap<>();
        m.put("access_token", accessToken);
        m.put("refresh_token", refreshToken);
        m.put("token_type", "bearer");
        m.put("expires_in",
                Math.floor((creationDate.getTimeInMillis()
                        + expirationTimeMilliseconds - new Date().getTime()) / 1000));
        return m;
    }

    public void updateEntry(DocumentModel entry) throws ClientException {
        entry.setProperty(SCHEMA, "serviceName", this.serviceName);
        entry.setProperty(SCHEMA, "nuxeoLogin", this.nuxeoLogin);
        entry.setProperty(SCHEMA, "accessToken", this.accessToken);
        entry.setProperty(SCHEMA, "refreshToken", this.refreshToken);
        entry.setProperty(SCHEMA, "expirationTimeMilliseconds",
                this.expirationTimeMilliseconds);
        entry.setProperty(SCHEMA, "clientId", this.clientId);
    }

    public void refresh() {
        accessToken = RandomStringUtils.random(32, true, true);
        refreshToken = RandomStringUtils.random(64, true, true);
        creationDate = Calendar.getInstance();
    }

    public boolean isExpired() {
        return creationDate != null
                && creationDate.getTimeInMillis() + expirationTimeMilliseconds < Calendar.getInstance().getTimeInMillis();
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setNuxeoLogin(String userId) {
        this.nuxeoLogin = userId;
    }

    public String getNuxeoLogin() {
        return nuxeoLogin;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpirationTimeMilliseconds() {
        return expirationTimeMilliseconds;
    }

    public void setExpirationTimeMilliseconds(Long expirationTimeMilliseconds) {
        this.expirationTimeMilliseconds = expirationTimeMilliseconds;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
