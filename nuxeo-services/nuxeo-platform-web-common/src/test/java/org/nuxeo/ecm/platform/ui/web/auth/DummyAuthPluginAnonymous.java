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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;

/**
 * Dummy authentication plugin that identifies everyone as {@code DummyAnonyous}.
 *
 * @since 10.2
 */
public class DummyAuthPluginAnonymous implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension {

    public static final String DUMMY_ANONYMOUS_LOGIN = "DummyAnonymous";

    @Override
    public void initPlugin(Map<String, String> parameters) {
        // nothing to do
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {
        return new UserIdentificationInfo(DUMMY_ANONYMOUS_LOGIN, DUMMY_ANONYMOUS_LOGIN);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest request) {
        return FALSE;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {
        return FALSE;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        return FALSE;
    }

}
