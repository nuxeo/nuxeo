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
 */

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;

public class DefaultSessionManager implements NuxeoAuthenticationSessionManager {

    @Override
    public boolean canBypassRequest(ServletRequest request) {
        return false;
    }

    @Override
    public boolean invalidateSession(ServletRequest request) {
        // NOP
        return false;
    }

    @Override
    public void onAfterSessionReinit(ServletRequest request) {
        // NOP
    }

    @Override
    public void onAuthenticatedSessionCreated(ServletRequest request, HttpSession session,
            CachableUserIdentificationInfo cachableUserInfo) {
        // NOP
    }

    @Override
    public void onBeforeSessionReinit(ServletRequest request) {
        // NOP
    }

    @Override
    public boolean needResetLogin(ServletRequest req) {
        return false;
    }

}
