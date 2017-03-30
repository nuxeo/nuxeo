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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 7.3
 */
public class OAuth2ServiceProviderContributionRegistry extends ContributionFragmentRegistry<OAuth2ServiceProviderDescriptor> {

    protected static final Log log = LogFactory.getLog(OAuth2ServiceProviderContributionRegistry.class);

    protected final Map<String, OAuth2ServiceProviderDescriptor> providers = new HashMap<>();

    @Override
    public OAuth2ServiceProviderDescriptor clone(OAuth2ServiceProviderDescriptor source) {

        OAuth2ServiceProviderDescriptor copy = new OAuth2ServiceProviderDescriptor();

        copy.scopes = source.scopes;
        copy.authorizationServerURL = source.authorizationServerURL;
        copy.clientId = source.clientId;
        copy.clientSecret = source.clientSecret;
        copy.icon = source.icon;
        copy.enabled = source.enabled;
        copy.name = source.name;
        copy.tokenServerURL = source.tokenServerURL;
        copy.userInfoURL = source.userInfoURL;
        copy.label = source.label;
        copy.description = source.description;
        copy.accessTokenKey = source.accessTokenKey;
        copy.providerClass = source.providerClass;
        return copy;
    }

    @Override
    public void contributionRemoved(String name, OAuth2ServiceProviderDescriptor origContrib) {
        providers.remove(name);
    }

    @Override
    public void contributionUpdated(String name, OAuth2ServiceProviderDescriptor contrib,
        OAuth2ServiceProviderDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            providers.put(name, contrib);
        } else {
            providers.remove(name);
        }
    }

    @Override
    public String getContributionId(OAuth2ServiceProviderDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void merge(OAuth2ServiceProviderDescriptor src, OAuth2ServiceProviderDescriptor dst) {

        if (dst.authorizationServerURL == null || dst.authorizationServerURL.isEmpty()) {
            dst.authorizationServerURL = src.authorizationServerURL;
        }
        if (dst.clientId == null || dst.clientId.isEmpty()) {
            dst.clientId = src.clientId;
        }
        if (dst.clientSecret == null || dst.clientSecret.isEmpty()) {
            dst.clientSecret = src.clientSecret;
        }
        if (dst.icon == null || dst.icon.isEmpty()) {
            dst.icon = src.icon;
        }
        if (dst.scopes == null || dst.scopes.length == 0) {
            dst.scopes = src.scopes;
        }
        if (dst.tokenServerURL == null || dst.tokenServerURL.isEmpty()) {
            dst.tokenServerURL = src.tokenServerURL;
        }
        if (dst.userInfoURL == null || dst.userInfoURL.isEmpty()) {
            dst.userInfoURL = src.userInfoURL;
        }
        if (dst.label == null || dst.label.isEmpty()) {
            dst.label = src.label;
        }
        if (dst.description == null || dst.description.isEmpty()) {
            dst.description = src.description;
        }
        if (!src.accessTokenKey.equals(OAuth2ServiceProviderDescriptor.DEFAULT_ACCESS_TOKEN_KEY)) {
            dst.accessTokenKey = src.accessTokenKey;
        }
        if (src.providerClass != OAuth2ServiceProviderDescriptor.DEFAULT_PROVIDER_CLASS) {
            dst.providerClass = src.providerClass;
        }

        dst.accessTokenKey = src.accessTokenKey;

        dst.enabled = src.enabled;
    }

    public OAuth2ServiceProvider getProvider(String name) {
        OAuth2ServiceProvider provider = null;
        OAuth2ServiceProviderDescriptor descriptor = providers.get(name);
        if (descriptor != null && descriptor.isEnabled()) {
            try {
                Class<? extends OAuth2ServiceProvider> providerClass = descriptor.getProviderClass();
                provider = providerClass.newInstance();
                provider.setDescription(descriptor.getDescription());
                provider.setAuthorizationServerURL(descriptor.getAuthorizationServerURL());
                provider.setTokenServerURL(descriptor.getTokenServerURL());
                provider.setServiceName(descriptor.getName());
                provider.setClientId(descriptor.getClientId());
                provider.setClientSecret(descriptor.getClientSecret());
                provider.setScopes(descriptor.getScopes());
                provider.setEnabled(descriptor.isEnabled());
            } catch (Exception e) {
                log.error("Failed to instantiate UserResolver", e);
            }
        }
        return provider;
    }

    public Collection<OAuth2ServiceProviderDescriptor> getContribs() {
        return providers.values();
    }
}
