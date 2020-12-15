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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.oauth2.openid.auth.DefaultOpenIDUserInfo;
import org.nuxeo.ecm.platform.oauth2.openid.auth.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfo;
import org.nuxeo.ecm.platform.oauth2.openid.auth.UserResolver;

@XObject("provider")
@XRegistry
public class OpenIDConnectProviderDescriptor {

    public static final String DEFAULT_ACCESS_TOKEN_KEY = "access_token";

    public static final Class<? extends UserResolver> DEFAULT_USER_RESOLVER_CLASS = EmailBasedUserResolver.class;

    public static final Class<? extends RedirectUriResolver> DEFAULT_REDIRECT_URI_RESOLVER_CLASS = RedirectUriResolverHelper.class;

    public static final Class<? extends OpenIDUserInfo> DEFAULT_USER_INFO_CLASS = DefaultOpenIDUserInfo.class;

    /**
     * @since 11.1
     */
    public static final String URL_AUTHENTICATION_METHOD = "url";

    /**
     * @since 11.1
     */
    public static final String BEARER_AUTHENTICATION_METHOD = "bearer";

    /**
     * @since 11.1
     */
    public static final String DEFAULT_AUTHENTICATION_METHOD = URL_AUTHENTICATION_METHOD;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("name")
    @XRegistryId
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

    @XNode("userResolverClass")
    protected Class<? extends UserResolver> userResolverClass;

    @XNode("userMapperName")
    protected String userMapper;

    @XNode("redirectUriResolver")
    protected Class<? extends RedirectUriResolver> redirectUriResolver = DEFAULT_REDIRECT_URI_RESOLVER_CLASS;

    @XNode("userInfoClass")
    protected Class<? extends OpenIDUserInfo> userInfoClass = DEFAULT_USER_INFO_CLASS;

    /**
     * @since 11.1
     */
    @XNode("authenticationMethod")
    protected String authenticationMethod = DEFAULT_AUTHENTICATION_METHOD;

    public String getId() {
        return getName();
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

    public String getUserInfoURL() {
        return userInfoURL;
    }

    public String getAccessTokenKey() {
        return accessTokenKey;
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

    public String getUserMapper() {
        return userMapper;
    }

    public Class<? extends UserResolver> getUserResolverClass() {
        if (userResolverClass == null && userMapper == null) {
            return DEFAULT_USER_RESOLVER_CLASS;
        }
        return userResolverClass;
    }

    public Class<? extends RedirectUriResolver> getRedirectUriResolver() {
        return redirectUriResolver;
    }

    public Class<? extends OpenIDUserInfo> getUserInfoClass() {
        return userInfoClass;
    }

    /**
     * @since 11.1
     */
    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

}
