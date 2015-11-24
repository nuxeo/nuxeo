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
package org.nuxeo.ecm.tokenauth.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.tokenauth.TokenAuthenticationException;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet that allows to get a unique authentication token given the request
 * Principal and some device information passed as request parameters:
 * application name, device id, device description, permission. An error
 * response will be sent with a 400 status code if one of the required
 * parameters is null or empty. All parameters are required except for the
 * device description.
 * <p>
 * The token is provided by the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationServlet extends HttpServlet {

    private static final long serialVersionUID = 7792388601558509103L;

    private static final Log log = LogFactory.getLog(TokenAuthenticationServlet.class);

    protected static final String APPLICATION_NAME_PARAM = "applicationName";

    protected static final String DEVICE_ID_PARAM = "deviceId";

    protected static final String DEVICE_DESCRIPTION_PARAM = "deviceDescription";

    protected static final String PERMISSION_PARAM = "permission";

    protected static final String REVOKE_PARAM = "revoke";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

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
                && (StringUtils.isEmpty(applicationName)
                        || StringUtils.isEmpty(deviceId) || StringUtils.isEmpty(permission))) {
            log.error("The following request parameters are mandatory to acquire an authentication token: applicationName, deviceId, permission.");
            resp.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        if (revoke
                && (StringUtils.isEmpty(applicationName) || StringUtils.isEmpty(deviceId))) {
            log.error("The following request parameters are mandatory to revoke an authentication token: applicationName, deviceId.");
            resp.sendError(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        // Decode parameters
        applicationName = URIUtil.decode(applicationName);
        deviceId = URIUtil.decode(deviceId);
        if (!StringUtils.isEmpty(deviceDescription)) {
            deviceDescription = URIUtil.decode(deviceDescription);
        }
        if (!StringUtils.isEmpty(permission)) {
            permission = URIUtil.decode(permission);
        }

        // Get user name from request Principal
        Principal principal = req.getUserPrincipal();
        if (principal == null) {
            resp.sendError(HttpStatus.SC_UNAUTHORIZED);
            return;
        }
        String userName = principal.getName();

        // Write response
        String response = null;
        int statusCode;
        TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
        try {
            // Token acquisition: acquire token and write it to the response
            // body
            if (!revoke) {
                response = tokenAuthService.acquireToken(userName,
                        applicationName, deviceId, deviceDescription,
                        permission);
                statusCode = 201;
            }
            // Token revocation
            else {
                String token = tokenAuthService.getToken(userName,
                        applicationName, deviceId);
                if (token == null) {
                    response = String.format(
                            "No token found for userName %s, applicationName %s and deviceId %s; nothing to do.",
                            userName, applicationName, deviceId);
                    statusCode = 400;
                } else {
                    tokenAuthService.revokeToken(token);
                    response = String.format(
                            "Token revoked for userName %s, applicationName %s and deviceId %s.",
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

    protected void sendTextResponse(HttpServletResponse resp,
            String textResponse, int statusCode) throws IOException {

        resp.setContentType("text/plain");
        resp.setStatus(statusCode);
        resp.setContentLength(textResponse.getBytes().length);
        OutputStream out = resp.getOutputStream();
        out.write(textResponse.getBytes());
        out.close();
    }

}
