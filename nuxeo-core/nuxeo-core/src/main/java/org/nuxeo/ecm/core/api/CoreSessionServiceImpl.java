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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Implementation for the service managing the acquisition/release of {@link CoreSession} instances.
 *
 * @since 8.4
 */
public class CoreSessionServiceImpl extends DefaultComponent implements CoreSessionService {

    private static final Logger log = LogManager.getLogger(CoreSessionServiceImpl.class);

    /**
     * All open {@link CoreSessionRegistrationInfo}, keyed by session id.
     */
    private final Map<String, CoreSessionRegistrationInfo> sessions = new ConcurrentHashMap<String, CoreSessionRegistrationInfo>();

    /**
     * Most recently closed sessions.
     */
    protected final Cache<String, CoreSessionRegistrationInfo> recentlyClosedSessions = //
            CacheBuilder.newBuilder().maximumSize(100).build();

    @Override
    public CloseableCoreSession createCoreSession(String repositoryName, NuxeoPrincipal principal) {
        LocalSession session = new LocalSession(repositoryName, principal);
        sessions.put(session.getSessionId(), new CoreSessionRegistrationInfo(session));
        return session;
    }

    @Override
    public void releaseCoreSession(CloseableCoreSession session) {
        String sessionId = session.getSessionId();
        CoreSessionRegistrationInfo info = sessions.remove(sessionId);
        String debug = "closing stacktrace, sessionId=" + sessionId + ", thread=" + Thread.currentThread().getName();
        if (info == null) {
            CoreSessionRegistrationInfo closed = recentlyClosedSessions.getIfPresent(sessionId);
            if (closed == null) {
                // no knowledge of this sessionId, log the current stacktrace
                Exception e = new Exception("DEBUG: " + debug);
                log.warn("Closing unknown CoreSession", e);
            } else {
                // this sessionId was recently closed and we kept info about it
                // log the current stacktrace with the original opening and closing as suppressed exceptions
                Exception e = new Exception("DEBUG: spurious " + debug);
                e.addSuppressed(closed);
                log.warn("Closing already closed CoreSession", e);
            }
        } else {
            // regular closing, record a stacktrace
            info.addSuppressed(new Exception("DEBUG: " + debug));
            recentlyClosedSessions.put(sessionId, info);
            // don't keep the session around, all we want is the stacktrace objects
            info.session = null;
        }
        session.destroy();
    }

    @Override
    public CoreSession getCoreSession(String sessionId) {
        if (sessionId == null) {
            throw new NullPointerException("null sessionId");
        }
        CoreSessionRegistrationInfo info = sessions.get(sessionId);
        return info == null ? null : info.getCoreSession();
    }

    @Override
    public int getNumberOfOpenCoreSessions() {
        return sessions.size();
    }

    @Override
    public List<CoreSessionRegistrationInfo> getCoreSessionRegistrationInfos() {
        return new ArrayList<>(sessions.values());
    }

}
