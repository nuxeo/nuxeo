/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: ClearTrustAuthenticator.java 33212 2009-04-22 14:06:56Z madarche $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cleartrust;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;

/**
 * @author M.-A. Darche
 */
public class ClearTrustAuthenticator implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension {

    protected static final String CLEARTRUST_HEADER_UID = "REMOTE_USER";

    protected static final String CLEARTRUST_COOKIE_SESSION_A = "ACTSESSION";

    protected static final String CLEARTRUST_COOKIE_SESSION = "CTSESSION";

    protected String cookieDomain = "";

    protected String cleartrustLoginUrl = "";

    protected String cleartrustLogoutUrl = "";

    private static final Log log = LogFactory.getLog(ClearTrustAuthenticator.class);

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        // There isn't any URL that should not need authentication
        return null;
    }

    /**
     * Redirects to the ClearTrust login page if the request doesn't contain cookies indicating that a positive
     * authentication occurred.
     *
     * @return true if AuthFilter must stop execution (ie: login prompt generated a redirect), false otherwise
     */
    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {
        log.debug("handleLoginPrompt ...");
        log.debug("handleLoginPrompt requestURL = " + request.getRequestURL());
        Cookie[] cookies = getCookies(request);
        displayRequestInformation(request);
        displayCookieInformation(cookies);
        String ctSession = getCookieValue(CLEARTRUST_COOKIE_SESSION, cookies);
        String ctSessionA = getCookieValue(CLEARTRUST_COOKIE_SESSION_A, cookies);
        log.debug("ctSession = " + ctSession);
        log.debug("ctSessionA = " + ctSessionA);

        boolean redirectToClearTrustLoginPage = false;
        if (ctSession == null) {
            log.debug("No ClearTrust session: not authorizing + redirecting to ClearTrust");
            redirectToClearTrustLoginPage = true;
        }

        if ("%20".equals(ctSessionA)) {
            log.debug("User has logout from ClearTrust: not authorizing + redirecting to ClearTrust");
            redirectToClearTrustLoginPage = true;
        }

        String ctUid = request.getHeader(CLEARTRUST_HEADER_UID);
        log.debug("ctUid = [" + ctUid + "]");
        if (ctUid == null) {
            redirectToClearTrustLoginPage = true;
        }

        if (redirectToClearTrustLoginPage) {
            String loginUrl = cleartrustLoginUrl;
            try {
                if (cleartrustLoginUrl == null || "".equals(cleartrustLoginUrl)) {
                    // loginUrl = baseURL
                    // + LoginScreenHelper.getStartupPagePath();
                    loginUrl = baseURL + "login.jsp";
                }
                log.debug("Redirecting to loginUrl: " + loginUrl);
                response.sendRedirect(loginUrl);
                return true;
            } catch (IOException ex) {
                log.error("Unable to redirect to ClearTrust login URL [" + loginUrl + "]:", ex);
                return false;
            }
        }
        log.debug("ClearTrust authentication is OK, letting the user in.");
        return false;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse httpResponse) {
        log.debug("handleRetrieveIdentity ...");
        Cookie[] cookies = getCookies(request);
        displayRequestInformation(request);
        displayCookieInformation(cookies);

        String ctUid = request.getHeader(CLEARTRUST_HEADER_UID);
        log.debug("handleRetrieveIdentity ctUid = [" + ctUid + "]");
        String userName = ctUid;
        UserIdentificationInfo uui = new UserIdentificationInfo(userName,
                "No password needed for ClearTrust authentication");
        log.debug("handleRetrieveIdentity going on with authenticated user = [" + userName + "]");
        return uui;
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest request) {
        // Returning true means that the handleLoginPrompt method will be called
        return true;
    }

    /**
     * @return true if there is a redirection
     */
    @Override
    public Boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        log.debug("handleLogout ...");
        expireCookie(CLEARTRUST_COOKIE_SESSION, request, response);
        expireCookie(CLEARTRUST_COOKIE_SESSION_A, request, response);

        if (cleartrustLogoutUrl == null || "".equals(cleartrustLogoutUrl)) {
            return false;
        }

        try {
            log.debug("Redirecting to logoutUrl = [" + cleartrustLogoutUrl + "] ...");
            response.sendRedirect(cleartrustLogoutUrl);
            log.debug("handleLogout DONE!");
            return true;
        } catch (IOException e) {
            log.error("Unable to redirect to the logout URL [" + cleartrustLogoutUrl + "] :", e);
            return false;
        }
    }

    protected Cookie[] getCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            cookies = new Cookie[0];
        }
        return cookies;
    }

    private String getCookieValue(String cookieName, Cookie[] cookies) {
        String cookieValue = null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                cookieValue = cookie.getValue();
            }
        }
        return cookieValue;
    }

    private void expireCookie(String cookieName, HttpServletRequest request, HttpServletResponse response) {
        log.debug("expiring cookie [" + cookieName + "]  ...");
        Cookie cookie = new Cookie(cookieName, "");
        // A zero value causes the cookie to be deleted
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    protected void displayCookieInformation(Cookie[] cookies) {
        log.debug(">>>>>>>>>>>>> Here are the cookies: ");
        for (Cookie cookie : cookies) {
            log.debug("displayCookieInformation cookie name: [" + cookie.getName() + "] path: [" + cookie.getPath()
                    + "] domain: " + cookie.getDomain() + " max age: " + cookie.getMaxAge() + " value: ["
                    + cookie.getValue() + "]");
        }
    }

    protected void displayRequestInformation(HttpServletRequest request) {
        log.debug(">>>>>>>>>>>>> Here is the request: ");
        for (Enumeration headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
            String headerName = (String) headerNames.nextElement();
            log.debug("header " + headerName + " : [" + request.getHeader(headerName) + "]");
        }
        for (Enumeration attributeNames = request.getAttributeNames(); attributeNames.hasMoreElements();) {
            String attributeName = (String) attributeNames.nextElement();
            log.debug("attribute " + attributeName + " : [" + request.getAttribute(attributeName) + "]");
        }
        for (Enumeration parameterNames = request.getParameterNames(); parameterNames.hasMoreElements();) {
            String parameterName = (String) parameterNames.nextElement();
            log.debug("parameter " + parameterName + " : [" + request.getParameter(parameterName) + "]");
        }
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        log.debug("initPlugin v 1.1");
        if (parameters.containsKey(ClearTrustParameters.COOKIE_DOMAIN)) {
            cookieDomain = parameters.get(ClearTrustParameters.COOKIE_DOMAIN);
            log.debug("initPlugin cookieDomain = [" + cookieDomain + "]");
        }
        if (parameters.containsKey(ClearTrustParameters.CLEARTRUST_LOGIN_URL)) {
            cleartrustLoginUrl = parameters.get(ClearTrustParameters.CLEARTRUST_LOGIN_URL);
            log.debug("initPlugin cleartrustLoginUrl = [" + cleartrustLoginUrl + "]");
        }
        if (parameters.containsKey(ClearTrustParameters.CLEARTRUST_LOGOUT_URL)) {
            cleartrustLogoutUrl = parameters.get(ClearTrustParameters.CLEARTRUST_LOGOUT_URL);
            log.debug("initPlugin cleartrustLogoutUrl = [" + cleartrustLogoutUrl + "]");
        }
        log.debug("initPlugin DONE");
    }

}
