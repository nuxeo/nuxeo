/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.auth;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.LoginResponseHandler;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Dummy authentication plugin that simulates delegation to an external SSO.
 *
 * @since 10.2
 */
public class DummyAuthPluginSSO
        implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension, LoginResponseHandler {

    public static final String DUMMY_SSO_LOGIN_URL = "http://sso.example.com/login";

    public static final String DUMMY_SSO_LOGOUT_URL = "http://sso.example.com/logout";

    public static final String DUMMY_SSO_ERROR_URL = "http://sso.example.com/error";

    public static final String DUMMY_SSO_TICKET = "ticket";

    public static final String LOGIN_PAGE = "dummy_login.jsp";

    @Override
    public void initPlugin(Map<String, String> parameters) {
        // nothing to do
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {
        // a real plugin would check the state token
        String ticket = request.getParameter(DUMMY_SSO_TICKET);
        String username = checkUsernameFromTicket(ticket);
        if (username == null) {
            return null;
        }
        return new UserIdentificationInfo(username, username);
    }

    /** dummy check: any non-null ticket is accepted */
    protected String checkUsernameFromTicket(String ticket) {
        if (isBlank(ticket)) {
            return null;
        }
        // a real plugin would verify the ticket against a remote server
        String username = ticket; // dummy implementation
        return username;
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest request) {
        return TRUE;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {
        // a real plugin would also store a state token in the session and pass it to the SSO login page
        // then expect to receive it back
        try {
            String url = getRequestedUrl(request);
            String ssoUrl = DUMMY_SSO_LOGIN_URL + "?redirect=" + URLEncoder.encode(url, "UTF-8");
            response.sendRedirect(ssoUrl);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return TRUE;
    }

    protected String getRequestedUrl(HttpServletRequest request) {
        String url = VirtualHostHelper.getServerURL(request) + request.getRequestURI();
        if (request.getQueryString() != null) {
            url += '?' + request.getQueryString();
        }
        return url;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        String url = DUMMY_SSO_LOGOUT_URL;
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return TRUE;
    }

    @Override
    public boolean onError(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.sendRedirect(DUMMY_SSO_ERROR_URL);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return true;
    }

    @Override
    public boolean onSuccess(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

}
