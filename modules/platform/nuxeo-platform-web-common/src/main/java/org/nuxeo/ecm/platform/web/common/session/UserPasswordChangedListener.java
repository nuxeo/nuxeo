/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <thomas.roger@hyland.com>
 */
package org.nuxeo.ecm.platform.web.common.session;

import static org.nuxeo.ecm.platform.usermanager.UserManager.USER_HTTP_SESSION_ID_KEY;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

/**
 * Synchronous listener that invalidates all HTTP sessions for a user when the user's password is changed.
 * <p>
 * The current session (the one that initiated the password change) is preserved by reading the session ID from the
 * event context property {@link org.nuxeo.ecm.platform.usermanager.UserManager#USER_HTTP_SESSION_ID_KEY}. Callers
 * should set this property on the user
 * {@link org.nuxeo.ecm.core.api.DocumentModel#putContextData(String, java.io.Serializable) DocumentModel} before
 * updating the password.
 *
 * @since 2025.18
 */
public class UserPasswordChangedListener implements EventListener {

    private static final Logger log = LogManager.getLogger(UserPasswordChangedListener.class);

    protected static final String ID_PROPERTY_KEY = "id";

    @Override
    public void handleEvent(Event event) {
        var ctx = event.getContext();
        var userName = (String) ctx.getProperty(ID_PROPERTY_KEY);
        if (userName == null) {
            return;
        }

        var excludeSessionId = (String) ctx.getProperty(USER_HTTP_SESSION_ID_KEY);
        log.debug("Password changed for user: {}, invalidating other HTTP sessions", userName);
        NuxeoHttpSessionMonitor.instance().invalidateSessionsForUser(userName, excludeSessionId);
    }
}
