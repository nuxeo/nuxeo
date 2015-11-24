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

package org.nuxeo.ecm.platform.oauth2.openid.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
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
 * Servlet that handles OAuth2 Callbacks
 *
 * @author Nelson Silva <nelson.silva@inevo.pt>
 *
 */
public class OAuth2CallbackHandlerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(OAuth2CallbackHandlerServlet.class);

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    public static final String CODE_URL_PARAM_NAME = "code";

    public static final String ERROR_URL_PARAM_NAME = "error";

    public static final String INSTALLED_APP_URL_PARAMETER = "app";

    public static final String INSTALLED_APP_USER_ID = "system";

    /** The URL suffix of the servlet */
    public static final String URL_MAPPING = "/openid";

    /**
     * The URL to redirect the user to after handling the callback. Consider
     * saving this in a cookie before redirecting users to the Google
     * authorization URL if you have multiple possible URL to redirect people
     * to.
     */
    public static final String REDIRECT_URL = "/";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Getting the "error" URL parameter
        String error = req.getParameter(ERROR_URL_PARAM_NAME);

        // / Checking if there was an error such as the user denied access
        if (error != null && error.length() > 0) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                    "There was an error: \"" + error + "\".");
            return;
        }

        // Getting the "code" URL parameter
        String code = req.getParameter(CODE_URL_PARAM_NAME);

        // Checking conditions on the "code" URL parameter
        if (code == null || code.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "There was an error: \"" + error + "\".");
            return;
        }

        String path = req.getRequestURI().split(URL_MAPPING + "/")[1];
        String[] parts = path.split("/");
        String serviceProviderName = parts[0];

        NuxeoOAuth2ServiceProvider provider;
        try {
            provider = getServiceProvider(serviceProviderName);

            if (provider == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "No service provider called: \"" + serviceProviderName
                                + "\".");
                return;
            }

            AuthorizationCodeFlow flow = provider.getAuthorizationCodeFlow(
                    HTTP_TRANSPORT, JSON_FACTORY);

            String redirectUri = req.getRequestURL().toString();

            HttpResponse response = flow.newTokenRequest(code).setRedirectUri(
                    redirectUri).executeUnparsed();
            TokenResponse tokenResponse = response.parseAs(TokenResponse.class);

            // Validate the token
            String accessToken = tokenResponse.getAccessToken();

            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                }
            });

            GenericUrl url = new GenericUrl(
                    "https://www.googleapis.com/oauth2/v1/tokeninfo");
            url.set("access_token", accessToken);

            HttpRequest request = requestFactory.buildGetRequest(url);
            response = request.execute();

            // TODO - get the email
            String email = "nelson.silva@gmail.com";

            UserManager manager = Framework.getLocalService(UserManager.class);

            Map<String, Serializable> query = new HashMap<String, Serializable>();
            query.put(manager.getUserEmailField(), email);

            DocumentModelList users = manager.searchUsers(query, null);

            if (users.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "No user found with email: \"" + email + "\".");
            }

            DocumentModel user = users.get(0);
            String userId = (String) user.getPropertyValue(manager.getUserIdField());

            Framework.loginAs(userId);
            LoginContext loginContext = NuxeoAuthenticationFilter.loginAs(userId);
            loginContext.login();

            resp.sendRedirect(req.getContextPath());
        } catch (Exception e) {
            log.error("Error while processing OAuth2 Callback", e);
        }

    }

    protected static NuxeoOAuth2ServiceProvider getServiceProvider(
            String serviceName) throws Exception {
        OAuth2ServiceProviderRegistry registry;
        registry = Framework.getLocalService(OAuth2ServiceProviderRegistry.class);
        NuxeoOAuth2ServiceProvider nuxeoServiceProvider = registry.getProvider(serviceName);

        return nuxeoServiceProvider;
    }

}
