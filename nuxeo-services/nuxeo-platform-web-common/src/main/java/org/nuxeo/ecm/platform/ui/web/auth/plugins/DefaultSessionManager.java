/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;

public class DefaultSessionManager implements NuxeoAuthenticationSessionManager {

    public boolean canBypassRequest(ServletRequest request) {
        return false;
    }

    public void onBeforeSessionInvalidate(ServletRequest request) {
        // NOP
    }

    public void onAfterSessionReinit(ServletRequest request) {
        // NOP
    }

    public void onAuthenticatedSessionCreated(ServletRequest request,
            HttpSession session, CachableUserIdentificationInfo cachableUserInfo) {
        // NOP
    }

    public void onBeforeSessionReinit(ServletRequest request) {
        // NOP
    }

    public boolean needResetLogin(ServletRequest req) {
        return false;
    }

}
