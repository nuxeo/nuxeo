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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.SecureRandom;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDConnectAuthenticator;
import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfo;
import org.nuxeo.ecm.platform.oauth2.openid.auth.UserResolver;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

/**
 * Class that holds info about an OpenID provider, this includes an OAuth
 * Provider as well as urls and icons
 *
 * @author Nelson Silva <nelson.silva@inevo.pt>
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class OpenIDConnectProvider implements LoginProviderLinkComputer {

    protected static final Log log = LogFactory.getLog(OpenIDConnectProvider.class);

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private boolean enabled = true;

    NuxeoOAuth2ServiceProvider oauth2Provider;

    private String userInfoURL;

    private String icon;

    protected RedirectUriResolver redirectUriResolver;

    protected UserResolver userResolver;

    private String accessTokenKey;

    private Class<? extends OpenIDUserInfo> openIdUserInfoClass;

    public OpenIDConnectProvider(NuxeoOAuth2ServiceProvider oauth2Provider,
            String accessTokenKey, String userInfoURL, Class<? extends OpenIDUserInfo> openIdUserInfoClass,
            String icon, boolean enabled,
            RedirectUriResolver redirectUriResolver,
            Class<? extends UserResolver> userResolverClass) {
        this.oauth2Provider = oauth2Provider;
        this.userInfoURL = userInfoURL;
        this.openIdUserInfoClass = openIdUserInfoClass;
        this.icon = icon;
        this.enabled = enabled;
        this.accessTokenKey = accessTokenKey;
        this.redirectUriResolver = redirectUriResolver;

        try {
            Constructor<? extends UserResolver> c = userResolverClass.getConstructor(new Class[]{OpenIDConnectProvider.class});
            userResolver = c.newInstance(new Object[]{this});
        } catch (Exception e) {
            log.error("Failed to instantiate UserResolver", e);
        }

    }

    public String getRedirectUri(HttpServletRequest req) {
        return redirectUriResolver.getRedirectUri(this, req);
    }

    /**
     * Create a state token to prevent request forgery.
     * Store it in the session for later validation.
     * @param HttpServletRequest request
     * @return
     */
    public String createStateToken(HttpServletRequest request) {
        String state = new BigInteger(130, new SecureRandom()).toString(32);
        request.getSession().setAttribute(OpenIDConnectAuthenticator.STATE_SESSION_ATTRIBUTE + "_" + getName(), state);
        return state;
    }

    /**
     * Ensure that this is no request forgery going on, and that the user
     * sending us this connect request is the user that was supposed to.
     * @param HttpServletRequest request
     * @return
     */
    public boolean verifyStateToken(HttpServletRequest request) {
        return request.getParameter(OpenIDConnectAuthenticator.STATE_URL_PARAM_NAME).equals(
                request.getSession().getAttribute(OpenIDConnectAuthenticator.STATE_SESSION_ATTRIBUTE + "_" + getName()));
    }

    public String getAuthenticationUrl(HttpServletRequest req,
            String requestedUrl) {
        // redirect to the authorization flow
        AuthorizationCodeFlow flow = oauth2Provider.getAuthorizationCodeFlow(
                HTTP_TRANSPORT, JSON_FACTORY);
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl(); // .setResponseTypes("token");
        authorizationUrl.setRedirectUri(getRedirectUri(req));

        String state = createStateToken(req);
        authorizationUrl.setState(state);

        String authUrl = authorizationUrl.build();

        return authUrl;
    }

    public String getName() {
        return oauth2Provider.getServiceName();
    }

    public String getIcon() {
        return icon;
    }

    public String getAccessToken(HttpServletRequest req, String code) {
        String accessToken = null;

        HttpResponse response = null;

        try {
            AuthorizationCodeFlow flow = oauth2Provider.getAuthorizationCodeFlow(
                    HTTP_TRANSPORT, JSON_FACTORY);

            String redirectUri = getRedirectUri(req);
            response = flow.newTokenRequest(code).setRedirectUri(redirectUri).executeUnparsed();
        } catch (IOException e) {
            log.error("Error during OAuth2 Authorization", e);
        }

        String type = response.getContentType();


        try {
            // Try to parse as json
            TokenResponse tokenResponse = response.parseAs(TokenResponse.class);
            accessToken = tokenResponse.getAccessToken();
        } catch (IOException e) {
            log.debug("Unable to parse accesstoken as JSON", e);
        }

        if (StringUtils.isBlank(accessToken)) {
            // Fallback as plain text format
            try {
                String str = response.parseAsString();
                String[] params = str.split("&");
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

        if (StringUtils.isBlank(accessToken)) {
            log.error("Unable to parse access token from response.");
        }

        return accessToken;
    }

    public OpenIDUserInfo getUserInfo(String accessToken) {
        OpenIDUserInfo userInfo = null;

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.setParser(new JsonObjectParser(JSON_FACTORY));
            }
        });

        GenericUrl url = new GenericUrl(userInfoURL);
        url.set(accessTokenKey, accessToken);

        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
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
