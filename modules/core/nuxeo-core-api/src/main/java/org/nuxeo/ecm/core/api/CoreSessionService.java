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

/**
 * Service managing the creation of {@link CoreSession} instances.
 *
 * @since 8.4
 */
public interface CoreSessionService {

    /**
     * Instantiates a {@link CoreSession}.
     *
     * @param repositoryName the repository name
     * @param principal the principal
     * @return a {@link CloseableCoreSession}
     */
    CloseableCoreSession createCoreSession(String repositoryName, NuxeoPrincipal principal);

    /**
     * Does nothing.
     *
     * @param session the session to close
     * @deprecated since 11.1, does nothing
     */
    @Deprecated
    void releaseCoreSession(CloseableCoreSession session);

    /**
     * Gets the number of open sessions.
     */
    int getNumberOfOpenCoreSessions();

}
