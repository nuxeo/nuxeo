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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet that allows to get a unique authentication token given some user
 * information passed as request parameters.
 * <p>
 * The token is provided by the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationServlet extends HttpServlet {

    private static final long serialVersionUID = 7792388601558509103L;

    protected static final String USERNAME_PARAM = "userName";

    protected static final String APPLICATION_NAME_PARAM = "applicationName";

    protected static final String DEVICE_NAME_PARAM = "deviceName";

    protected static final String PERMISSION_PARAM = "permission";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String userName = req.getParameter(USERNAME_PARAM);
        String applicationName = req.getParameter(APPLICATION_NAME_PARAM);
        String deviceName = req.getParameter(DEVICE_NAME_PARAM);
        String permission = req.getParameter(PERMISSION_PARAM);

        TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
        String token = tokenAuthService.getToken(userName, applicationName,
                deviceName, permission);
        sendTextResponse(resp, token);
    }

    protected void sendTextResponse(HttpServletResponse resp,
            String textResponse) throws IOException {

        resp.setContentType("text/plain");
        resp.setContentLength(textResponse.getBytes().length);
        OutputStream out = resp.getOutputStream();
        out.write(textResponse.getBytes());
        out.close();
    }

}
