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

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.*;

public class FormAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(FormAuthenticator.class);

    protected String loginPage = "login.jsp";

    protected String usernameKey = USERNAME_KEY;

    protected String passwordKey = PASSWORD_KEY;

    protected String getLoginPage() {
        return loginPage;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        try {
            log.debug("Forward to Login Screen");
            Map<String, String> parameters = new HashMap<String, String>();
            String redirectUrl = baseURL + getLoginPage();
            @SuppressWarnings("unchecked")
            Enumeration<String> paramNames = httpRequest.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String name = paramNames.nextElement();
                String value = httpRequest.getParameter(name);
                parameters.put(name, value);
            }
            HttpSession session = httpRequest.getSession(false);
            String requestedUrl = null;
            if (session != null) {
                requestedUrl = (String) session.getAttribute(
                        START_PAGE_SAVE_KEY);
            }
            if (requestedUrl != null && !requestedUrl.equals("")) {
                parameters.put(REQUESTED_URL,
                        URLEncoder.encode(requestedUrl, "UTF-8"));
            }
            String loginError = (String) httpRequest.getAttribute(LOGIN_ERROR);
            if (loginError != null) {
                if (ERROR_USERNAME_MISSING.equals(loginError)) {
                    parameters.put(LOGIN_MISSING, "true");
                } else if (ERROR_CONNECTION_FAILED.equals(loginError)) {
                    parameters.put(LOGIN_CONNECTION_FAILED, "true");
                    parameters.put(LOGIN_FAILED, "true"); // compat
                } else {
                    parameters.put(LOGIN_FAILED, "true");
                }
            }
            // avoid resending the password in clear !!!
            parameters.remove(passwordKey);
            redirectUrl = URIUtils.addParametersToURIQuery(redirectUrl,
                    parameters);
            httpResponse.sendRedirect(redirectUrl);
        } catch (IOException e) {
            log.error(e, e);
            return false;
        }
        return true;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        log.debug("Looking for user/password in the request");
        String userName = httpRequest.getParameter(usernameKey);
        String password = httpRequest.getParameter(passwordKey);
        // NXP-2650: ugly hack to check if form was submitted
        if (httpRequest.getParameter(FORM_SUBMITTED_MARKER) != null
                && (userName == null || userName.length() == 0)) {
            httpRequest.setAttribute(LOGIN_ERROR, ERROR_USERNAME_MISSING);
        }
        if (userName==null  || userName.length() == 0 ) {
            return null;
        }
        return new UserIdentificationInfo(userName, password);
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.get("LoginPage") != null) {
            loginPage = parameters.get("LoginPage");
        }
        if (parameters.get("UsernameKey") != null) {
            usernameKey = parameters.get("UsernameKey");
        }
        if (parameters.get("PasswordKey") != null) {
            passwordKey = parameters.get("PasswordKey");
        }
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        // Login Page is unauthenticated !
        List<String> prefix = new ArrayList<String>();
        prefix.add(getLoginPage());
        return prefix;
    }

}
