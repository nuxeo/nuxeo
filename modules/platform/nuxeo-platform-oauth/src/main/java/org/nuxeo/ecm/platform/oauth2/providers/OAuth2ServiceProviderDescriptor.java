/*
 * (C) Copyright 2015-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 7.3
 */
@XObject("provider")
public class OAuth2ServiceProviderDescriptor implements Descriptor {

    public static final String DEFAULT_ACCESS_TOKEN_KEY = "access_token";

    public static final Class<? extends OAuth2ServiceProvider> DEFAULT_PROVIDER_CLASS = NuxeoOAuth2ServiceProvider.class;

    @XNode("name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("tokenServerURL")
    protected String tokenServerURL;

    @XNode("authorizationServerURL")
    protected String authorizationServerURL;

    @XNode("userInfoURL")
    protected String userInfoURL;

    @XNode("accessTokenKey")
    protected String accessTokenKey = DEFAULT_ACCESS_TOKEN_KEY;

    @XNode("clientId")
    protected String clientId;

    @XNode("clientSecret")
    protected String clientSecret;

    @XNodeList(value = "scope", type = String[].class, componentType = String.class)
    protected String[] scopes;

    @XNode("icon")
    protected String icon;

    @XNode("label")
    protected String label;

    @XNode("description")
    protected String description;

    @XNode("class")
    protected Class<? extends OAuth2ServiceProvider> providerClass = DEFAULT_PROVIDER_CLASS;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTokenServerURL() {
        return tokenServerURL;
    }

    public String getAuthorizationServerURL() {
        return authorizationServerURL;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String[] getScopes() {
        return scopes;
    }

    public String getIcon() {
        return icon;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Class<? extends OAuth2ServiceProvider> getProviderClass() {
        return providerClass;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (OAuth2ServiceProviderDescriptor) o;
        var merged = new OAuth2ServiceProviderDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.enabled = other.enabled;
        merged.tokenServerURL = defaultIfEmpty(other.tokenServerURL, tokenServerURL);
        merged.authorizationServerURL = defaultIfEmpty(other.authorizationServerURL, tokenServerURL);
        merged.userInfoURL = defaultIfEmpty(other.userInfoURL, userInfoURL);
        merged.accessTokenKey = !DEFAULT_ACCESS_TOKEN_KEY.equals(other.accessTokenKey) ? other.accessTokenKey
                : accessTokenKey;
        merged.clientId = defaultIfEmpty(other.clientId, clientId);
        merged.clientSecret = defaultIfEmpty(other.clientSecret, clientSecret);
        merged.scopes = isNotEmpty(other.scopes) ? other.scopes : scopes;
        merged.icon = defaultIfEmpty(other.icon, icon);
        merged.label = defaultIfEmpty(other.label, label);
        merged.description = defaultIfEmpty(other.description, description);
        merged.providerClass = !DEFAULT_PROVIDER_CLASS.equals(other.providerClass) ? other.providerClass
                : providerClass;
        return merged;
    }
}
