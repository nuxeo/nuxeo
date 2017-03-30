/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Returns the authorization URL
     *
     * @since 9.2
     */
    String getAuthorizationUrl(String serverURL);

    /**
     * Handles the authorization response and stores the token(s)
     */
    Credential handleAuthorizationCallback(HttpServletRequest request);

    /**
     * Loads a credential from the token store
     */
    Credential loadCredential(String user);

    void setId(Long id);

    /**
     * @since 9.2
     */
    void setDescription(String description);

    void setAuthorizationServerURL(String authorizationServerURL);

    void setTokenServerURL(String tokenServerURL);

    /**
     * @since 9.2
     */
    void setUserAuthorizationURL(String userAuthorizationURL);

    void setServiceName(String serviceName);

    void setClientId(String clientId);

    void setClientSecret(String clientSecret);

    void setScopes(String... strings);

    String getServiceName();

    Long getId();

    /**
     * @since 9.2
     */
    String getDescription();

    String getTokenServerURL();

    /**
     * @since 9.2
     */
    String getUserAuthorizationURL();

    String getClientId();

    String getClientSecret();

    List<String> getScopes();

    String getAuthorizationServerURL();

    boolean isEnabled();

    void setEnabled(Boolean enabled);

    /**
     * @since 7.4
     */
    boolean isProviderAvailable();
}
