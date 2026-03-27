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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Tests for {@link NuxeoHttpSessionMonitor}, specifically the session invalidation for a user and the NPE fix in
 * {@code removeEntry}.
 *
 * @since 2025.18
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestNuxeoHttpSessionMonitor {

    /**
     * Rule that provides a fresh {@link NuxeoHttpSessionMonitor} instance for each test.
     *
     * @since 2025.18
     */
    public class MonitorRule extends ExternalResource {

        protected NuxeoHttpSessionMonitor monitor;

        @Override
        protected void before() {
            monitor = new NuxeoHttpSessionMonitor();
        }

        public NuxeoHttpSessionMonitor getMonitor() {
            return monitor;
        }
    }

    @Rule
    public MonitorRule monitorRule = new MonitorRule();

    @Test
    public void testRemoveEntryWithUnknownSessionDoesNotThrow() {
        var monitor = monitorRule.getMonitor();
        // Should not throw NPE for unknown session ID
        monitor.removeEntry("unknown-session-id", true);
        monitor.removeEntry("unknown-session-id", false);
    }

    @Test
    public void testInvalidateSessionsForUser() {
        var monitor = monitorRule.getMonitor();
        var invalidated = new AtomicInteger();

        // Register sessions for two users
        addSession("session-1", "alice", invalidated);
        addSession("session-2", "alice", invalidated);
        addSession("session-3", "bob", invalidated);

        assertEquals(3, monitor.getTrackedSessions().size());

        // Invalidate all sessions for alice
        monitor.invalidateSessionsForUser("alice", null);

        // Alice's sessions should be removed and invalidated
        assertEquals(2, invalidated.get());
        // Only bob's session remains
        assertEquals(1, monitor.getTrackedSessions().size());
        var remaining = monitor.getTrackedSessions().iterator().next();
        assertEquals("bob", remaining.getLoginName());
    }

    @Test
    public void testInvalidateSessionsForUserExcludesCurrent() {
        var monitor = monitorRule.getMonitor();
        var invalidated = new AtomicInteger();

        // Register multiple sessions for alice
        addSession("session-1", "alice", invalidated);
        addSession("session-2", "alice", invalidated);
        addSession("session-3", "alice", invalidated);

        assertEquals(3, monitor.getTrackedSessions().size());

        // Invalidate all sessions for alice except session-2
        monitor.invalidateSessionsForUser("alice", "session-2");

        // Only session-1 and session-3 should be invalidated
        assertEquals(2, invalidated.get());
        // session-2 should remain
        assertEquals(1, monitor.getTrackedSessions().size());
        var remaining = monitor.getTrackedSessions().iterator().next();
        assertEquals("session-2", remaining.getSessionId());
        assertEquals("alice", remaining.getLoginName());
    }

    @Test
    public void testInvalidateSessionsForUserWithNoSessions() {
        var monitor = monitorRule.getMonitor();
        // Should be a no-op, no errors
        monitor.invalidateSessionsForUser("nonexistent", null);
    }

    /**
     * Registers a session in the monitor with the given ID, user, and an invalidation callback that increments the
     * counter.
     */
    protected void addSession(String sessionId, String loginName, AtomicInteger invalidatedCounter) {
        var si = new SessionInfo(sessionId, invalidatedCounter::incrementAndGet);
        si.setLoginName(loginName);
        monitorRule.getMonitor().sessionTracker.put(sessionId, si);
    }
}
