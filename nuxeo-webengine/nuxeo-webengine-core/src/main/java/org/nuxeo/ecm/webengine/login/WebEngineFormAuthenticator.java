/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webengine.login;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.LoginResponseHandler;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class WebEngineFormAuthenticator implements NuxeoAuthenticationPlugin, LoginResponseHandler {

    private static final Log log = LogFactory.getLog(WebEngineFormAuthenticator.class);

    protected static String usernameKey = "username";

    protected static String passwordKey = "password";

    public static final String LOGIN_KEY = "/@@login";


    public Boolean handleLoginPrompt(HttpServletRequest request,
            HttpServletResponse response, String baseURL) {
        return false; // TODO doesn't have a login page ?
    }

    /**
     * Gets the path info to be used to redirect after login.
     */
    protected String getLoginPathInfo(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null) {
            if (path.endsWith(LOGIN_KEY)) {
                return path.substring(0, path.length()-LOGIN_KEY.length());
            }
        }
        return null;
    }

    public static boolean isLoginRequest(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path != null) {
            if (path.endsWith(LOGIN_KEY)) {
                return true;
            }
        }
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (!isLoginRequest(httpRequest)) {
            return null;
        }
        String userName = httpRequest.getParameter(usernameKey);
        String password = httpRequest.getParameter(passwordKey);
        return new UserIdentificationInfo(userName, password);
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.get("UsernameKey") != null) {
            usernameKey = parameters.get("UsernameKey");
        }
        if (parameters.get("PasswordKey") != null) {
            passwordKey = parameters.get("PasswordKey");
        }
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return Collections.emptyList();
    }

    public boolean onError(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String path = getLoginPathInfo(request);
            if (path == null) { // this should never happens
                return false;
            }
            // ajax request
            if (request.getParameter("caller") != null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed");
            } else { // normal request
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.sendRedirect(path+"?failed=true");
            }
        } catch (Exception e) {
            log.error(e);
            return false;
        }
        return true;
    }

    public boolean onSuccess(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String path = getLoginPathInfo(request);
            if (path == null) { // this should never happens
                return false;
            }
            // ajax request
            if (request.getParameter("caller") != null) {
                response.sendError(HttpServletResponse.SC_OK);
            } else { // normal request
                response.sendRedirect(path);
            }
        } catch (Exception e) {
            log.error(e);
            return false;
        }
        return true;
    }

}
