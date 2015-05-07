/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import com.google.api.client.auth.oauth2.Credential;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @since 7.3
 */
public interface OAuth2ServiceProvider {

    /**
     * Returns the authorization URL
     */
    String getAuthorizationUrl(HttpServletRequest request);

    /**
     * Handles the authorization response and stores the token(s)
     */
    Credential handleAuthorizationCallback(HttpServletRequest request);

    /**
     * Loads a credential from the token store
     */
    Credential loadCredential(String user);

    void setId(Long id);

    void setAuthorizationServerURL(String authorizationServerURL);

    void setTokenServerURL(String tokenServerURL);

    void setServiceName(String serviceName);

    void setClientId(String clientId);

    void setClientSecret(String clientSecret);

    void setScopes(String... strings);

    String getServiceName();

    Long getId();

    String getTokenServerURL();

    String getClientId();

    String getClientSecret();

    List<String> getScopes();

    String getAuthorizationServerURL();

    boolean isEnabled();

    void setEnabled(Boolean enabled);
}
