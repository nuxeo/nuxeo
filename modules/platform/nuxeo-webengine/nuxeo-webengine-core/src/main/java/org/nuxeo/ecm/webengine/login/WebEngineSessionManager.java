/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.webengine.login;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

public class WebEngineSessionManager extends DefaultSessionManager {

    // TODO work on skin request to avoid hardcoding paths
    private static final String RESOURCES_PATH = VirtualHostHelper.getContextPathProperty() + "/site/files/";

    private static final Log log = LogFactory.getLog(WebEngineSessionManager.class);

    @Override
    public boolean canBypassRequest(ServletRequest request) {
        // static resources don't require Authentication
        return ((HttpServletRequest) request).getRequestURI().startsWith(RESOURCES_PATH);
    }

    @Override
    public void onAuthenticatedSessionCreated(ServletRequest request, HttpSession httpSession,
            CachableUserIdentificationInfo cachableUserInfo) {

        // do nothing
    }

    @Override
    public boolean needResetLogin(ServletRequest req) {
        return WebEngineFormAuthenticator.isLoginRequest((HttpServletRequest) req);
    }

}
