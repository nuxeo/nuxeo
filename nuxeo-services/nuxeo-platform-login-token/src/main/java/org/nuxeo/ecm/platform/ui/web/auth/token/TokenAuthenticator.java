/*
 * (C) Copyright 2006-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.ui.web.auth.token;

import java.util.List;
import java.util.Map;

import javax.security.auth.spi.LoginModule;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles authentication with a token sent as a request header.
 * <p>
 * The user is retrieved with the {@link TokenAuthenticationService}.
 * <p>
 * This Authentication Plugin is configured to be used with the Trusting_LM {@link LoginModule} plugin => no password
 * check will be done, a principal will be created from the userName if the user exists in the user directory.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticator implements NuxeoAuthenticationPlugin {

    public static final String ALLOW_ANONYMOUS_KEY = "allowAnonymous";

    private static final String HTTPS = "https";

    private static final String LOCALHOST = "localhost";

    private static final Log log = LogFactory.getLog(TokenAuthenticator.class);

    protected static final String TOKEN_HEADER = "X-Authentication-Token";

    protected static final String TOKEN_PARAM = "token";

    protected boolean allowAnonymous = false;

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String token = getTokenFromRequest(httpRequest);

        if (token == null) {
            log.debug(String.format("Found no '%s' header in the request.", TOKEN_HEADER));
            return null;
        }

        String userName = getUserByToken(token);
        if (userName == null) {
            log.debug(String.format("No user bound to the token '%s' (maybe it has been revoked), returning null.",
                    token));
            return null;
        }
        // Don't retrieve identity for anonymous user unless 'allowAnonymous' parameter is explicitly set to true in
        // the authentication plugin configuration
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager != null && userName.equals(userManager.getAnonymousUserId()) && !allowAnonymous) {
            log.debug("Anonymous user is not allowed to get authenticated by token, returning null.");
            return null;
        }
        return new UserIdentificationInfo(userName, userName);
    }

    /**
     * Gets the token from the request if present else null.
     *
     * @param httpRequest
     * @return
     * @since 5.9.2
     */
    private String getTokenFromRequest(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(TOKEN_HEADER);

        if (token == null) {
            token = httpRequest.getParameter(TOKEN_PARAM);
        }

        // If we don't find the token in request header, let's check in cookies
        if (token == null && httpRequest.getCookies() != null) {
            Cookie cookie = getTokenCookie(httpRequest);
            if (cookie != null && isAllowedToUseCookieToken(httpRequest)) {
                return cookie.getValue();
            }
        }
        return token;
    }

    /**
     * Returns the token from the cookies if found else null.
     *
     * @param httpRequest
     * @return
     * @since 5.9.2
     */
    private Cookie getTokenCookie(HttpServletRequest httpRequest) {
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(TOKEN_HEADER) && isAllowedToUseCookieToken(httpRequest)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * Guard the use of cookie token to htpps only or localhost.
     *
     * @param httpRequest
     * @return
     * @since 5.9.2
     */
    private boolean isAllowedToUseCookieToken(HttpServletRequest req) {
        if (LOCALHOST.equals(req.getServerName())) {
            return true;
        }
        return HTTPS.equals(req.getScheme());
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(ALLOW_ANONYMOUS_KEY)) {
            allowAnonymous = Boolean.valueOf(parameters.get(ALLOW_ANONYMOUS_KEY));
        }
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    protected String getUserByToken(String token) {

        TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
        return tokenAuthService.getUserName(token);
    }

}
