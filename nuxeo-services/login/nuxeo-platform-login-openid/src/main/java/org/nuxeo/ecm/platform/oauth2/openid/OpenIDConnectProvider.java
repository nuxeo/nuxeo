/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.openid.auth.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDConnectAuthenticator;
import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfo;
import org.nuxeo.ecm.platform.oauth2.openid.auth.UserMapperResolver;
import org.nuxeo.ecm.platform.oauth2.openid.auth.UserResolver;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * Class that holds info about an OpenID provider, this includes an OAuth Provider as well as urls and icons
 *
 * @author Nelson Silva <nelson.silva@inevo.pt>
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class OpenIDConnectProvider implements LoginProviderLinkComputer {

    protected static final Log log = LogFactory.getLog(OpenIDConnectProvider.class);

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private boolean enabled = true;

    OAuth2ServiceProvider oauth2Provider;

    private String userInfoURL;

    private String icon;

    protected RedirectUriResolver redirectUriResolver;

    protected UserResolver userResolver;

    protected String userMapper;

    private String accessTokenKey;

    protected String authenticationMethod;

    private Class<? extends OpenIDUserInfo> openIdUserInfoClass;
    
    /**
     * @deprecated since 11.1, use
     *             {@link #OpenIDConnectProvider(OAuth2ServiceProvider, String, String, Class, String, boolean, RedirectUriResolver, Class, String, String)}
     */
    @Deprecated
    public OpenIDConnectProvider(OAuth2ServiceProvider oauth2Provider, String accessTokenKey, String userInfoURL,
            Class<? extends OpenIDUserInfo> openIdUserInfoClass, String icon, boolean enabled,
            RedirectUriResolver redirectUriResolver, Class<? extends UserResolver> userResolverClass,
            String userMapper) {
        this(oauth2Provider, accessTokenKey, userInfoURL, openIdUserInfoClass, icon, enabled, redirectUriResolver,
                userResolverClass, userMapper, OpenIDConnectProviderDescriptor.DEFAULT_AUTHENTICATION_METHOD);
    }

    public OpenIDConnectProvider(OAuth2ServiceProvider oauth2Provider, String accessTokenKey, String userInfoURL,
            Class<? extends OpenIDUserInfo> openIdUserInfoClass, String icon, boolean enabled,
            RedirectUriResolver redirectUriResolver, Class<? extends UserResolver> userResolverClass, String userMapper,
            String authenticationMethod) {
        this.oauth2Provider = oauth2Provider;
        this.userInfoURL = userInfoURL;
        this.openIdUserInfoClass = openIdUserInfoClass;
        this.icon = icon;
        this.enabled = enabled;
        this.accessTokenKey = accessTokenKey;
        this.redirectUriResolver = redirectUriResolver;
        this.authenticationMethod = authenticationMethod;

        try {
            if (userResolverClass == null) {
                if (userMapper != null) {
                    userResolver = new UserMapperResolver(this, userMapper);
                } else {
                    userResolver = new EmailBasedUserResolver(this);
                }
            } else {
                Constructor<? extends UserResolver> c = userResolverClass.getConstructor(OpenIDConnectProvider.class);
                userResolver = c.newInstance(this);
            }
        } catch (ReflectiveOperationException e) {
            log.error("Failed to instantiate UserResolver", e);
        }
    }

    public String getRedirectUri(HttpServletRequest req) {
        return redirectUriResolver.getRedirectUri(this, req);
    }

    /**
     * Create a state token to prevent request forgery. Store it in the session for later validation.
     */
    public String createStateToken(HttpServletRequest request) {
        String state = new BigInteger(130, new SecureRandom()).toString(32);
        request.getSession().setAttribute(OpenIDConnectAuthenticator.STATE_SESSION_ATTRIBUTE + "_" + getName(), state);
        return state;
    }

    /**
     * Ensure that this is no request forgery going on, and that the user sending us this connect request is the user
     * that was supposed to.
     */
    public boolean verifyStateToken(HttpServletRequest request) {
        return request.getParameter(OpenIDConnectAuthenticator.STATE_URL_PARAM_NAME)
                      .equals(request.getSession().getAttribute(
                              OpenIDConnectAuthenticator.STATE_SESSION_ATTRIBUTE + "_" + getName()));
    }

    public String getAuthenticationUrl(HttpServletRequest req, String requestedUrl) {
        // redirect to the authorization flow
        AuthorizationCodeFlow flow = ((NuxeoOAuth2ServiceProvider) oauth2Provider).getAuthorizationCodeFlow();
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl(); // .setResponseTypes("token");
        authorizationUrl.setRedirectUri(getRedirectUri(req));

        String state = createStateToken(req);
        authorizationUrl.setState(state);

        return authorizationUrl.build();
    }

    public String getName() {
        return oauth2Provider != null ? oauth2Provider.getServiceName() : null;
    }

    public String getIcon() {
        return icon;
    }

    public String getAccessToken(HttpServletRequest req, String code) {
        String accessToken = null;

        HttpResponse response = null;

        try {
            AuthorizationCodeFlow flow = ((NuxeoOAuth2ServiceProvider) oauth2Provider).getAuthorizationCodeFlow();

            String redirectUri = getRedirectUri(req);
            response = flow.newTokenRequest(code).setRedirectUri(redirectUri).executeUnparsed();
        } catch (IOException e) {
            log.error("Error during OAuth2 Authorization", e);
            return null;
        }

        HttpMediaType mediaType = response.getMediaType();
        if (mediaType != null && "json".equals(mediaType.getSubType())) {
            // Try to parse as json
            try {
                TokenResponse tokenResponse = response.parseAs(TokenResponse.class);
                accessToken = tokenResponse.getAccessToken();
            } catch (IOException e) {
                log.warn("Unable to parse accesstoken as JSON", e);
            }
        } else {
            // Fallback as plain text format
            try {
                String[] params = response.parseAsString().split("&");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv[0].equals("access_token")) {
                        accessToken = kv[1]; // get the token
                        break;
                    }
                }
            } catch (IOException e) {
                log.warn("Unable to parse accesstoken as plain text", e);
            }
        }

        return accessToken;
    }

    public OpenIDUserInfo getUserInfo(String accessToken) {
        OpenIDUserInfo userInfo = null;

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> request.setParser(new JsonObjectParser(
                JSON_FACTORY)));

        GenericUrl url = new GenericUrl(userInfoURL);
        if (OpenIDConnectProviderDescriptor.URL_AUTHENTICATION_METHOD.equals(authenticationMethod)) {
            url.set(accessTokenKey, accessToken);
        }

        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            if (OpenIDConnectProviderDescriptor.BEARER_AUTHENTICATION_METHOD.equals(authenticationMethod)) {
                request.getHeaders().put("Authorization", Arrays.asList("Bearer " + accessToken));
            }
            HttpResponse response = request.execute();
            String body = IOUtils.toString(response.getContent(), "UTF-8");
            log.debug(body);
            userInfo = parseUserInfo(body);

        } catch (IOException e) {
            log.error("Unable to parse server response", e);
        }

        return userInfo;
    }

    public OpenIDUserInfo parseUserInfo(String userInfoJSON) throws IOException {
        return new JsonObjectParser(JSON_FACTORY).parseAndClose(new StringReader(userInfoJSON), openIdUserInfoClass);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UserResolver getUserResolver() {
        return userResolver;
    }

    @Override
    public String computeUrl(HttpServletRequest req, String requestedUrl) {
        return getAuthenticationUrl(req, requestedUrl);
    }
}
