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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.START_PAGE_SAVE_KEY;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;

/**
 * Dummy authentication plugin that has redirection to a form-based login page.
 *
 * @since 10.2
 */
public class DummyAuthPluginForm implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension {

    public static final String DUMMY_AUTH_FORM_USERNAME_KEY = "username";

    public static final String DUMMY_AUTH_FORM_PASSWORD_KEY = "password";

    public static final String LOGIN_PAGE = "dummy_login.jsp";

    @Override
    public void initPlugin(Map<String, String> parameters) {
        // nothing to do
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter(DUMMY_AUTH_FORM_USERNAME_KEY);
        String password = request.getParameter(DUMMY_AUTH_FORM_PASSWORD_KEY);
        if (!checkUsernamePassword(username, password)) {
            return null;
        }
        return new UserIdentificationInfo(username, password);
    }

    /** dummy check: username = password to authenticate */
    protected boolean checkUsernamePassword(String username, String password) {
        return isNotBlank(username) && username.equals(password);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest request) {
        return TRUE;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {
        String url = baseURL + LOGIN_PAGE;
        HttpSession session = request.getSession(false);
        if (session != null) {
            String requestedUrl = (String) session.getAttribute(START_PAGE_SAVE_KEY);
            if (isNotBlank(requestedUrl)) {
                url += "?" + REQUESTED_URL + '=' + requestedUrl;
            }
        }
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return TRUE;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return Arrays.asList("dummy_form_login.jsp");
    }

    @Override
    public Boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        // no cookies to remove
        return FALSE; // did not redirect
    }

}
