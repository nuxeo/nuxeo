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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.io.Serializable;

/**
 * @since 7.3
 */
@XObject("provider")
public class OAuth2ServiceProviderDescriptor implements Serializable {
    protected static final long serialVersionUID = 1L;

    public static final String DEFAULT_ACCESS_TOKEN_KEY = "access_token";

    public static final Class<? extends OAuth2ServiceProvider> DEFAULT_PROVIDER_CLASS = NuxeoOAuth2ServiceProvider.class;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("name")
    protected String name;

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

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getName() {
        return name;
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

    public boolean isEnabled() {
        return enabled;
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
}
