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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class OpenIDProviderFragmentRegistry extends
        ContributionFragmentRegistry<OpenIDConnectProviderDescriptor> {

    protected final Map<String, OpenIDConnectProviderDescriptor> providers = new HashMap<String, OpenIDConnectProviderDescriptor>();

    @Override
    public OpenIDConnectProviderDescriptor clone(
            OpenIDConnectProviderDescriptor source) {

        OpenIDConnectProviderDescriptor copy = new OpenIDConnectProviderDescriptor();

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
        copy.redirectUriResolver = source.redirectUriResolver;
        copy.userResolverClass = source.userResolverClass;
        copy.accessTokenKey = source.accessTokenKey;
        copy.userInfoClass = source.userInfoClass;
        return copy;
    }

    @Override
    public void contributionRemoved(String name,
            OpenIDConnectProviderDescriptor origContrib) {
        providers.remove(name);
    }

    @Override
    public void contributionUpdated(String name,
            OpenIDConnectProviderDescriptor contrib,
            OpenIDConnectProviderDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            providers.put(name, contrib);
        } else {
            providers.remove(name);
        }
    }

    @Override
    public String getContributionId(OpenIDConnectProviderDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void merge(OpenIDConnectProviderDescriptor src,
            OpenIDConnectProviderDescriptor dst) {

        if (dst.authorizationServerURL == null
                || dst.authorizationServerURL.isEmpty()) {
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
        if (!src.accessTokenKey.equals(OpenIDConnectProviderDescriptor.DEFAULT_ACCESS_TOKEN_KEY)) {
            dst.accessTokenKey = src.accessTokenKey;
        }
        if (src.userInfoClass != OpenIDConnectProviderDescriptor.DEFAULT_USER_INFO_CLASS) {
            dst.userInfoClass = src.userInfoClass;
        }
        if (src.redirectUriResolver != OpenIDConnectProviderDescriptor.DEFAULT_REDIRECT_URI_RESOLVER_CLASS) {
            dst.redirectUriResolver = src.redirectUriResolver;
        }
        if (src.userResolverClass != OpenIDConnectProviderDescriptor.DEFAULT_USER_RESOLVER_CLASS) {
            dst.userResolverClass = src.userResolverClass;
        }

        dst.accessTokenKey = src.accessTokenKey;

        dst.userInfoClass = src.userInfoClass;

        dst.redirectUriResolver = src.redirectUriResolver;

        dst.userResolverClass = src.userResolverClass;

        dst.enabled = src.enabled;
    }

    public Collection<OpenIDConnectProviderDescriptor> getContribs() {
        return providers.values();
    }
}
