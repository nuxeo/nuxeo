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

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;

/**
 * SessionManager interface for Authentication Filter.
 *
 * @author tiry
 */
public interface NuxeoAuthenticationSessionManager {

    /**
     * Checks whether or not this request was made to perform login. This is tested by the authentication filter to
     * decide if a switch user is needed.
     */
    boolean needResetLogin(ServletRequest req);

    /**
     * Returns true if request does not require to be authenticated.
     */
    boolean canBypassRequest(ServletRequest request);

    /**
     * May invalidates the session. Return true is the session was invalidated.
     */
    boolean invalidateSession(ServletRequest request);

    /**
     * CallBack before SessionReinit.
     */
    void onBeforeSessionReinit(ServletRequest request);

    /**
     * CallBack after SessionReinit.
     */
    void onAfterSessionReinit(ServletRequest request);

    /**
     * CallBack for session creation
     */
    void onAuthenticatedSessionCreated(ServletRequest request, HttpSession session,
            CachableUserIdentificationInfo cachebleUserInfo);

}
