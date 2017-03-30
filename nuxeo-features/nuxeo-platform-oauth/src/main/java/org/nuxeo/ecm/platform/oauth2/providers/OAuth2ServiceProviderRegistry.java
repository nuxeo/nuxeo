/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import java.util.List;

/**
 * This service is used to manage OAuth2 Service Providers
 */
public interface OAuth2ServiceProviderRegistry {

    OAuth2ServiceProvider getProvider(String serviceName);

    List<OAuth2ServiceProvider> getProviders();

    OAuth2ServiceProvider addProvider(String serviceName, String description, String tokenServerURL,
        String authorizationServerURL, String clientId, String clientSecret,
        List<String> scopes);

    OAuth2ServiceProvider addProvider(String serviceName, String description, String tokenServerURL,
        String authorizationServerURL, String userAuthorizationURL, String clientId, String clientSecret,
        List<String> scopes, Boolean isEnabled);

    /**
     * @since 9.2
     */
    OAuth2ServiceProvider updateProvider(String serviceName, OAuth2ServiceProvider provider);

    /**
     * @since 9.2
     */
    void deleteProvider(String serviceName);

}
