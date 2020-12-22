/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.oauth2.openid;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Nelson Silva
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class OpenIDConnectProviderRegistryImpl extends DefaultComponent implements OpenIDConnectProviderRegistry {

    protected static final Logger log = LogManager.getLogger(OpenIDConnectProviderRegistryImpl.class);

    public static final String PROVIDER_EP = "providers";

    protected Map<String, OpenIDConnectProvider> providers = new HashMap<>();

    protected OAuth2ServiceProviderRegistry getOAuth2ServiceProviderRegistry() {
        return Framework.getService(OAuth2ServiceProviderRegistry.class);
    }

    @Override
    public Collection<OpenIDConnectProvider> getProviders() {
        return providers.values();
    }

    @Override
    public Collection<OpenIDConnectProvider> getEnabledProviders() {
        return getProviders().stream().filter(OpenIDConnectProvider::isEnabled).collect(Collectors.toList());
    }

    @Override
    public OpenIDConnectProvider getProvider(String name) {
        return providers.get(name);
    }

    protected void registerPendingProviders() {
        List<OpenIDConnectProviderDescriptor> providers = getRegistryContributions(PROVIDER_EP);
        for (OpenIDConnectProviderDescriptor provider : providers) {
            // former registry discarded disabled providers
            if (provider.isEnabled()) {
                registerOpenIdProvider(provider);
            }
        }
    }

    protected void registerOpenIdProvider(OpenIDConnectProviderDescriptor provider) {

        OAuth2ServiceProviderRegistry oauth2ProviderRegistry = getOAuth2ServiceProviderRegistry();
        RedirectUriResolver redirectUriResolver;
        try {
            redirectUriResolver = provider.getRedirectUriResolver().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        if (oauth2ProviderRegistry != null) {

            OAuth2ServiceProvider oauth2Provider = oauth2ProviderRegistry.getProvider(provider.getName());

            if (oauth2Provider == null) {
                oauth2Provider = oauth2ProviderRegistry.addProvider(provider.getName(), provider.getDescription(),
                        provider.getTokenServerURL(), provider.getAuthorizationServerURL(), provider.getClientId(),
                        provider.getClientSecret(), Arrays.asList(provider.getScopes()));
            } else {
                log.warn("Provider {} is already in the Database, XML contribution  won't overwrite it",
                        provider::getName);
            }
            providers.put(provider.getName(),
                    new OpenIDConnectProvider(oauth2Provider, provider.getAccessTokenKey(), provider.getUserInfoURL(),
                            provider.getUserInfoClass(), provider.getIcon(), provider.isEnabled(), redirectUriResolver,
                            provider.getUserResolverClass(), provider.getUserMapper(),
                            provider.getAuthenticationMethod()));

            // contribute icon and link to the Login Screen
            LoginScreenHelper.registerSingleProviderLoginScreenConfig(provider.getName(), provider.getIcon(),
                    provider.getUserInfoURL(), provider.getLabel(), provider.getDescription(),
                    providers.get(provider.getName()));

        } else {
            if (Framework.isTestModeSet()) {
                providers.put(provider.getName(),
                        new OpenIDConnectProvider(null, provider.getAccessTokenKey(), provider.getUserInfoURL(),
                                provider.getUserInfoClass(), provider.getIcon(), provider.isEnabled(),
                                redirectUriResolver, provider.getUserResolverClass(), provider.getUserMapper(),
                                provider.getAuthenticationMethod()));
            } else {
                log.error("Can not register OAuth Provider since OAuth Registry is not available");
            }
        }

    }

    @Override
    public void start(ComponentContext context) {
        registerPendingProviders();
    }

}
