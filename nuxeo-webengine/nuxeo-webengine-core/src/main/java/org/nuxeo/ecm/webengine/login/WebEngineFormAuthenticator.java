/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webengine.login;

import java.io.IOException;
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

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {
        return false; // TODO doesn't have a login page ?
    }

    /**
     * Gets the path info to be used to redirect after login.
     */
    protected String getLoginPathInfo(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null) {
            if (path.endsWith(LOGIN_KEY)) {
                return path.substring(0, path.length() - LOGIN_KEY.length());
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

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        // Only accept POST requests
        String method = httpRequest.getMethod();
        if (!"POST".equals(method)) {
            log.debug("Request method is " + method + ", only accepting POST");
            return null;
        }
        if (!isLoginRequest(httpRequest)) {
            return null;
        }
        String userName = httpRequest.getParameter(usernameKey);
        String password = httpRequest.getParameter(passwordKey);
        return new UserIdentificationInfo(userName, password);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        if (parameters.get("UsernameKey") != null) {
            usernameKey = parameters.get("UsernameKey");
        }
        if (parameters.get("PasswordKey") != null) {
            passwordKey = parameters.get("PasswordKey");
        }
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return Collections.emptyList();
    }

    @Override
    public boolean onError(HttpServletRequest request, HttpServletResponse response) {
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
                response.sendRedirect(path + "?failed=true");
            }
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean onSuccess(HttpServletRequest request, HttpServletResponse response) {
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
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }

}
