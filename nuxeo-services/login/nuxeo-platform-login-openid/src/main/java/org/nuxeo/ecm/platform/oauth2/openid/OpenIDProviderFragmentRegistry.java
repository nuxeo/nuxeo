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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 * @deprecated since 11.1
 */
@Deprecated
public class OpenIDProviderFragmentRegistry extends ContributionFragmentRegistry<OpenIDConnectProviderDescriptor> {

    protected final Map<String, OpenIDConnectProviderDescriptor> providers = new HashMap<>();

    @Override
    public OpenIDConnectProviderDescriptor clone(OpenIDConnectProviderDescriptor source) {

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
        copy.userMapper = source.userMapper;
        copy.authenticationMethod = source.authenticationMethod;
        return copy;
    }

    @Override
    public void contributionRemoved(String name, OpenIDConnectProviderDescriptor origContrib) {
        providers.remove(name);
    }

    @Override
    public void contributionUpdated(String name, OpenIDConnectProviderDescriptor contrib,
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
    public void merge(OpenIDConnectProviderDescriptor src, OpenIDConnectProviderDescriptor dst) {

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
        if (!src.accessTokenKey.equals(OpenIDConnectProviderDescriptor.DEFAULT_ACCESS_TOKEN_KEY)) {
            dst.accessTokenKey = src.accessTokenKey;
        }
        if (src.userInfoClass != OpenIDConnectProviderDescriptor.DEFAULT_USER_INFO_CLASS) {
            dst.userInfoClass = src.userInfoClass;
        }
        if (src.redirectUriResolver != OpenIDConnectProviderDescriptor.DEFAULT_REDIRECT_URI_RESOLVER_CLASS) {
            dst.redirectUriResolver = src.redirectUriResolver;
        }
        if (src.getUserResolverClass() != OpenIDConnectProviderDescriptor.DEFAULT_USER_RESOLVER_CLASS) {
            dst.userResolverClass = src.userResolverClass;
        }
        if (src.userMapper != null && src.userMapper.length() > 0) {
            dst.userMapper = src.userMapper;
        }
        if (!src.authenticationMethod.equals(OpenIDConnectProviderDescriptor.DEFAULT_AUTHENTICATION_METHOD)) {
            dst.authenticationMethod = src.authenticationMethod;
        }

        dst.enabled = src.enabled;
    }

    public Collection<OpenIDConnectProviderDescriptor> getContribs() {
        return providers.values();
    }
}
