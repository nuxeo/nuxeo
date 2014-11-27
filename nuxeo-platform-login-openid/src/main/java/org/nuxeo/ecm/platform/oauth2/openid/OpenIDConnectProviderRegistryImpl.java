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

package org.nuxeo.ecm.platform.oauth2.openid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Nelson Silva <nelson.silva@inevo.pt>
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class OpenIDConnectProviderRegistryImpl extends DefaultComponent
        implements OpenIDConnectProviderRegistry {

    protected static final Log log = LogFactory.getLog(OpenIDConnectProviderRegistryImpl.class);

    public static final String PROVIDER_EP = "providers";

    protected Map<String, OpenIDConnectProvider> providers = new HashMap<String, OpenIDConnectProvider>();

    protected OpenIDProviderFragmentRegistry pendingProviders = new OpenIDProviderFragmentRegistry();

    protected OAuth2ServiceProviderRegistry getOAuth2ServiceProviderRegistry() {
        return Framework.getLocalService(OAuth2ServiceProviderRegistry.class);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (PROVIDER_EP.equals(extensionPoint)) {
            OpenIDConnectProviderDescriptor provider = (OpenIDConnectProviderDescriptor) contribution;

            if (provider.getClientId() == null
                    || provider.getClientSecret() == null) {
                log.info("OpenId provider for "
                        + provider.getName()
                        + " is disabled because clientId and/or clientSecret are empty (component id = "
                        + contributor.getName().toString() + ")");
                provider.setEnabled(false);
            }
            log.info("OpenId provider for " + provider.getName()
                    + " will be registred at application startup");
            // delay registration because data sources may not be available
            // at this point
            pendingProviders.addContribution(provider);
        }
    }

    @Override
    public Collection<OpenIDConnectProvider> getProviders() {
        return providers.values();
    }

    @Override
    public Collection<OpenIDConnectProvider> getEnabledProviders() {
        List<OpenIDConnectProvider> result = new ArrayList<OpenIDConnectProvider>();
        for (OpenIDConnectProvider provider : getProviders()) {
            if (provider.isEnabled()) {
                result.add(provider);
            }
        }
        return result;
    }

    @Override
    public OpenIDConnectProvider getProvider(String name) {
        return providers.get(name);
    }

    protected void registerPendingProviders() {
        for (OpenIDConnectProviderDescriptor provider : pendingProviders.getContribs()) {
            registerOpenIdProvider(provider);
        }
    }

    protected void registerOpenIdProvider(
            OpenIDConnectProviderDescriptor provider) {

        OAuth2ServiceProviderRegistry oauth2ProviderRegistry = getOAuth2ServiceProviderRegistry();
        RedirectUriResolver redirectUriResolver;
        try {
            redirectUriResolver = provider.getRedirectUriResolver().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        if (oauth2ProviderRegistry != null) {

            NuxeoOAuth2ServiceProvider oauth2Provider = oauth2ProviderRegistry.getProvider(provider.getName());

            if (oauth2Provider == null) {
                oauth2Provider = oauth2ProviderRegistry.addProvider(
                        provider.getName(), provider.getTokenServerURL(),
                        provider.getAuthorizationServerURL(),
                        provider.getClientId(), provider.getClientSecret(),
                        Arrays.asList(provider.getScopes()));
            } else {
                log.warn("Provider "
                        + provider.getName()
                        + " is already in the Database, XML contribution  won't overwrite it");
            }
            providers.put(
                    provider.getName(),
                    new OpenIDConnectProvider(oauth2Provider,
                            provider.getAccessTokenKey(),
                            provider.getUserInfoURL(),
                            provider.getUserInfoClass(),
                            provider.getIcon(),
                            provider.isEnabled(),
                            redirectUriResolver,
                            provider.getUserResolverClass()));

            // contribute icon and link to the Login Screen
            LoginScreenHelper.registerLoginProvider(provider.getName(),
                    provider.getIcon(), provider.getUserInfoURL(),
                    provider.getLabel(), provider.getDescription(),
                    providers.get(provider.getName()));

        } else {
            if (Framework.isTestModeSet()) {
                providers.put(provider.getName(), new OpenIDConnectProvider(
                        null, provider.getAccessTokenKey(),
                        provider.getUserInfoURL(),
                        provider.getUserInfoClass(),
                        provider.getIcon(),
                        provider.isEnabled(),
                        redirectUriResolver,
                        provider.getUserResolverClass()));
            } else {
                log.error("Can not register OAuth Provider since OAuth Registry is not available");
            }
        }

    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
        registerPendingProviders();
    }

}
