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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIdUserInfo;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

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
public class OpenIDConnectProvider {

    protected static final Log log = LogFactory.getLog(OpenIDConnectProvider.class);

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private boolean enabled = true;

    NuxeoOAuth2ServiceProvider oauth2Provider;

    private String userInfoURL;

    private String icon;

    public OpenIDConnectProvider(NuxeoOAuth2ServiceProvider oauth2Provider,
            String userInfoURL, String icon, boolean enabled) {
        this.oauth2Provider = oauth2Provider;
        this.userInfoURL = userInfoURL;
        this.icon = icon;
        this.enabled = enabled;
    }

    public String getRedirectUri(HttpServletRequest req) {
        return VirtualHostHelper.getBaseURL(req) + "nxstartup.faces?provider="
                + oauth2Provider.getServiceName();
    }

    public String getAuthenticationUrl(HttpServletRequest req,
            String requestedUrl) {
        // redirect to the authorization flow
        AuthorizationCodeFlow flow = oauth2Provider.getAuthorizationCodeFlow(
                HTTP_TRANSPORT, JSON_FACTORY);
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl(); // .setResponseTypes("token");
        authorizationUrl.setRedirectUri(getRedirectUri(req));

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
            if (type.contains("text/plain")) {
                String str = response.parseAsString();
                String[] params = str.split("&");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv[0].equals("access_token")) {
                        accessToken = kv[1]; // get the token
                        break;
                    }
                }
            } else { // try to parse as JSON

                TokenResponse tokenResponse = response.parseAs(TokenResponse.class);
                accessToken = tokenResponse.getAccessToken();

            }
        } catch (IOException e) {
            log.error("Unable to parse server response", e);
        }

        return accessToken;
    }

    public OpenIdUserInfo getUserInfo(String accessToken) {
        OpenIdUserInfo userInfo = null;

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.setParser(new JsonObjectParser(JSON_FACTORY));
            }
        });

        GenericUrl url = new GenericUrl(userInfoURL);
        url.set("access_token", accessToken);

        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            HttpResponse response = request.execute();
            userInfo = response.parseAs(OpenIdUserInfo.class);

        } catch (IOException e) {
            log.error("Unable to parse server response", e);
        }

        return userInfo;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
