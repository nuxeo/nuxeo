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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles authentication with a token sent as a request header.
 * <p>
 * The user is retrieved with the {@link TokenAuthenticationService}.
 * <p>
 * This Authentication Plugin is configured to be used with the Trusting_LM
 * {@link LoginModule} plugin => no password check will be done, a principal
 * will be created from the userName if the user exists in the user directory.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(TokenAuthenticator.class);

    protected static final String TOKEN_HEADER = "X-Authentication-Token";

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String token = httpRequest.getHeader(TOKEN_HEADER);

        if (token == null) {
            log.debug(String.format("Found no '%s' header in the request.",
                    TOKEN_HEADER));
            return null;
        }

        String userName = getUserByToken(token);
        if (userName == null) {
            log.error(String.format(
                    "No user bound to the token '%s' (maybe it has been revoked), returning null.",
                    token));
            return null;
        } else {
            return new UserIdentificationInfo(userName, userName);
        }
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

    public void initPlugin(Map<String, String> parameters) {
        // Nothing to do as the authenticationPlugin contribution has no
        // parameters
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    protected String getUserByToken(String token) {

        TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
        return tokenAuthService.getUserName(token);
    }

}
