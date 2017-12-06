/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth.servlet;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator;
import org.nuxeo.ecm.tokenauth.TokenAuthenticationException;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet that allows to get a unique authentication token given the request Principal and some device information
 * passed as request parameters: application name, device id, device description, permission. An error response will be
 * sent with a 400 status code if one of the required parameters is null or empty. All parameters are required except
 * for the device description.
 * <p>
 * The token is provided by the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationServlet extends HttpServlet {

    private static final long serialVersionUID = 7792388601558509103L;

    private static final Log log = LogFactory.getLog(TokenAuthenticationServlet.class);

    public static final String TOKEN_AUTH_PLUGIN_NAME = "TOKEN_AUTH";

    public static final String APPLICATION_NAME_PARAM = "applicationName";

    public static final String DEVICE_ID_PARAM = "deviceId";

    public static final String DEVICE_DESCRIPTION_PARAM = "deviceDescription";

    public static final String PERMISSION_PARAM = "permission";

    public static final String REVOKE_PARAM = "revoke";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Don't provide token for anonymous user unless 'allowAnonymous' parameter is explicitly set to true in
        // the authentication plugin configuration
        Principal principal = req.getUserPrincipal();
        if (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAnonymous()) {
            PluggableAuthenticationService authenticationService = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                    PluggableAuthenticationService.NAME);
            AuthenticationPluginDescriptor tokenAuthPluginDesc = authenticationService.getDescriptor(TOKEN_AUTH_PLUGIN_NAME);
            if (tokenAuthPluginDesc == null
                    || !(Boolean.valueOf(tokenAuthPluginDesc.getParameters().get(TokenAuthenticator.ALLOW_ANONYMOUS_KEY)))) {
                log.debug("Anonymous user is not allowed to acquire an authentication token.");
                resp.sendError(HttpStatus.SC_UNAUTHORIZED);
                return;
            }

        }

        // Get request parameters
        String applicationName = req.getParameter(APPLICATION_NAME_PARAM);
        String deviceId = req.getParameter(DEVICE_ID_PARAM);
        String deviceDescription = req.getParameter(DEVICE_DESCRIPTION_PARAM);
        String permission = req.getParameter(PERMISSION_PARAM);
        String revokeParam = req.getParameter(REVOKE_PARAM);
        boolean revoke = Boolean.valueOf(revokeParam);

        // If one of the required parameters is null or empty, send an
        // error with the 400 status
        if (!revoke
                && (StringUtils.isEmpty(applicationName) || StringUtils.isEmpty(deviceId) || StringUtils.isEmpty(permission))) {
            log.error("The following request parameters are mandatory to acquire an authentication token: applicationName, deviceId, permission.");
            resp.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        if (revoke && (StringUtils.isEmpty(applicationName) || StringUtils.isEmpty(deviceId))) {
            log.error("The following request parameters are mandatory to revoke an authentication token: applicationName, deviceId.");
            resp.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        // Get user name from request Principal
        if (principal == null) {
            resp.sendError(HttpStatus.SC_UNAUTHORIZED);
            return;
        }
        String userName = principal.getName();

        // Write response
        String response = null;
        int statusCode;
        TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
        try {
            // Token acquisition: acquire token and write it to the response
            // body
            if (!revoke) {
                response = tokenAuthService.acquireToken(userName, applicationName, deviceId, deviceDescription,
                        permission);
                statusCode = 201;
            }
            // Token revocation
            else {
                String token = tokenAuthService.getToken(userName, applicationName, deviceId);
                if (token == null) {
                    response = String.format(
                            "No token found for userName %s, applicationName %s and deviceId %s; nothing to do.",
                            userName, applicationName, deviceId);
                    statusCode = 400;
                } else {
                    tokenAuthService.revokeToken(token);
                    response = String.format("Token revoked for userName %s, applicationName %s and deviceId %s.",
                            userName, applicationName, deviceId);
                    statusCode = 202;
                }
            }
            sendTextResponse(resp, response, statusCode);
        } catch (TokenAuthenticationException e) {
            // Should never happen as parameters have already been checked
            resp.sendError(HttpStatus.SC_NOT_FOUND);
        }
    }

    protected void sendTextResponse(HttpServletResponse resp, String textResponse, int statusCode) throws IOException {

        resp.setContentType("text/plain");
        resp.setCharacterEncoding(UTF_8.name());
        resp.setStatus(statusCode);
        resp.setContentLength(textResponse.getBytes().length);
        OutputStream out = resp.getOutputStream();
        out.write(textResponse.getBytes(UTF_8));
        out.close();
    }

}
