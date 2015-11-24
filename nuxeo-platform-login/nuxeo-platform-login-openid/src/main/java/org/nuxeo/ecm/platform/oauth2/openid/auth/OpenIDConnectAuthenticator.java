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

package org.nuxeo.ecm.platform.oauth2.openid.auth;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProvider;
import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProviderRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.runtime.api.Framework;

/**
 * Authenticator using OpenID to retrieve user identity.
 *
 * @author Nelson Silva <nelson.silva@inevo.pt>
 *
 */
public class OpenIDConnectAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(OpenIDConnectAuthenticator.class);

    public static final String STATE_URL_PARAM_NAME = "state";

    public static final String STATE_SESSION_ATTRIBUTE = STATE_URL_PARAM_NAME;

    public static final String CODE_URL_PARAM_NAME = "code";

    public static final String ERROR_URL_PARAM_NAME = "error";

    public static final String PROVIDER_URL_PARAM_NAME = "provider";

    public static final String USERINFO_KEY = "OPENID_USERINFO";

    public static final String PROPERTY_OAUTH_CREATE_USER = "nuxeo.oauth.auth.create.user";

    public static final String PROPERTY_SKIP_OAUTH_TOKEN = "nuxeo.skip.oauth.token.state.check";

    protected void sendError(HttpServletRequest req, String msg) {
        req.setAttribute(LOGIN_ERROR, msg);
    }

    public UserIdentificationInfo retrieveIdentityFromOAuth(
            HttpServletRequest req, HttpServletResponse resp) {

        // Getting the "error" URL parameter
        String error = req.getParameter(ERROR_URL_PARAM_NAME);

        // / Checking if there was an error such as the user denied access
        if (error != null && error.length() > 0) {
            sendError(req, "There was an error: \"" + error + "\".");
            return null;
        }

        // Getting the "code" URL parameter
        String code = req.getParameter(CODE_URL_PARAM_NAME);

        // Checking conditions on the "code" URL parameter
        if (code == null || code.isEmpty()) {
            sendError(req, "There was an error: \"" + code + "\".");
            return null;
        }

        // Getting the "provider" URL parameter
        String serviceProviderName = req.getParameter(PROVIDER_URL_PARAM_NAME);

        // Checking conditions on the "provider" URL parameter
        if (serviceProviderName == null || serviceProviderName.isEmpty()) {
            sendError(req, "Missing OpenID Connect Provider ID.");
            return null;
        }

        try {
            OpenIDConnectProviderRegistry registry = Framework.getLocalService(OpenIDConnectProviderRegistry.class);
            OpenIDConnectProvider provider = registry.getProvider(serviceProviderName);

            if (provider == null) {
                sendError(req, "No service provider called: \""
                        + serviceProviderName + "\".");
                return null;
            }

            // Check the state token

            if (!Framework.isBooleanPropertyTrue(PROPERTY_SKIP_OAUTH_TOKEN)
                    && !provider.verifyStateToken(req)) {
                sendError(req, "Invalid state parameter.");
            }

            // Validate the token
            String accessToken = provider.getAccessToken(req, code);

            if (accessToken == null) {
                return null;
            }

            OpenIDUserInfo info = provider.getUserInfo(accessToken);

            // Store the user info as a key in the request so apps can use it
            // later in the chain
            req.setAttribute(USERINFO_KEY, info);

            UserResolver userResolver = provider.getUserResolver();

            String userId;
            if (Framework.isBooleanPropertyTrue(PROPERTY_OAUTH_CREATE_USER)) {
                userId = userResolver.findOrCreateNuxeoUser(info);
            } else {
                userId = userResolver.findNuxeoUser(info);
            }

            if (userId == null) {

                sendError(req, "No user found with email: \"" + info.getEmail()
                        + "\".");
                return null;
            }

            return new UserIdentificationInfo(userId, userId);

        } catch (Exception e) {
            log.error("Error while retrieve Identity From OAuth", e);
        }

        return null;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return new ArrayList<String>();
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String error = httpRequest.getParameter(ERROR_URL_PARAM_NAME);
        String code = httpRequest.getParameter(CODE_URL_PARAM_NAME);
        String serviceProviderName = httpRequest.getParameter(PROVIDER_URL_PARAM_NAME);
        if (serviceProviderName == null) {
            return null;
        }
        if (code == null && error == null) {
            return null;
        }
        UserIdentificationInfo userIdent = retrieveIdentityFromOAuth(
                httpRequest, httpResponse);
        if (userIdent != null) {
            userIdent.setAuthPluginName("TRUSTED_LM");
        }
        return userIdent;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
    }
}
