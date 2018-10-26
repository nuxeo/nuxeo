/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api;

import java.util.List;

/**
 * Service managing the acquisition/release of {@link CoreSession} instances.
 *
 * @since 8.4
 */
public interface CoreSessionService {

    /**
     * Debug information about a {@link CoreSession} acquisition.
     * <p>
     * Since 10.3, we also store the closing stacktrace as a suppressed exception.
     */
    static class CoreSessionRegistrationInfo extends Throwable {

        private static final long serialVersionUID = 1L;

        protected CloseableCoreSession session;

        public CoreSessionRegistrationInfo(CloseableCoreSession session) {
            super("DEBUG: opening stacktrace, sessionId=" + session.getSessionId() + ", thread="
                    + Thread.currentThread().getName());
            this.session = session;
        }

        public CoreSession getCoreSession() {
            return session;
        }
    }

    /**
     * Instantiates a {@link CoreSession}.
     *
     * @param repositoryName the repository name
     * @param principal the principal
     * @return a {@link CloseableCoreSession}
     */
    CloseableCoreSession createCoreSession(String repositoryName, NuxeoPrincipal principal);

    /**
     * Releases (closes) a {@link CloseableCoreSession} acquired by {@link #createCoreSession}.
     *
     * @param session the session to close
     */
    void releaseCoreSession(CloseableCoreSession session);

    /**
     * Gets an existing open session for the given session id.
     * <p>
     * The returned {@link CoreSession} must not be closed, as it is owned by someone else.
     *
     * @param sessionId the session id
     * @return the session, which must not be closed
     */
    CoreSession getCoreSession(String sessionId);

    /**
     * Gets the number of open sessions.
     */
    int getNumberOfOpenCoreSessions();

    /**
     * Gets the debug info for the open sessions.
     *
     * @return a list of debug info
     */
    List<CoreSessionRegistrationInfo> getCoreSessionRegistrationInfos();

}
